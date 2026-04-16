package com.github.tvbox.osc.ui.compose.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import androidx.compose.ui.Alignment

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    onSettingClick: (String) -> Unit
) {
    val settings = listOf("API Configuration", "Player Settings", "Theme", "Language", "About")

    Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(settings) { setting ->
                Surface(
                    onClick = { onSettingClick(setting) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(setting, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
