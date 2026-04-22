package com.github.tvbox.osc.ui.compose.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.ViewModelProvider
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.ui.compose.screens.SearchScreen
import com.github.tvbox.osc.ui.compose.theme.TVBoxTheme
import com.github.tvbox.osc.viewmodel.SourceViewModel
import android.content.Intent
import com.github.tvbox.osc.ui.compose.activities.DetailComposeActivity

class SearchComposeActivity : ComponentActivity() {
    private lateinit var sourceViewModel: SourceViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sourceViewModel = ViewModelProvider(this).get(SourceViewModel::class.java)

        setContent {
            TVBoxTheme {
                var query by remember { mutableStateOf("") }
                val searchResult by sourceViewModel.searchResult.observeAsState()
                val results = searchResult?.movie?.videoList ?: emptyList()

                SearchScreen(
                    query = query,
                    onQueryChange = { newQuery ->
                        query = newQuery
                        if (newQuery.isNotEmpty()) {
                            sourceViewModel.getSearch(ApiConfig.get().homeSourceBean.key, newQuery)
                        }
                    },
                    results = results,
                    onMovieClick = { movie ->
                        val intent = Intent(this, DetailComposeActivity::class.java).apply {
                            putExtra("id", movie.id)
                            putExtra("sourceKey", movie.sourceKey ?: ApiConfig.get().homeSourceBean.key)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }
}
