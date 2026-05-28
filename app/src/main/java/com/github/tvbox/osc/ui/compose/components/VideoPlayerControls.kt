package com.github.tvbox.osc.ui.compose.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VideoPlayerControls(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    onPlayPauseToggle: () -> Unit,
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    title: String,
    subtitle: String,
    onSubtitleClick: () -> Unit,
    onAudioTrackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onNextClick: () -> Unit,
    onPrevClick: () -> Unit,
    isVisible: Boolean
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                        startY = 300f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp)
            ) {
                // Title and Info
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Seek Bar
                VideoProgressBar(
                    currentPosition = currentPosition,
                    duration = duration,
                    onSeek = onSeek
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlayerIconButton(icon = Icons.Default.SkipPrevious, onClick = onPrevClick)
                        Spacer(modifier = Modifier.width(16.dp))
                        PlayerIconButton(
                            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            onClick = onPlayPauseToggle,
                            isLarge = true
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        PlayerIconButton(icon = Icons.Default.SkipNext, onClick = onNextClick)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlayerTextButton(text = "Subtitles", icon = Icons.Default.Subtitles, onClick = onSubtitleClick)
                        Spacer(modifier = Modifier.width(12.dp))
                        PlayerTextButton(text = "Audio", icon = Icons.Default.Audiotrack, onClick = onAudioTrackClick)
                        Spacer(modifier = Modifier.width(12.dp))
                        PlayerIconButton(icon = Icons.Default.Settings, onClick = onSettingsClick)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VideoProgressBar(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit
) {
    val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentPosition),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
            Text(
                text = formatTime(duration),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    isLarge: Boolean = false
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(if (isLarge) 64.dp else 48.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(if (isLarge) 48.dp else 32.dp),
            tint = Color.White
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerTextButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(text = text)
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
