package com.github.tvbox.osc.ui.compose.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.ViewModelProvider
import com.github.tvbox.osc.bean.Movie
import com.github.tvbox.osc.ui.compose.screens.DetailScreen
import com.github.tvbox.osc.ui.compose.theme.TVBoxTheme
import com.github.tvbox.osc.viewmodel.SourceViewModel
import android.content.Intent
import com.github.tvbox.osc.ui.activity.PlayActivity

class DetailComposeActivity : ComponentActivity() {
    private lateinit var sourceViewModel: SourceViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val id = intent.getStringExtra("id")
        val sourceKey = intent.getStringExtra("sourceKey")

        sourceViewModel = ViewModelProvider(this).get(SourceViewModel::class.java)
        if (id != null && sourceKey != null) {
            sourceViewModel.getDetail(sourceKey, id)
        }

        setContent {
            TVBoxTheme {
                val detailResult by sourceViewModel.detailResult.observeAsState()
                val movie = detailResult?.movie?.videoList?.getOrNull(0)

                DetailScreen(
                    movie = movie,
                    onPlayClick = { movie, episode ->
                        val intent = Intent(this, PlayActivity::class.java).apply {
                            putExtra("id", movie.id)
                            putExtra("sourceKey", movie.sourceKey)
                            putExtra("playFlag", movie.urlBean.infoList[0].flag)
                            putExtra("playIndex", movie.urlBean.infoList[0].beanList.indexOf(episode))
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }
}
