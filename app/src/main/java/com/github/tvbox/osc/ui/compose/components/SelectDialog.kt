package com.github.tvbox.osc.ui.compose.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun <T> SelectDialog(
    title: String,
    items: List<T>,
    selectedItem: T?,
    onItemClick: (T) -> Unit,
    getDisplay: (T) -> String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Surface(
            modifier = Modifier.width(400.dp).heightIn(max = 500.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = title, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn {
                    items(items) { item ->
                        Surface(
                            onClick = { onItemClick(item) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = getDisplay(item), modifier = Modifier.padding(12.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDismiss, modifier = Modifier.align(androidx.compose.ui.Alignment.End)) {
                    Text("Close")
                }
            }
        }
    }
}
