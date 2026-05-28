package com.github.tvbox.osc.ui.compose.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.github.tvbox.osc.bean.LiveChannelItem
import com.github.tvbox.osc.bean.LiveChannelGroup
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LivePlayScreen(
    groups: List<LiveChannelGroup>,
    currentGroup: LiveChannelGroup?,
    channels: List<LiveChannelItem>,
    onChannelClick: (LiveChannelItem) -> Unit,
    onGroupClick: (LiveChannelGroup) -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Left: Groups
        LazyColumn(
            modifier = Modifier.width(240.dp).fillMaxHeight().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(groups) { group ->
                var isFocused by remember { mutableStateOf(false) }
                Surface(
                    onClick = { onGroupClick(group) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                        .border(
                            width = if (isFocused) 2.dp else 0.dp,
                            color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Text(
                        text = group.groupName,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (group == currentGroup) MaterialTheme.colorScheme.primary else Color.Unspecified
                    )
                }
            }
        }

        // Right: Channels
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(channels) { channel ->
                var isFocused by remember { mutableStateOf(false) }
                Surface(
                    onClick = { onChannelClick(channel) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                        .border(
                            width = if (isFocused) 2.dp else 0.dp,
                            color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = channel.channelNum.toString().padStart(3, '0'),
                            modifier = Modifier.width(60.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = channel.channelName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}
