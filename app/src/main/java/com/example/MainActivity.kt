package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CreateVideoScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.SettingsDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VoidBlack
import com.example.viewmodel.VideoAiViewModel
import com.example.viewmodel.VideoGenerationUiState

enum class AppScreen {
    CREATE,
    PLAYER,
    HISTORY
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                VideoAiApp()
            }
        }
    }
}

@Composable
fun VideoAiApp(
    viewModel: VideoAiViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val customApiKey by viewModel.customApiKey.collectAsState()
    var currentScreen by remember { mutableStateOf(AppScreen.CREATE) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // When generation succeeds, switch to PLAYER screen
    val effectiveScreen = when {
        currentScreen == AppScreen.HISTORY -> AppScreen.HISTORY
        uiState is VideoGenerationUiState.Success -> AppScreen.PLAYER
        else -> AppScreen.CREATE
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = VoidBlack
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(VoidBlack)
        ) {
            when (effectiveScreen) {
                AppScreen.CREATE -> {
                    CreateVideoScreen(
                        viewModel = viewModel,
                        onNavigateToHistory = { currentScreen = AppScreen.HISTORY },
                        onOpenSettings = { showSettingsDialog = true }
                    )
                }

                AppScreen.PLAYER -> {
                    val success = uiState as? VideoGenerationUiState.Success
                    if (success != null) {
                        PlayerScreen(
                            viewModel = viewModel,
                            successState = success,
                            onNavigateBack = {
                                viewModel.resetToCreate()
                                currentScreen = AppScreen.CREATE
                            },
                            onNavigateToHistory = { currentScreen = AppScreen.HISTORY }
                        )
                    } else {
                        currentScreen = AppScreen.CREATE
                    }
                }

                AppScreen.HISTORY -> {
                    HistoryScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = AppScreen.CREATE }
                    )
                }
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            currentApiKey = customApiKey,
            onSaveApiKey = { viewModel.onCustomApiKeyChanged(it) },
            onDismiss = { showSettingsDialog = false }
        )
    }
}
