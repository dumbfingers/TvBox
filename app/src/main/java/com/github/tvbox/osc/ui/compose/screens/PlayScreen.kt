package com.github.tvbox.osc.ui.compose.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.github.tvbox.osc.player.MyVideoView
import com.github.tvbox.osc.player.controller.VodController
import com.github.tvbox.osc.ui.compose.components.VideoPlayerControls
import kotlinx.coroutines.delay

@Composable
fun PlayScreen(
    videoView: MyVideoView,
    controller: VodController,
    title: String = "",
    subtitle: String = ""
) {
    var isPlaying by remember { mutableStateOf(videoView.isPlaying) }
    var currentPosition by remember { mutableStateOf(videoView.currentPosition) }
    var duration by remember { mutableStateOf(videoView.duration) }
    var isControlsVisible by remember { mutableStateOf(true) }

    // Update state periodically
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = videoView.currentPosition
            duration = videoView.duration
            delay(1000)
        }
    }

    // Auto-hide controls
    LaunchedEffect(isControlsVisible) {
        if (isControlsVisible) {
            delay(5000)
            isControlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                FrameLayout(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    (videoView.parent as? ViewGroup)?.removeView(videoView)
                    addView(videoView)
                }
            },
            update = { _ ->
            }
        )

        VideoPlayerControls(
            isVisible = isControlsVisible,
            isPlaying = isPlaying,
            onPlayPauseToggle = {
                if (videoView.isPlaying) {
                    videoView.pause()
                } else {
                    videoView.start()
                }
                isPlaying = videoView.isPlaying
            },
            currentPosition = currentPosition,
            duration = duration,
            onSeek = { pos ->
                videoView.seekTo(pos)
                currentPosition = pos
            },
            title = title,
            subtitle = subtitle,
            onSubtitleClick = { /* TODO */ },
            onAudioTrackClick = { /* TODO */ },
            onSettingsClick = { /* TODO */ },
            onNextClick = { /* TODO */ },
            onPrevClick = { /* TODO */ }
        )

        // Transparent overlay to detect clicks and show controls
        if (!isControlsVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            )
        }
    }
}
