package com.example.uniportal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.uniportal.data.local.entity.SearchQueryEntity
import com.example.uniportal.data.local.entity.SearchResultEntity
import com.example.uniportal.data.local.entity.UniversityEntity
import com.example.uniportal.data.local.relation.SearchResultRow
import kotlinx.coroutines.flow.Flow

@Dao
abstract class UniversityDao {
    @Query("SELECT * FROM universities WHERE id = :id")
    abstract fun observe(id: String): Flow<UniversityEntity?>

    @Query("SELECT * FROM universities WHERE id = :id")
    abstract suspend fun get(id: String): UniversityEntity?

    @Query("SELECT fetchedAt FROM universities WHERE id = :id")
    abstract suspend fun getFetchedAt(id: String): Long?

    @Upsert
    abstract suspend fun upsert(university: UniversityEntity)

    @Upsert
    abstract suspend fun upsertAll(universities: List<UniversityEntity>)

    @Query("SELECT COUNT(*) FROM universities")
    abstract fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM universities WHERE country = :country COLLATE NOCASE")
    abstract suspend fun countByCountry(country: String): Int

    @Query(
        "SELECT * FROM universities " +
            "WHERE (:query = '' OR name LIKE '%' || :query || '%' ESCAPE '\\') " +
            "AND (:country = '' OR country = :country COLLATE NOCASE) " +
            "ORDER BY name COLLATE NOCASE LIMIT :limit"
    )
    abstract fun observeLocalSearch(query: String, country: String, limit: Int): Flow<List<UniversityEntity>>

    @Query(
        "SELECT search_queries.loadedPages AS loadedPages, search_queries.hasMore AS hasMore, " +
            "search_queries.fetchedAt AS queryFetchedAt, universities.* " +
            "FROM search_queries " +
            "LEFT JOIN search_results ON search_results.queryKey = search_queries.queryKey " +
            "LEFT JOIN universities ON universities.id = search_results.universityId " +
            "WHERE search_queries.queryKey = :queryKey " +
            "ORDER BY search_results.position"
    )
    abstract fun observeSearch(queryKey: String): Flow<List<SearchResultRow>>

    @Query("SELECT * FROM search_queries WHERE queryKey = :queryKey")
    abstract suspend fun getSearchQuery(queryKey: String): SearchQueryEntity?

    @Upsert
    abstract suspend fun upsertSearchQuery(query: SearchQueryEntity)

    @Query("DELETE FROM search_results WHERE queryKey = :queryKey")
    abstract suspend fun deleteSearchResults(queryKey: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSearchResults(results: List<SearchResultEntity>)

    @Query("SELECT universityId FROM search_results WHERE queryKey = :queryKey")
    abstract suspend fun getSearchResultIds(queryKey: String): List<String>

    @Query("SELECT MAX(position) FROM search_results WHERE queryKey = :queryKey")
    abstract suspend fun getMaxSearchPosition(queryKey: String): Int?

    @Query("DELETE FROM search_queries WHERE fetchedAt < :threshold")
    abstract suspend fun deleteSearchQueriesOlderThan(threshold: Long): Int

    @Query("DELETE FROM search_queries")
    abstract suspend fun deleteAllSearchQueries(): Int

    @Transaction
    open suspend fun replaceSearch(query: SearchQueryEntity, universities: List<UniversityEntity>) {
        upsertAll(universities)

        upsertSearchQuery(query)
        deleteSearchResults(query.queryKey)
        insertSearchResults(
            universities.mapIndexed { index, university ->
                SearchResultEntity(queryKey = query.queryKey, position = index, universityId = university.id)
            },
        )
    }

    @Transaction
    open suspend fun appendSearchPage(
        queryKey: String,
        expectedLoadedPages: Int,
        hasMore: Boolean,
        universities: List<UniversityEntity>,
    ): Boolean {
        val query = getSearchQuery(queryKey) ?: return false
        if (query.loadedPages != expectedLoadedPages) return false
        upsertAll(universities)
        val knownIds = getSearchResultIds(queryKey).toHashSet()
        val firstPosition = (getMaxSearchPosition(queryKey) ?: -1) + 1
        val newResults = universities
            .filter { knownIds.add(it.id) }
            .mapIndexed { index, university ->
                SearchResultEntity(queryKey = queryKey, position = firstPosition + index, universityId = university.id)
            }
        insertSearchResults(newResults)
        upsertSearchQuery(query.copy(loadedPages = expectedLoadedPages + 1, hasMore = hasMore))
        return true
    }

    @Query(
        "SELECT id FROM universities WHERE fetchedAt < :staleBefore AND (" +
            "id IN (SELECT universityId FROM favourites) " +
            "OR id IN (SELECT universityId FROM shortlist_items) " +
            "OR id IN (SELECT universityId FROM applications) " +
            "OR id IN (SELECT universityId FROM history)" +
            ") ORDER BY fetchedAt ASC LIMIT :limit"
    )
    abstract suspend fun getStalePinnedIds(staleBefore: Long, limit: Int): List<String>

    @Query(
        "DELETE FROM universities WHERE fetchedAt < :threshold " +
            "AND id NOT IN (SELECT universityId FROM favourites) " +
            "AND id NOT IN (SELECT universityId FROM shortlist_items) " +
            "AND id NOT IN (SELECT universityId FROM applications) " +
            "AND id NOT IN (SELECT universityId FROM history) " +
            "AND id NOT IN (SELECT universityId FROM search_results) " +
            "AND lower(country) NOT IN (SELECT lower(countryName) FROM offline_countries)"
    )
    abstract suspend fun deleteUnpinnedUniversitiesOlderThan(threshold: Long): Int

    @Transaction
    open suspend fun prune(threshold: Long): Int {
        val searches = deleteSearchQueriesOlderThan(threshold)
        val universities = deleteUnpinnedUniversitiesOlderThan(threshold)
        return searches + universities
    }

    @Transaction
    open suspend fun clearUnpinned(): Int {
        val searches = deleteAllSearchQueries()
        val universities = deleteUnpinnedUniversitiesOlderThan(Long.MAX_VALUE)
        return searches + universities
    }
}
