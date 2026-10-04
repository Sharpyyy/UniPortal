package com.example.uniportal.ui.favourites

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.uniportal.R
import com.example.uniportal.domain.model.ApplicationStatus
import com.example.uniportal.domain.model.University
import com.example.uniportal.ui.components.BackToSearchButton
import com.example.uniportal.ui.components.EmptyState
import com.example.uniportal.ui.components.LoadingState
import com.example.uniportal.ui.components.SearchField
import com.example.uniportal.ui.components.TestTags
import com.example.uniportal.ui.components.UniversityCard
import com.example.uniportal.ui.preview.PreviewData
import com.example.uniportal.ui.theme.UniPortalTheme

@Composable
fun FavouritesPage(
    state: FavouritesUiState,
    onEvent: (FavouritesEvent) -> Unit,
    onUniversityClick: (String) -> Unit,
    onRemove: (University) -> Unit,
    onOpenSearch: () -> Unit,
    bottomInset: Dp,
    modifier: Modifier = Modifier,
) {
    val showFilter = state.content is FavouritesContent.Loading ||
        state.totalCount > 0 ||
        state.filterQuery.isNotEmpty()

    Column(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(visible = showFilter) {
            SearchField(
                query = state.filterQuery,
                onQueryChange = { onEvent(FavouritesEvent.FilterChanged(it)) },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                placeholder = stringResource(R.string.favourites_filter_placeholder),
                clearContentDescription = stringResource(R.string.cd_clear_filter),
                latinHint = stringResource(R.string.favourites_filter_latin_hint),
                tag = TestTags.FAVOURITES_FILTER_FIELD,
            )
        }
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = state.content,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = { fadeIn(tween(CONTENT_FADE_MS)) togetherWith fadeOut(tween(CONTENT_FADE_MS)) },
                label = "favouritesContent",

                contentKey = { it::class },
            ) { content ->
                val stateModifier = Modifier.padding(bottom = bottomInset)
                when (content) {
                    FavouritesContent.Loading -> LoadingState(
                        modifier = stateModifier,
                        showAfterMillis = LOADING_SPINNER_DELAY_MS,
                    )
                    FavouritesContent.Empty -> EmptyState(
                        emoji = "💫",
                        title = stringResource(R.string.favourites_empty_title),
                        message = stringResource(R.string.favourites_empty_message),
                        modifier = stateModifier,
                    ) {
                        BackToSearchButton(onClick = onOpenSearch)
                    }
                    FavouritesContent.NoMatches -> EmptyState(
                        emoji = "🔍",
                        title = stringResource(R.string.favourites_no_matches_title),
                        message = stringResource(R.string.favourites_no_matches_message),
                        modifier = stateModifier,
                    ) {
                        ResetFilterButton(onClick = { onEvent(FavouritesEvent.FilterChanged("")) })
                    }
                    is FavouritesContent.Success -> FavouritesList(
                        favourites = content.items,
                        applicationStatuses = state.applicationStatuses,
                        shortlistCounts = state.shortlistCounts,
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = LIST_TOP_PADDING,
                            end = 16.dp,
                            bottom = bottomInset + 16.dp,
                        ),
                        onUniversityClick = onUniversityClick,
                        onRemove = onRemove,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(LIST_TOP_PADDING)
                    .background(
                        Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, Color.Transparent)),
                    ),
            )
        }
    }
}

@Composable
private fun FavouritesList(
    favourites: List<University>,
    applicationStatuses: Map<String, ApplicationStatus>,
    shortlistCounts: Map<String, Int>,
    contentPadding: PaddingValues,
    onUniversityClick: (String) -> Unit,
    onRemove: (University) -> Unit,
) {
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val isScrolling = listState.isScrollInProgress
    LaunchedEffect(isScrolling) {
        if (isScrolling) focusManager.clearFocus()
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .testTag(TestTags.FAVOURITES_LIST),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = favourites, key = { it.id }) { university ->
            UniversityCard(
                university = university,
                isFavourite = true,
                onClick = { onUniversityClick(university.id) },
                onToggleFavourite = { onRemove(university) },
                modifier = Modifier.animateItem(),
                applicationStatus = applicationStatuses[university.id],
                shortlistCount = shortlistCounts[university.id] ?: 0,
            )
        }
    }
}

@Composable
private fun ResetFilterButton(onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Filled.Clear,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(text = stringResource(R.string.action_reset_filter))
    }
}

private const val CONTENT_FADE_MS = 220
private const val LOADING_SPINNER_DELAY_MS = 300L
private val LIST_TOP_PADDING = 8.dp

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun FavouritesPagePreview() {
    UniPortalTheme {
        FavouritesPage(
            state = FavouritesUiState(
                totalCount = PreviewData.universities.size,
                content = FavouritesContent.Success(PreviewData.universities),
                applicationStatuses = mapOf(PreviewData.mtsu.id to ApplicationStatus.PREPARING),
                shortlistCounts = mapOf(PreviewData.msu.id to 2),
            ),
            onEvent = {},
            onUniversityClick = {},
            onRemove = {},
            onOpenSearch = {},
            bottomInset = 0.dp,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun FavouritesPageNoMatchesPreview() {
    UniPortalTheme {
        FavouritesPage(
            state = FavouritesUiState(
                filterQuery = "Oxford",
                totalCount = PreviewData.universities.size,
                content = FavouritesContent.NoMatches,
            ),
            onEvent = {},
            onUniversityClick = {},
            onRemove = {},
            onOpenSearch = {},
            bottomInset = 0.dp,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun FavouritesPageEmptyPreview() {
    UniPortalTheme {
        FavouritesPage(
            state = FavouritesUiState(content = FavouritesContent.Empty),
            onEvent = {},
            onUniversityClick = {},
            onRemove = {},
            onOpenSearch = {},
            bottomInset = 0.dp,
        )
    }
}
