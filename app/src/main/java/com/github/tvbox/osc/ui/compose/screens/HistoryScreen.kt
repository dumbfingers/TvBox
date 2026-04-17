package com.github.tvbox.osc.ui.compose.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.github.tvbox.osc.bean.VodInfo
import com.github.tvbox.osc.bean.Movie
import androidx.compose.ui.Alignment

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HistoryScreen(
    historyItems: List<VodInfo>,
    onItemClick: (VodInfo) -> Unit,
    onClearAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("History", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = onClearAll) {
                Text("Clear All")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (historyItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No history found")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(historyItems) { vodInfo ->
                    // Reuse MovieCard but adapter for VodInfo
                    val movie = Movie.Video().apply {
                        id = vodInfo.id
                        name = vodInfo.name
                        pic = vodInfo.pic
                        sourceKey = vodInfo.sourceKey
                    }
                    MovieCard(movie = movie, onClick = { onItemClick(vodInfo) })
                }
            }
        }
    }
}
