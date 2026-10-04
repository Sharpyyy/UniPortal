package com.example.uniportal

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniportal.ui.MainViewModel
import com.example.uniportal.ui.navigation.TopLevelDestination
import com.example.uniportal.ui.navigation.UniPortalAppScaffold
import com.example.uniportal.ui.theme.LightBackground
import com.example.uniportal.ui.theme.PortalBackground
import com.example.uniportal.ui.theme.UniPortalTheme
import com.example.uniportal.ui.theme.isDarkTheme
import com.example.uniportal.work.DeadlineNotifier
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applySystemBarStyle(darkTheme = true)
        if (savedInstanceState == null) handleLaunchIntent(intent)

        addOnNewIntentListener { newIntent -> handleLaunchIntent(newIntent) }

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

            val mode = themeMode
            if (mode != null) {
                val darkTheme = mode.isDarkTheme(isSystemInDarkTheme())
                DisposableEffect(darkTheme) {
                    applySystemBarStyle(darkTheme)
                    onDispose {}
                }
                UniPortalTheme(themeMode = mode) {
                    val pendingTab by viewModel.pendingTab.collectAsStateWithLifecycle()
                    val applicationsBadgeCount by viewModel.applicationsBadgeCount.collectAsStateWithLifecycle()
                    UniPortalAppScaffold(
                        applicationsBadgeCount = applicationsBadgeCount,
                        pendingTab = pendingTab,
                        onPendingTabHandled = viewModel::onPendingTabHandled,
                    )
                }
            }
        }
    }

    private fun handleLaunchIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(DeadlineNotifier.EXTRA_OPEN_APPLICATIONS, false) == true) {
            intent.removeExtra(DeadlineNotifier.EXTRA_OPEN_APPLICATIONS)
            viewModel.openTab(TopLevelDestination.APPLICATIONS)
        }
    }

    private fun applySystemBarStyle(darkTheme: Boolean) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },

            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = LightBackground.copy(alpha = NAVIGATION_BAR_SCRIM_ALPHA).toArgb(),
                darkScrim = PortalBackground.copy(alpha = NAVIGATION_BAR_SCRIM_ALPHA).toArgb(),
            ) { darkTheme },
        )
    }
}

private const val NAVIGATION_BAR_SCRIM_ALPHA = 0.8f
