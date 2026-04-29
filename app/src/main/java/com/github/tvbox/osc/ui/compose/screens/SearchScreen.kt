package com.github.tvbox.osc.ui.compose.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.github.tvbox.osc.bean.Movie
import androidx.compose.ui.Alignment
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.border

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<Movie.Video>,
    onMovieClick: (Movie.Video) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Search Input
        var isTextFieldFocused by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Search:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.width(16.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = TextStyle(color = Color.White, fontSize = MaterialTheme.typography.bodyLarge.fontSize),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { isTextFieldFocused = it.isFocused }
                    .background(Color.DarkGray, RoundedCornerShape(4.dp))
                    .border(
                        width = if (isTextFieldFocused) 2.dp else 0.dp,
                        color = if (isTextFieldFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp),
                singleLine = true
            )
        }

        // Search Results
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(results) { movie ->
                MovieCard(movie = movie, onClick = { onMovieClick(movie) })
            }
        }
    }
}
