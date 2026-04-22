package com.github.tvbox.osc.ui.compose.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.github.tvbox.osc.ui.compose.screens.SettingsScreen
import com.github.tvbox.osc.ui.compose.screens.ApiConfigScreen
import com.github.tvbox.osc.ui.compose.theme.TVBoxTheme
import com.github.tvbox.osc.util.HawkConfig
import com.orhanobut.hawk.Hawk
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.tv.material3.Text
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api

class SettingsComposeActivity : ComponentActivity() {
    enum class SettingsSubMenu {
        MAIN, API, PLAYER, THEME, LANGUAGE, ABOUT
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TVBoxTheme {
                var currentMenu by remember { mutableStateOf(SettingsSubMenu.MAIN) }

                BackHandler(enabled = currentMenu != SettingsSubMenu.MAIN) {
                    currentMenu = SettingsSubMenu.MAIN
                }

                when (currentMenu) {
                    SettingsSubMenu.MAIN -> SettingsScreen(
                        onApiConfigClick = { currentMenu = SettingsSubMenu.API },
                        onPlayerSettingsClick = { currentMenu = SettingsSubMenu.PLAYER },
                        onThemeClick = { currentMenu = SettingsSubMenu.THEME },
                        onLanguageClick = { currentMenu = SettingsSubMenu.LANGUAGE },
                        onAboutClick = { currentMenu = SettingsSubMenu.ABOUT }
                    )
                    SettingsSubMenu.API -> ApiConfigScreen(
                        currentApi = Hawk.get(HawkConfig.API_URL, ""),
                        onApiChange = { newApi ->
                            Hawk.put(HawkConfig.API_URL, newApi)
                        },
                        onBack = { currentMenu = SettingsSubMenu.MAIN }
                    )
                    else -> {
                        // Placeholder for other submenus
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Submenu ${currentMenu.name} coming soon")
                                Button(onClick = { currentMenu = SettingsSubMenu.MAIN }) {
                                    Text("Back")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
