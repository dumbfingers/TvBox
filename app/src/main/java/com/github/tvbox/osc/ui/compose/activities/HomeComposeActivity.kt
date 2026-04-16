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
import com.github.tvbox.osc.ui.activity.SearchActivity
import com.github.tvbox.osc.ui.activity.SettingActivity

class HomeComposeActivity : ComponentActivity() {
    private lateinit var sourceViewModel: SourceViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sourceViewModel = ViewModelProvider(this).get(SourceViewModel::class.java)
        sourceViewModel.getSort(ApiConfig.get().homeSourceBean.key)

        setContent {
            TVBoxTheme {
                val absSortXml by sourceViewModel.sortResult.observeAsState()
                val categories = absSortXml?.classes?.sortList ?: emptyList()

                HomeScreen(
                    categories = categories,
                    onCategorySelected = { sortData ->
                        sourceViewModel.getSort(ApiConfig.get().homeSourceBean.key)
                    },
                    onSearchClick = {
                        startActivity(Intent(this, SearchActivity::class.java))
                    },
                    onSettingsClick = {
                        startActivity(Intent(this, SettingActivity::class.java))
                    }
                )
            }
        }
    }
}
