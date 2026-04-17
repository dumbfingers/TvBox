package com.github.tvbox.osc.ui.compose.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.github.tvbox.osc.ui.compose.screens.SettingsScreen
import com.github.tvbox.osc.ui.compose.theme.TVBoxTheme
import android.widget.Toast

class SettingsComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TVBoxTheme {
                SettingsScreen(
                    onSettingClick = { setting ->
                        Toast.makeText(this, "Clicked: $setting", Toast.LENGTH_SHORT).show()
                        // Legacy settings integration can be added here
                    }
                )
            }
        }
    }
}
