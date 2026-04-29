package com.github.tvbox.osc.ui.compose.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.github.tvbox.osc.bean.Movie
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import androidx.compose.ui.layout.ContentScale
import androidx.tv.foundation.lazy.list.*

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun DetailScreen(
    movie: Movie.Video?,
    onPlayClick: (Movie.Video, Movie.Video.UrlBean.UrlInfo.InfoBean) -> Unit
) {
    if (movie == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("Loading...")
        }
        return
    }

    Row(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        // Left Side: Poster and Info
        Column(modifier = Modifier.weight(1f)) {
            Card(
                onClick = {},
                modifier = Modifier.width(200.dp).aspectRatio(2f/3f)
            ) {
                GlideImage(
                    model = movie.pic,
                    contentDescription = movie.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = movie.name, style = MaterialTheme.typography.headlineLarge)
            Text(text = movie.year.toString(), style = MaterialTheme.typography.bodyMedium)
            Text(text = movie.type ?: "", style = MaterialTheme.typography.bodyMedium)
            Text(text = movie.actor ?: "", style = MaterialTheme.typography.bodySmall, maxLines = 2)
        }

        Spacer(modifier = Modifier.width(32.dp))

        // Right Side: Description and Episodes
        Column(modifier = Modifier.weight(2f)) {
            Text(text = movie.des ?: "", style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Episodes", style = MaterialTheme.typography.headlineSmall)

            Spacer(modifier = Modifier.height(8.dp))

            val urlInfo = movie.urlBean?.infoList?.getOrNull(0)
            if (urlInfo != null) {
                TvLazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(urlInfo.beanList) { episode ->
                        Button(onClick = { onPlayClick(movie, episode) }) {
                            Text(episode.name)
                        }
                    }
                }
            }
        }
    }
}
