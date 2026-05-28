package com.github.tvbox.osc.ui.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.bean.VodInfo
import com.github.tvbox.osc.cache.RoomDataManger
import com.github.tvbox.osc.ui.compose.screens.*
import com.github.tvbox.osc.ui.compose.theme.TVBoxTheme
import com.github.tvbox.osc.viewmodel.SourceViewModel
import com.github.tvbox.osc.player.MyVideoView
import com.github.tvbox.osc.player.controller.VodController
import com.github.tvbox.osc.viewmodel.drive.LocalDriveViewModel
import com.github.tvbox.osc.bean.DriveFolderFile

class MainActivity : ComponentActivity() {
    private lateinit var sourceViewModel: SourceViewModel
    private lateinit var videoView: MyVideoView
    private lateinit var vodController: VodController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sourceViewModel = ViewModelProvider(this).get(SourceViewModel::class.java)

        videoView = MyVideoView(this)
        vodController = VodController(this)

        setContent {
            TVBoxTheme {
                AppNavigation(sourceViewModel, videoView, vodController)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        videoView.release()
    }
}

@Composable
fun AppNavigation(
    sourceViewModel: SourceViewModel,
    videoView: MyVideoView,
    vodController: VodController
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
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
                    navController.navigate("detail/${movie.sourceKey ?: ApiConfig.get().homeSourceBean.key}/${movie.id}")
                },
                onSearchClick = { navController.navigate("search") },
                onHistoryClick = { navController.navigate("history") },
                onCollectClick = { navController.navigate("collect") },
                onPushClick = { navController.navigate("push") },
                onLiveClick = { navController.navigate("live") },
                onDriveClick = { navController.navigate("drive") },
                onGlobalSearchClick = { navController.navigate("global_search") },
                onAppsClick = { navController.navigate("apps") },
                onSettingsClick = { navController.navigate("settings") }
            )
        }

        composable(
            "detail/{sourceKey}/{id}",
            arguments = listOf(
                navArgument("sourceKey") { type = NavType.StringType },
                navArgument("id") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sourceKey = backStackEntry.arguments?.getString("sourceKey") ?: ""
            val id = backStackEntry.arguments?.getString("id") ?: ""

            LaunchedEffect(sourceKey, id) {
                sourceViewModel.getDetail(sourceKey, id)
            }

            val detailResult by sourceViewModel.detailResult.observeAsState()
            val movie = detailResult?.movie?.videoList?.firstOrNull()

            DetailScreen(
                movie = movie,
                onPlayClick = { m, _ ->
                    navController.navigate("play/${m.sourceKey}/${m.id}")
                }
            )
        }

        composable(
            "play/{sourceKey}/{id}",
            arguments = listOf(
                navArgument("sourceKey") { type = NavType.StringType },
                navArgument("id") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sourceKey = backStackEntry.arguments?.getString("sourceKey") ?: ""
            val id = backStackEntry.arguments?.getString("id") ?: ""

            val detailResult by sourceViewModel.detailResult.observeAsState()
            val movie = detailResult?.movie?.videoList?.firstOrNull()

            LaunchedEffect(movie) {
                movie?.let {
                    val urlInfo = it.urlBean?.infoList?.firstOrNull()
                    val vs = urlInfo?.beanList?.firstOrNull()
                    vs?.let { episode ->
                        sourceViewModel.getPlay(sourceKey, urlInfo.flag, "", episode.url, "")
                    }
                }
            }

            val playResult by sourceViewModel.playResult.observeAsState()
            LaunchedEffect(playResult) {
                playResult?.let {
                    val url = it.optString("url")
                    if (url.isNotEmpty()) {
                        videoView.setUrl(url)
                        videoView.start()
                    }
                }
            }

            PlayScreen(
                videoView = videoView,
                controller = vodController,
                title = movie?.name ?: "Loading...",
                subtitle = ""
            )
        }

        composable("search") {
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
                    navController.navigate("detail/${movie.sourceKey ?: ApiConfig.get().homeSourceBean.key}/${movie.id}")
                }
            )
        }

        composable("global_search") {
            var query by remember { mutableStateOf("") }
            val searchResult by sourceViewModel.searchResult.observeAsState()
            val results = searchResult?.movie?.videoList ?: emptyList()

            GlobalSearchScreen(
                query = query,
                onQueryChange = { query = it },
                results = results,
                onMovieClick = { movie ->
                    navController.navigate("detail/${movie.sourceKey}/${movie.id}")
                },
                onSearchExecute = {
                    sourceViewModel.initExecutor()
                    for (source in ApiConfig.get().sourceBeanList) {
                        if (source.isSearchable()) {
                            sourceViewModel.execute { sourceViewModel.getSearch(source.key, query) }
                        }
                    }
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                onApiConfigClick = {},
                onPlayerSettingsClick = {},
                onThemeClick = {},
                onLanguageClick = {},
                onAboutClick = {}
            )
        }

        composable("history") {
            var historyItems by remember { mutableStateOf(emptyList<VodInfo>()) }
            LaunchedEffect(Unit) {
                historyItems = RoomDataManger.getAllVodRecord(100) ?: emptyList()
            }
            HistoryScreen(
                historyItems = historyItems,
                onItemClick = { vodInfo ->
                    navController.navigate("detail/${vodInfo.sourceKey}/${vodInfo.id}")
                },
                onClearAll = {
                    RoomDataManger.deleteVodRecordAll()
                    historyItems = emptyList()
                }
            )
        }

        composable("collect") {
            var collectItems by remember { mutableStateOf(RoomDataManger.getAllVodCollect() ?: emptyList()) }
            CollectScreen(
                collectItems = collectItems,
                onItemClick = { vodCollect ->
                    navController.navigate("detail/${vodCollect.sourceKey}/${vodCollect.vodId}")
                },
                onClearAll = {
                    RoomDataManger.deleteVodCollectAll()
                    collectItems = emptyList()
                }
            )
        }

        composable("push") {
            PushScreen(
                address = "http://127.0.0.1:9978",
                onPushLocalClick = { }
            )
        }

        composable("apps") {
            AppsScreen(
                apps = emptyList(),
                onAppClick = { }
            )
        }

        composable("live") {
            val groups = ApiConfig.get().channelGroupList
            var currentGroup by remember { mutableStateOf(groups.firstOrNull()) }
            val channels = currentGroup?.liveChannels ?: emptyList()

            LivePlayScreen(
                groups = groups,
                currentGroup = currentGroup,
                channels = channels,
                onChannelClick = { channel ->
                    videoView.setUrl(channel.url)
                    videoView.start()
                    navController.navigate("play_live")
                },
                onGroupClick = { group ->
                    currentGroup = group
                }
            )
        }

        composable("play_live") {
            PlayScreen(
                videoView = videoView,
                controller = vodController,
                title = "Live TV",
                subtitle = ""
            )
        }

        composable("drive") {
            var files by remember { mutableStateOf(emptyList<DriveFolderFile>()) }

            LaunchedEffect(Unit) {
                val storageDrives = RoomDataManger.getAllDrives()
                files = storageDrives.map { DriveFolderFile(it) }
            }

            DriveScreen(
                currentPath = "/",
                files = files,
                onFileClick = { _ ->
                },
                onBackClick = { navController.popBackStack() },
                onAddServerClick = {}
            )
        }
    }
}
