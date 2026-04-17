package com.github.tvbox.osc.ui.compose.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.ViewModelProvider
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.bean.MovieSort
import com.github.tvbox.osc.ui.compose.screens.HomeScreen
import com.github.tvbox.osc.ui.compose.theme.TVBoxTheme
import com.github.tvbox.osc.viewmodel.SourceViewModel
import android.content.Intent
import com.github.tvbox.osc.ui.compose.activities.SearchComposeActivity
import com.github.tvbox.osc.ui.compose.activities.SettingsComposeActivity
import com.github.tvbox.osc.ui.compose.activities.DetailComposeActivity
import com.github.tvbox.osc.ui.compose.activities.HistoryComposeActivity
import com.github.tvbox.osc.ui.compose.activities.CollectComposeActivity

class HomeComposeActivity : ComponentActivity() {
    private lateinit var sourceViewModel: SourceViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sourceViewModel = ViewModelProvider(this).get(SourceViewModel::class.java)

        val homeSourceBean = ApiConfig.get().homeSourceBean
        if (homeSourceBean != null) {
            sourceViewModel.getSort(homeSourceBean.key)
        }

        setContent {
            TVBoxTheme {
                val absSortXml by sourceViewModel.sortResult.observeAsState()
                val listResult by sourceViewModel.listResult.observeAsState()

                val categories = absSortXml?.classes?.sortList ?: emptyList()
                val movies = listResult?.movie?.videoList ?: absSortXml?.videoList ?: emptyList()

                HomeScreen(
                    categories = categories,
                    movies = movies,
                    onCategorySelected = { sortData ->
                        sourceViewModel.getList(sortData, 1)
                    },
                    onMovieClick = { movie ->
                        val intent = Intent(this, DetailComposeActivity::class.java).apply {
                            putExtra("id", movie.id)
                            putExtra("sourceKey", movie.sourceKey ?: ApiConfig.get().homeSourceBean.key)
                        }
                        startActivity(intent)
                    },
                    onSearchClick = {
                        startActivity(Intent(this, SearchComposeActivity::class.java))
                    },
                    onHistoryClick = {
                        startActivity(Intent(this, HistoryComposeActivity::class.java))
                    },
                    onCollectClick = {
                        startActivity(Intent(this, CollectComposeActivity::class.java))
                    },
                    onSettingsClick = {
                        startActivity(Intent(this, SettingsComposeActivity::class.java))
                    }
                )
            }
        }
    }
}
