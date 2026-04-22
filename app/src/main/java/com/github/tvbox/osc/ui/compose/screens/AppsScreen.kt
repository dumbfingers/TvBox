package com.github.tvbox.osc.ui.compose.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.github.tvbox.osc.bean.AppInfo
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toBitmap

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AppsScreen(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Apps", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(apps) { appInfo ->
                Card(
                    onClick = { onAppClick(appInfo) },
                    modifier = Modifier.width(120.dp).height(140.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val iconBitmap = remember(appInfo.icon) {
                            appInfo.icon.toBitmap().asImageBitmap()
                        }
                        Image(
                            bitmap = iconBitmap,
                            contentDescription = appInfo.name,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = appInfo.name,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
