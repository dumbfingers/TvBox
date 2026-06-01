package com.github.tvbox.osc.ui.compose.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.github.tvbox.osc.bean.DriveFolderFile
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DriveScreen(
    currentPath: String,
    files: List<DriveFolderFile>,
    onFileClick: (DriveFolderFile) -> Unit,
    onBackClick: () -> Unit,
    onAddServerClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Storage: $currentPath", style = MaterialTheme.typography.headlineMedium)
            Row {
                Button(onClick = onAddServerClick) {
                    Text("Add Server")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(onClick = onBackClick) {
                    Text("Back")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (files.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No files or folders found", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(files) { file ->
                    var isFocused by remember { mutableStateOf(false) }
                    Surface(
                        onClick = { onFileClick(file) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFocused = it.isFocused }
                            .border(
                                width = if (isFocused) 2.dp else 0.dp,
                                color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (file.isFile) "📄" else "📁",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(end = 16.dp)
                            )
                            Text(text = file.name ?: "Unknown", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}
