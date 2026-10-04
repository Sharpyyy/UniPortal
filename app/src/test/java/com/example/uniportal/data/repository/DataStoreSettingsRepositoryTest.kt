package com.example.uniportal.data.repository

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.example.uniportal.domain.model.AppSettings
import com.example.uniportal.domain.model.CacheTtl
import com.example.uniportal.domain.model.CountryFilter
import com.example.uniportal.domain.model.ThemeMode
import java.io.File
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class DataStoreSettingsRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: DataStoreSettingsRepository

    @Before
    fun setUp() {
        dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { File(folder.root, "settings.preferences_pb") },
        )
        repository = DataStoreSettingsRepository(dataStore)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun `given settings observed, when values change or are set again, then only real changes are emitted`() = runTest {
        repository.settings.test(timeout = TIMEOUT) {
            assertEquals(AppSettings(), awaitItem())

            repository.setTheme(ThemeMode.LIGHT)
            assertEquals(ThemeMode.LIGHT, awaitItem().theme)

            repository.setTheme(ThemeMode.LIGHT)
            repository.setHistoryLimit(20)
            val next = awaitItem()
            assertEquals("no repeated LIGHT emission in between", 20, next.historyLimit)
            assertEquals(ThemeMode.LIGHT, next.theme)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private companion object {
        val TIMEOUT = 10.seconds
    }
}
