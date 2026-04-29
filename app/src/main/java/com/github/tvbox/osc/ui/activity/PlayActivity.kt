package com.github.tvbox.osc.ui.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Icon
import android.net.Uri
import android.net.http.SslError
import android.os.*
import android.text.TextUtils
import android.util.Rational
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.Player
import androidx.media3.common.text.Cue
import androidx.recyclerview.widget.DiffUtil
import com.github.tvbox.osc.R
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.base.App
import com.github.tvbox.osc.base.BaseActivity
import com.github.tvbox.osc.bean.ParseBean
import com.github.tvbox.osc.bean.SourceBean
import com.github.tvbox.osc.player.TrackInfoBean
import com.github.tvbox.osc.bean.VodInfo
import com.github.tvbox.osc.cache.CacheManager
import com.github.tvbox.osc.event.RefreshEvent
import com.github.tvbox.osc.player.*
import com.github.tvbox.osc.player.controller.VodController
import com.github.tvbox.osc.util.FileUtils
import com.github.tvbox.osc.player.danmu.Parser
import com.github.tvbox.osc.player.thirdparty.Kodi
import com.github.tvbox.osc.player.thirdparty.MXPlayer
import com.github.tvbox.osc.player.thirdparty.ReexPlayer
import com.github.tvbox.osc.server.ControlManager
import com.github.tvbox.osc.server.RemoteServer
import com.github.tvbox.osc.subtitle.model.Subtitle
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter
import com.github.tvbox.osc.ui.dialog.*
import com.github.tvbox.osc.util.*
import com.github.tvbox.osc.util.parser.SuperParse
import com.github.tvbox.osc.util.thunder.Jianpian
import com.github.tvbox.osc.util.thunder.Thunder
import com.github.tvbox.osc.viewmodel.SourceViewModel
import com.google.gson.JsonParser
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.AbsCallback
import com.lzy.okgo.model.HttpHeaders
import com.lzy.okgo.model.Response
import com.obsez.android.lib.filechooser.ChooserDialog
import com.orhanobut.hawk.Hawk
import master.flame.danmaku.danmaku.model.BaseDanmaku
import master.flame.danmaku.danmaku.model.IDisplayer
import master.flame.danmaku.danmaku.model.android.DanmakuContext
import master.flame.danmaku.ui.widget.DanmakuView
import me.jessyan.autosize.AutoSize
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONException
import org.json.JSONObject
import org.xwalk.core.*
import xyz.doikki.videoplayer.player.AndroidMediaPlayer
import xyz.doikki.videoplayer.player.ProgressManager
import java.io.ByteArrayInputStream
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import com.github.tvbox.osc.ui.compose.screens.PlayScreen
import com.github.tvbox.osc.ui.compose.theme.TVBoxTheme

class PlayActivity : BaseActivity() {
    private lateinit var mVideoView: MyVideoView
    private lateinit var mPlayLoadTip: TextView
    private lateinit var mPlayLoadErr: ImageView
    private lateinit var mPlayLoading: ProgressBar
    private lateinit var mController: VodController
    private lateinit var sourceViewModel: SourceViewModel
    private lateinit var mHandler: Handler
    private var videoURL: String? = null
    private var videoDuration: Long = -1
    private val videoSegmentationURL: MutableList<String> = ArrayList()
    private var pipActionReceiver: BroadcastReceiver? = null
    private var executorService: ExecutorService? = null
    private lateinit var mDanmuView: DanmakuView
    private lateinit var mDanmakuContext: DanmakuContext
    private var danmuText: String? = null

    companion object {
        const val BROADCAST_ACTION = "VOD_CONTROL"
        const val BROADCAST_ACTION_PREV = 0
        const val BROADCAST_ACTION_PLAYPAUSE = 1
        const val BROADCAST_ACTION_NEXT = 2
    }

    override fun getLayoutResID(): Int = 0

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refresh(event: RefreshEvent) {
        if (event.type == RefreshEvent.TYPE_SUBTITLE_SIZE_CHANGE) {
            mController.mSubtitleView.setTextSize((event.obj as Int).toFloat())
        }
        if (event.type == RefreshEvent.TYPE_SET_DANMU_SETTINGS) {
            setDanmuViewSettings(event.obj as Boolean)
        }
    }

    override fun init() {
        EventBus.getDefault().register(this)
        initComponents()
        initViewModel()
        initData()
        initDanmuView()

        setContent {
            TVBoxTheme {
                PlayScreen(videoView = mVideoView, controller = mController)
            }
        }
    }

    private fun initComponents() {
        hideSystemUI(false)
        mHandler = Handler(Looper.getMainLooper()) { msg ->
            when (msg.what) {
                100 -> {
                    stopParse()
                    errorWithRetry("嗅探错误", false)
                }
                200 -> {
                    if (mHandler.hasMessages(100)) {
                        setTip("加载完成，嗅探视频中", true, false)
                    }
                }
                300 -> {
                    setTip(msg.obj as String, false, true)
                }
            }
            false
        }

        mVideoView = MyVideoView(this)
        mPlayLoadTip = TextView(this)
        mPlayLoading = ProgressBar(this)
        mPlayLoadErr = ImageView(this)

        mController = VodController(this)
        mController.setCanChangePosition(true)
        mController.setEnableInNormal(true)
        mController.setGestureEnabled(true)

        val progressManager = object : ProgressManager() {
            override fun saveProgress(url: String, progress: Long) {
                if (videoDuration == 0L) return
                CacheManager.save(MD5.string2MD5(url), progress)
            }

            override fun getSavedProgress(url: String): Long {
                var st = 0
                try {
                    st = mVodPlayerCfg?.optInt("st", 0) ?: 0
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                val skip = st * 1000L
                val cache = CacheManager.getCache(MD5.string2MD5(url))
                if (cache == null) return skip
                val rec = cache as Long
                return if (rec < skip) skip else rec
            }
        }
        mVideoView.setProgressManager(progressManager)
        mController.setListener(object : VodController.VodControlListener {
            override fun showDanmuSetting() {
                DanmuSettingDialog(this@PlayActivity, mDanmuView).show()
            }

            override fun playNext(rmProgress: Boolean) {
                if (videoSegmentationURL.size > 0) {
                    for (i in 0 until videoSegmentationURL.size - 1) {
                        if (videoSegmentationURL[i] == videoURL) {
                            mVideoView.setPlayFromZeroPositionOnce(true)
                            startPlayUrl(videoSegmentationURL[i + 1], HashMap())
                            return
                        }
                    }
                }
                val preProgressKey = progressKey
                this@PlayActivity.playNext(rmProgress)
                if (rmProgress && preProgressKey != null) CacheManager.delete(MD5.string2MD5(preProgressKey), 0)
            }

            override fun playPre() {
                if (videoSegmentationURL.size > 0) {
                    for (i in 1 until videoSegmentationURL.size) {
                        if (videoSegmentationURL[i] == videoURL) {
                            mVideoView.setPlayFromZeroPositionOnce(true)
                            startPlayUrl(videoSegmentationURL[i - 1], HashMap())
                            return
                        }
                    }
                }
                this@PlayActivity.playPrevious()
            }

            override fun changeParse(pb: ParseBean?) {
                autoRetryCount = 0
                pb?.let { doParse(it) }
            }

            override fun updatePlayerCfg() {
                mVodInfo?.playerCfg = mVodPlayerCfg.toString()
                EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_REFRESH, mVodPlayerCfg))
            }

            override fun replay(replay: Boolean) {
                autoRetryCount = 0
                play(replay)
            }

            override fun errReplay() {
                errorWithRetry("视频播放出错", false)
            }

            override fun selectSubtitle() {
                try {
                    selectMySubtitle()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun selectAudioTrack() {
                selectMyAudioTrack()
            }

            override fun openVideo() {
                openMyVideo()
            }

            override fun prepared() {
                initSubtitleView()
            }
        })
        mVideoView.setVideoController(mController)
        mVideoView.setmHandler(mHandler)
    }

    private fun initDanmuView() {
        mDanmuView = DanmakuView(this)
        mDanmakuContext = DanmakuContext.create()
        mVideoView.setDanmuView(mDanmuView)
    }

    private fun setDanmuViewSettings(reload: Boolean) {
        val speed = HawkUtils.getDanmuSpeed()
        val alpha = HawkUtils.getDanmuAlpha()
        val sizeScale = HawkUtils.getDanmuSizeScale()
        val maxLine = HawkUtils.getDanmuMaxLine()
        val maxLines = HashMap<Int, Int>()
        maxLines[BaseDanmaku.TYPE_FIX_TOP] = maxLine
        maxLines[BaseDanmaku.TYPE_SCROLL_RL] = maxLine
        maxLines[BaseDanmaku.TYPE_SCROLL_LR] = maxLine
        maxLines[BaseDanmaku.TYPE_FIX_BOTTOM] = maxLine
        mDanmakuContext.setMaximumLines(maxLines).setScrollSpeedFactor(speed).setDanmakuTransparency(alpha).setScaleTextSize(sizeScale)
        mDanmakuContext.setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 3f).setDanmakuMargin(8)
        if (reload) {
            executorService?.shutdownNow()
            executorService = Executors.newSingleThreadExecutor()
            executorService?.execute {
                mDanmuView.release()
                mDanmuView.prepare(Parser(danmuText), mDanmakuContext)
                App.post {
                    if (mVideoView.isPlaying) {
                        mDanmuView.seekTo(mVideoView.currentPosition)
                    }
                }
            }
        }
    }

    fun setSubtitle(path: String?) {
        if (!path.isNullOrEmpty()) {
            mController.mSubtitleView.visibility = View.GONE
            mController.mSubtitleView.setSubtitlePath(path)
            mController.mSubtitleView.visibility = View.VISIBLE
        }
    }

    fun selectMySubtitle() {
        val subtitleDialog = SubtitleDialog(this)
        subtitleDialog.setSubtitleViewListener(object : SubtitleDialog.SubtitleViewListener {
            override fun setTextSize(size: Int) {
                mController.mSubtitleView.setTextSize(size.toFloat())
            }

            override fun setSubtitleDelay(milliseconds: Int) {
                mController.mSubtitleView.setSubtitleDelay(milliseconds)
            }

            override fun selectInternalSubtitle() {
                selectMyInternalSubtitle()
            }

            override fun setTextStyle(style: Int) {
                setSubtitleViewTextStyle(style)
            }
        })
        subtitleDialog.setSearchSubtitleListener(object : SubtitleDialog.SearchSubtitleListener {
            override fun openSearchSubtitleDialog() {
                val searchSubtitleDialog = SearchSubtitleDialog(this@PlayActivity)
                searchSubtitleDialog.setSubtitleLoader { subtitle ->
                    runOnUiThread {
                        val zimuUrl = subtitle.url
                        LOG.i("Remote SubtitleBean Url: $zimuUrl")
                        setSubtitle(zimuUrl)
                        searchSubtitleDialog.dismiss()
                    }
                }
                val playFlag = mVodInfo?.playFlag ?: ""
                if (playFlag.contains("Ali") || playFlag.contains("parse")) {
                    searchSubtitleDialog.setSearchWord(mVodInfo?.playNote ?: "")
                } else {
                    searchSubtitleDialog.setSearchWord(mVodInfo?.name ?: "")
                }
                searchSubtitleDialog.show()
            }
        })
        subtitleDialog.setLocalFileChooserListener {
            ChooserDialog(this@PlayActivity)
                .withFilter(false, false, "srt", "ass", "scc", "stl", "ttml")
                .withStartFile("/storage/emulated/0/Download")
                .withChosenListener { path, _ ->
                    LOG.i("Local SubtitleBean Path: $path")
                    setSubtitle(path)
                }
                .build()
                .show()
        }
        subtitleDialog.show()
    }

    fun setSubtitleViewTextStyle(style: Int) {
        SubtitleHelper.upTextStyle(mController.mSubtitleView, style)
    }

    fun selectMyInternalSubtitle() {
        val mediaPlayer = mVideoView.mediaPlayer
        var trackInfo: TrackInfo? = null
        if (mediaPlayer is EXOmPlayer) trackInfo = mediaPlayer.trackInfo
        if (mediaPlayer is IjkmPlayer) trackInfo = mediaPlayer.trackInfo
        if (trackInfo == null) {
            return
        }
        val bean = trackInfo.subtitle
        if (bean.size < 1) {
            Toast.makeText(mContext, getString(R.string.vod_sub_na), Toast.LENGTH_SHORT).show()
            return
        }
        val dialog = SelectDialog<TrackInfoBean>(this)
        dialog.setTip(getString(R.string.vod_sub_sel))
        dialog.setAdapter(null, object : SelectDialogAdapter.SelectDialogInterface<TrackInfoBean> {
            override fun click(value: TrackInfoBean, pos: Int) {
                mController.mSubtitleView.visibility = View.VISIBLE
                try {
                    for (subtitle in bean) {
                        subtitle.selected = subtitle.trackId == value.trackId
                    }
                    mediaPlayer.pause()
                    val progress = mediaPlayer.currentPosition
                    if (mediaPlayer is IjkmPlayer) {
                        mController.mSubtitleView.destroy()
                        mController.mSubtitleView.clearSubtitleCache()
                        mController.mSubtitleView.isInternal = true
                        mediaPlayer.setTrack(value.trackId)
                        Handler(Looper.getMainLooper()).postDelayed({
                            mediaPlayer.seekTo(progress)
                            mediaPlayer.start()
                        }, 800)
                    }
                    if (mediaPlayer is EXOmPlayer) {
                        mController.mSubtitleView.destroy()
                        mController.mSubtitleView.clearSubtitleCache()
                        mController.mSubtitleView.isInternal = true
                        mediaPlayer.selectExoTrack(value)
                        Handler(Looper.getMainLooper()).postDelayed({
                            mediaPlayer.seekTo(progress)
                            mediaPlayer.start()
                            mController.startProgress()
                        }, 800)
                    }
                    dialog.dismiss()
                } catch (e: Exception) {
                    LOG.e("切换内置字幕出错")
                }
            }

            override fun getDisplay(val_: TrackInfoBean): String = val_.name + if (TextUtils.isEmpty(val_.language)) "" else " " + val_.language
        }, object : DiffUtil.ItemCallback<TrackInfoBean>() {
            override fun areItemsTheSame(oldItem: TrackInfoBean, newItem: TrackInfoBean): Boolean = oldItem.trackId == newItem.trackId
            override fun areContentsTheSame(oldItem: TrackInfoBean, newItem: TrackInfoBean): Boolean = oldItem.trackId == newItem.trackId
        }, bean, trackInfo.getSubtitleSelected(false))
        dialog.show()
    }

    fun selectMyAudioTrack() {
        val mediaPlayer = mVideoView.mediaPlayer
        var trackInfo: TrackInfo? = null
        if (mediaPlayer is IjkmPlayer) trackInfo = mediaPlayer.trackInfo
        if (mediaPlayer is EXOmPlayer) trackInfo = mediaPlayer.trackInfo
        if (mediaPlayer is AndroidMediaPlayer) trackInfo = mediaPlayer.trackInfo
        if (trackInfo == null) {
            Toast.makeText(mContext, getString(R.string.vod_no_audio), Toast.LENGTH_SHORT).show()
            return
        }
        val bean = trackInfo.audio
        if (bean.size < 1) {
            Toast.makeText(mContext, getString(R.string.vod_no_audio), Toast.LENGTH_SHORT).show()
            return
        }
        val dialog = SelectDialog<TrackInfoBean>(this)
        dialog.setTip(getString(R.string.vod_audio))
        dialog.setAdapter(null, object : SelectDialogAdapter.SelectDialogInterface<TrackInfoBean> {
            override fun click(value: TrackInfoBean, pos: Int) {
                try {
                    for (audio in bean) {
                        audio.selected = audio.trackId == value.trackId
                    }
                    mediaPlayer.pause()
                    val progress = mediaPlayer.currentPosition
                    if (mediaPlayer is IjkmPlayer) mediaPlayer.setTrack(value.trackId)
                    if (mediaPlayer is EXOmPlayer) mediaPlayer.selectExoTrack(value)
                    if (mediaPlayer is AndroidMediaPlayer) mediaPlayer.setTrack(value.trackId)
                    Handler(Looper.getMainLooper()).postDelayed({
                        mediaPlayer.seekTo(progress)
                        mediaPlayer.start()
                        mController.startProgress()
                    }, 800)
                    dialog.dismiss()
                } catch (e: Exception) {
                    LOG.e("切换音轨出错")
                }
            }

            override fun getDisplay(val_: TrackInfoBean): String {
                var name = val_.name.replace("AUDIO,", "").replace("N/A,", "").replace(" ", "")
                return name + if (TextUtils.isEmpty(val_.language)) "" else " " + val_.language
            }
        }, object : DiffUtil.ItemCallback<TrackInfoBean>() {
            override fun areItemsTheSame(oldItem: TrackInfoBean, newItem: TrackInfoBean): Boolean = oldItem.trackId == newItem.trackId
            override fun areContentsTheSame(oldItem: TrackInfoBean, newItem: TrackInfoBean): Boolean = oldItem.trackId == newItem.trackId
        }, bean, trackInfo.getAudioSelected(false))
        dialog.show()
    }

    fun openMyVideo() {
        val i = Intent(Intent.ACTION_VIEW)
        i.addCategory(Intent.CATEGORY_DEFAULT)
        val url = videoURL ?: return
        i.setDataAndType(Uri.parse(url), "video/*")
        startActivity(Intent.createChooser(i, "Open Video with ..."))
    }

    fun setTip(msg: String, loading: Boolean, err: Boolean) {
        runOnUiThread {
            mPlayLoadTip.text = msg
            mPlayLoadTip.visibility = View.VISIBLE
            mPlayLoading.visibility = if (loading) View.VISIBLE else View.GONE
            mPlayLoadErr.visibility = if (err) View.VISIBLE else View.GONE
        }
    }

    fun hideTip() {
        mPlayLoadTip.visibility = View.GONE
        mPlayLoading.visibility = View.GONE
        mPlayLoadErr.visibility = View.GONE
    }

    fun errorWithRetry(err: String, finish: Boolean) {
        if (!autoRetry()) {
            runOnUiThread {
                if (finish) {
                    Toast.makeText(mContext, err, Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    setTip(err, false, true)
                }
            }
        }
    }

    private fun yxdm(url: String, headers: Map<String, String>?): Boolean {
        if (url.startsWith("https://www.ziyuantt.com/") && url.endsWith(".mp4")) {
            val st = url.indexOf("&url=")
            if (st > 1) {
                val urls = url.substring(st + 5).split("\\|".toRegex()).toTypedArray()
                if (urls.size < 2) return false
                stopLoadWebView(false)
                videoSegmentationURL.clear()
                videoSegmentationURL.addAll(Arrays.asList(*urls))
                val hm = HashMap<String, String>()
                headers?.forEach { (k, v) -> hm[k] = " $v" }
                loadFoundVideoUrls.add(urls[0])
                loadFoundVideoUrlsHeader[videoSegmentationURL[0]] = hm
                startPlayUrl(videoSegmentationURL[0], hm)
                return true
            }
        }
        return false
    }

    fun playUrl(url: String, headers: HashMap<String, String>?) {
        if (!Hawk.get(HawkConfig.VIDEO_PURIFY, true) || (!url.contains("://127.0.0.1/") && !url.contains(".m3u8"))) {
            startPlayUrl(url, headers)
            return
        }
        OkGo.getInstance().cancelTag("m3u8-1")
        OkGo.getInstance().cancelTag("m3u8-2")
        val hheaders = HttpHeaders()
        headers?.forEach { (k, v) -> hheaders.put(k, v) }
        OkGo.get<String>(url).tag("m3u8-1").headers(hheaders).execute(object : AbsCallback<String>() {
            override fun onSuccess(response: Response<String>) {
                val content = response.body() ?: ""
                if (!content.startsWith("#EXTM3U")) {
                    startPlayUrl(url, headers)
                    return
                }
                val lines = if (content.contains("\r\n")) content.split("\r\n".toRegex(), 10).toTypedArray() else content.split("\n".toRegex(), 10).toTypedArray()
                var forwardurl = ""
                var dealedFirst = false
                for (line in lines) {
                    if (line != "" && line[0] != '#') {
                        if (dealedFirst) {
                            forwardurl = ""
                            break
                        }
                        if (line.endsWith(".m3u8") || line.contains(".m3u8?")) {
                            forwardurl = if (line.startsWith("http://") || line.startsWith("https://")) line else if (line[0] == '/') {
                                val ifirst = url.indexOf('/', 9)
                                url.substring(0, ifirst) + line
                            } else {
                                val ilast = url.lastIndexOf('/')
                                url.substring(0, ilast + 1) + line
                            }
                        }
                        dealedFirst = true
                    }
                }
                if (forwardurl == "") {
                    val ilast = url.lastIndexOf('/')
                    RemoteServer.m3u8Content = M3U8.purify(url.substring(0, ilast + 1), content)
                    if (RemoteServer.m3u8Content == null) startPlayUrl(url, headers) else startPlayUrl("http://127.0.0.1:" + RemoteServer.serverPort + "/m3u8", headers)
                    return
                }
                val finalforwardurl = forwardurl
                OkGo.get<String>(forwardurl).tag("m3u8-2").headers(hheaders).execute(object : AbsCallback<String>() {
                    override fun onSuccess(response: Response<String>) {
                        val content2 = response.body() ?: ""
                        val ilast2 = finalforwardurl.lastIndexOf('/')
                        RemoteServer.m3u8Content = M3U8.purify(finalforwardurl.substring(0, ilast2 + 1), content2)
                        if (RemoteServer.m3u8Content == null) startPlayUrl(finalforwardurl, headers) else startPlayUrl("http://127.0.0.1:" + RemoteServer.serverPort + "/m3u8", headers)
                    }

                    override fun convertResponse(response: okhttp3.Response): String? = response.body()?.string()
                    override fun onError(response: Response<String>) {
                        super.onError(response)
                        startPlayUrl(url, headers)
                    }
                })
            }

            override fun convertResponse(response: okhttp3.Response): String? = response.body()?.string()
            override fun onError(response: Response<String>) {
                super.onError(response)
                startPlayUrl(url, headers)
            }
        })
    }

    fun startPlayUrl(url: String?, headers: HashMap<String, String>?) {
        runOnUiThread {
            stopParse()
            mVideoView.release()
            if (url != null) {
                var finalUrl = url
                videoURL = finalUrl
                try {
                    val playerType = mVodPlayerCfg?.optInt("pl", 1) ?: 1
                    extPlay = false
                    if (playerType >= 10) {
                        val playFlag = mVodInfo?.playFlag ?: ""
                        val vs = mVodInfo?.seriesMap?.get(playFlag)?.get(mVodInfo?.playIndex ?: 0)
                        if (vs != null) {
                            val playTitle = (mVodInfo?.name ?: "") + " : " + (vs.name ?: "")
                            setTip("调用外部播放器" + PlayerHelper.getPlayerName(playerType) + "进行播放", true, false)
                            var callResult = false
                            when (playerType) {
                                10 -> {
                                    extPlay = true
                                    callResult = MXPlayer.run(this@PlayActivity, finalUrl, playTitle, playSubtitle, headers)
                                }
                                11 -> {
                                    extPlay = true
                                    callResult = ReexPlayer.run(this@PlayActivity, finalUrl, playTitle, playSubtitle, headers)
                                }
                                12 -> {
                                    extPlay = true
                                    callResult = Kodi.run(this@PlayActivity, finalUrl, playTitle, playSubtitle, headers)
                                }
                            }
                            setTip("调用外部播放器" + PlayerHelper.getPlayerName(playerType) + if (callResult) "成功" else "失败", callResult, !callResult)
                            return@runOnUiThread
                        }
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                hideTip()
                if (finalUrl.startsWith("data:application/dash+xml;base64,")) {
                    PlayerHelper.updateCfg(mVideoView, mVodPlayerCfg, 2)
                    App.getInstance().setDashData(finalUrl.split("base64,".toRegex()).toTypedArray()[1])
                    finalUrl = ControlManager.get().getAddress(true) + "dash/proxy.mpd"
                } else if (finalUrl.contains(".mpd") || finalUrl.contains("type=mpd")) {
                    PlayerHelper.updateCfg(mVideoView, mVodPlayerCfg, 2)
                } else {
                    PlayerHelper.updateCfg(mVideoView, mVodPlayerCfg)
                }
                mVideoView.setProgressKey(progressKey)
                if (headers != null) mVideoView.setUrl(finalUrl, headers) else mVideoView.setUrl(finalUrl)
                mVideoView.start()
                mController.resetSpeed()
            }
        }
    }

    private fun initSubtitleView() {
        val mediaPlayer = mVideoView.mediaPlayer
        var trackInfo: TrackInfo? = null
        if (mediaPlayer is IjkmPlayer) {
            trackInfo = mediaPlayer.trackInfo
            if (trackInfo != null && trackInfo.subtitle.size > 0) mController.mSubtitleView.hasInternal = true
            mediaPlayer.setOnTimedTextListener { _, text ->
                if (mController.mSubtitleView.isInternal && text != null) {
                    val subtitle = Subtitle()
                    subtitle.content = text.text
                    mController.mSubtitleView.onSubtitleChanged(subtitle)
                }
            }
        }
        if (mediaPlayer is EXOmPlayer) {
            trackInfo = mediaPlayer.trackInfo
            if (trackInfo != null && trackInfo.subtitle.size > 0) mController.mSubtitleView.hasInternal = true
            mediaPlayer.setOnTimedTextListener(object : Player.Listener {
                override fun onCues(cues: List<Cue>) {
                    if (cues.isNotEmpty()) {
                        val ss = cues[0].text
                        if (ss != null && mController.mSubtitleView.isInternal) {
                            val subtitle = Subtitle()
                            subtitle.content = ss.toString()
                            mController.mSubtitleView.onSubtitleChanged(subtitle)
                        }
                    } else {
                        val subtitle = Subtitle()
                        subtitle.content = ""
                        mController.mSubtitleView.onSubtitleChanged(subtitle)
                    }
                }
            })
        }
        mController.mSubtitleView.bindToMediaPlayer(mediaPlayer)
        mController.mSubtitleView.setPlaySubtitleCacheKey(subtitleCacheKey)
        val subtitlePathCache = CacheManager.getCache(MD5.string2MD5(subtitleCacheKey)) as? String
        if (!subtitlePathCache.isNullOrEmpty()) {
            mController.mSubtitleView.setSubtitlePath(subtitlePathCache)
        } else {
            if (!playSubtitle.isNullOrEmpty()) {
                mController.mSubtitleView.setSubtitlePath(playSubtitle)
            } else if (mController.mSubtitleView.hasInternal) {
                mController.mSubtitleView.isInternal = true
                if (mediaPlayer is IjkmPlayer && trackInfo != null && trackInfo.subtitle.size > 0) {
                    val subtitleTrackList = trackInfo.subtitle
                    val selectedIndex = trackInfo.getSubtitleSelected(true)
                    var hasCh = false
                    for (subtitleTrackInfoBean in subtitleTrackList) {
                        val lowerLang = subtitleTrackInfoBean.language.lowercase(Locale.getDefault())
                        if (lowerLang.startsWith("zh") || lowerLang.startsWith("ch")) {
                            hasCh = true
                            if (selectedIndex != subtitleTrackInfoBean.trackId) {
                                mediaPlayer.setTrack(subtitleTrackInfoBean.trackId)
                                break
                            }
                        }
                    }
                    if (!hasCh) mediaPlayer.setTrack(subtitleTrackList[0].trackId)
                }
            }
        }
    }

    private fun initViewModel() {
        sourceViewModel = ViewModelProvider(this).get(SourceViewModel::class.java)
        sourceViewModel.playResult.observeForever(mObserverPlayResult)
    }

    private val mObserverPlayResult = Observer<JSONObject> { info ->
        if (info != null) {
            try {
                progressKey = info.optString("proKey", null)
                val parse = info.optString("parse", "1") == "1"
                val jx = info.optString("jx", "0") == "1"
                playSubtitle = info.optString("subt", "")
                if (playSubtitle!!.isEmpty() && info.has("subs")) {
                    try {
                        val obj = info.getJSONArray("subs").optJSONObject(0)
                        var url = obj.optString("url", "")
                        if (!TextUtils.isEmpty(url) && !FileUtils.hasExtension(url)) {
                            val format = obj.optString("format", "")
                            val name = obj.optString("name", "字幕")
                            var ext = ".srt"
                            when (format) {
                                "text/x-ssa" -> ext = ".ass"
                                "text/vtt" -> ext = ".vtt"
                                "application/x-subrip" -> ext = ".srt"
                                "text/lrc" -> ext = ".lrc"
                            }
                            val filename = name + if (name.lowercase(Locale.getDefault()).endsWith(ext)) "" else ext
                            url += "#" + URLEncoder.encode(filename, "UTF-8")
                        }
                        playSubtitle = url
                    } catch (ignored: Throwable) {}
                }
                subtitleCacheKey = info.optString("subtKey", null)
                val playUrl = info.optString("playUrl", "")
                val msg = info.optString("msg", "")
                if (msg.isNotEmpty()) Toast.makeText(this@PlayActivity, msg, Toast.LENGTH_SHORT).show()
                val flag = info.optString("flag")
                val url = info.getString("url")
                val danmaku = info.optString("danmaku")
                var headers: HashMap<String, String>? = null
                webUserAgent = null
                webHeaderMap = null
                if (info.has("header")) {
                    try {
                        val hds = JSONObject(info.getString("header"))
                        val keys = hds.keys()
                        val hMap = HashMap<String, String>()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            hMap[key] = hds.getString(key)
                            if (key.equals("user-agent", ignoreCase = true)) {
                                webUserAgent = hds.getString(key).trim()
                            } else if (key.equals("cookie", ignoreCase = true)) {
                                hds.getString(key).split(";").forEach { split ->
                                    CookieManager.getInstance().setCookie(url, split.trim())
                                }
                            }
                        }
                        headers = hMap
                        webHeaderMap = hMap
                    } catch (ignored: Throwable) {}
                }
                if (parse || jx) {
                    val userJxList = (playUrl.isEmpty() && ApiConfig.get().vipParseFlags.contains(flag)) || jx
                    initParse(flag, userJxList, playUrl, url)
                } else {
                    mController.showParse(false)
                    playUrl(playUrl + url, headers)
                }
                checkDanmu(danmaku)
            } catch (th: Throwable) {
                errorWithRetry("获取播放信息错误", true)
            }
        } else {
            errorWithRetry("获取播放信息错误", true)
        }
    }

    private fun checkDanmu(danmaku: String?) {
        danmuText = danmaku
        mDanmuView.release()
        mDanmuView.visibility = if (TextUtils.isEmpty(danmuText) || !HawkUtils.getDanmuOpen()) View.GONE else View.VISIBLE
        if (TextUtils.isEmpty(danmuText) || !HawkUtils.getDanmuOpen() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode)) return
        if (!danmuText.isNullOrEmpty()) {
            mController.setHasDanmu(true)
            setDanmuViewSettings(true)
        }
    }

    private fun initData() {
        val intent = intent
        if (intent?.extras != null) {
            val bundle = intent.extras!!
            mVodInfo = bundle.getSerializable("VodInfo") as VodInfo
            sourceKey = bundle.getString("sourceKey")
            sourceBean = ApiConfig.get().getSource(sourceKey)
            initPlayerCfg()
            play(false)
        }
    }

    fun initPlayerCfg() {
        mVodPlayerCfg = try { JSONObject(mVodInfo?.playerCfg ?: "{}") } catch (th: Throwable) { JSONObject() }
        try {
            if (mVodPlayerCfg?.has("pl") == false) {
                var playType = Hawk.get(HawkConfig.PLAY_TYPE, 1)
                if (HawkUtils.getVodPlayerPreferredConfigurationFile() && (sourceBean?.playerType ?: -1) != -1) {
                    playType = sourceBean!!.playerType
                }
                mVodPlayerCfg?.put("pl", playType)
            }
            if (mVodPlayerCfg?.has("pr") == false) mVodPlayerCfg?.put("pr", Hawk.get(HawkConfig.PLAY_RENDER, 0))
            if (mVodPlayerCfg?.has("ijk") == false) mVodPlayerCfg?.put("ijk", Hawk.get(HawkConfig.IJK_CODEC, ""))
            if (mVodPlayerCfg?.has("sc") == false) mVodPlayerCfg?.put("sc", Hawk.get(HawkConfig.PLAY_SCALE, 0))
            if (mVodPlayerCfg?.has("sp") == false) mVodPlayerCfg?.put("sp", 1.0)
            if (mVodPlayerCfg?.has("st") == false) mVodPlayerCfg?.put("st", 0)
            if (mVodPlayerCfg?.has("et") == false) mVodPlayerCfg?.put("et", 0)
        } catch (ignored: Throwable) {}
        mController.setPlayerConfig(mVodPlayerCfg)
    }

    fun initPlayerDrive() {
        try {
            if (mVodPlayerCfg?.has("pl") == false) mVodPlayerCfg?.put("pl", Hawk.get(HawkConfig.PLAY_TYPE, 1))
            if (mVodPlayerCfg?.has("pr") == false) mVodPlayerCfg?.put("pr", Hawk.get(HawkConfig.PLAY_RENDER, 0))
            if (mVodPlayerCfg?.has("ijk") == false) mVodPlayerCfg?.put("ijk", Hawk.get(HawkConfig.IJK_CODEC, ""))
            if (mVodPlayerCfg?.has("sc") == false) mVodPlayerCfg?.put("sc", Hawk.get(HawkConfig.PLAY_SCALE, 0))
            if (mVodPlayerCfg?.has("sp") == false) mVodPlayerCfg?.put("sp", 1.0)
            if (mVodPlayerCfg?.has("st") == false) mVodPlayerCfg?.put("st", 0)
            if (mVodPlayerCfg?.has("et") == false) mVodPlayerCfg?.put("et", 0)
        } catch (ignored: Throwable) {}
        mController.setPlayerConfig(mVodPlayerCfg)
    }

    private var extPlay = false
    override fun onUserLeaveHint() {
        if (supportsPiPMode() && !extPlay && Hawk.get(HawkConfig.BACKGROUND_PLAY_TYPE, 0) == 2) {
            val vWidth = mVideoView.videoSize[0]
            val vHeight = mVideoView.videoSize[1]
            val ratio = if (vWidth != 0) {
                var h = vHeight
                if (vWidth.toDouble() / vHeight > 2.39) h = (vWidth.toDouble() / 2.35).toInt()
                Rational(vWidth, h)
            } else Rational(16, 9)
            val actions = ArrayList<android.app.RemoteAction>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                actions.add(generateRemoteAction(android.R.drawable.ic_media_previous, BROADCAST_ACTION_PREV, "Prev", "Play Previous"))
                actions.add(generateRemoteAction(android.R.drawable.ic_media_play, BROADCAST_ACTION_PLAYPAUSE, "Play/Pause", "Play or Pause"))
                actions.add(generateRemoteAction(android.R.drawable.ic_media_next, BROADCAST_ACTION_NEXT, "Next", "Play Next"))
                val params = PictureInPictureParams.Builder().setAspectRatio(ratio).setActions(actions).build()
                enterPictureInPictureMode(params)
            }
            mController.hideBottom()
            mVideoView.postDelayed({ if (!mVideoView.isPlaying) mController.togglePlay() }, 400)
        }
        super.onUserLeaveHint()
    }

    override fun onBackPressed() {
        if (mController.onBackPressed()) return
        super.onBackPressed()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (mController.onKeyEvent(event)) return true
        return super.dispatchKeyEvent(event)
    }

    private var onStopCalled = false
    override fun onResume() {
        super.onResume()
        onStopCalled = false
        mVideoView.resume()
    }

    override fun onStop() {
        super.onStop()
        onStopCalled = true
    }

    override fun onPause() {
        super.onPause()
        mVideoView.pause()
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun generateRemoteAction(iconResId: Int, actionCode: Int, title: String, desc: String): android.app.RemoteAction {
        val intent = PendingIntent.getBroadcast(this, actionCode, Intent(BROADCAST_ACTION).putExtra("action", actionCode), PendingIntent.FLAG_IMMUTABLE)
        val icon = Icon.createWithResource(this, iconResId)
        return android.app.RemoteAction(icon, title, desc, intent)
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (supportsPiPMode() && isInPictureInPictureMode) {
            pipActionReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent?) {
                    if (intent == null || intent.action != BROADCAST_ACTION) return
                    when (intent.getIntExtra("action", 1)) {
                        BROADCAST_ACTION_PREV -> playPrevious()
                        BROADCAST_ACTION_PLAYPAUSE -> mController.togglePlay()
                        BROADCAST_ACTION_NEXT -> playNext(false)
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) registerReceiver(pipActionReceiver, IntentFilter(BROADCAST_ACTION), RECEIVER_NOT_EXPORTED) else registerReceiver(pipActionReceiver, IntentFilter(BROADCAST_ACTION))
        } else {
            if (onStopCalled) mVideoView.release()
            pipActionReceiver?.let { unregisterReceiver(it) }
            pipActionReceiver = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
        executorService?.shutdownNow()
        sourceViewModel.playResult.removeObserver(mObserverPlayResult)
        mVideoView.release()
        stopLoadWebView(true)
        stopParse()
        Thunder.stop(false)
        Jianpian.finish()
        App.getInstance().setDashData(null)
    }

    private var mVodInfo: VodInfo? = null
    private var mVodPlayerCfg: JSONObject? = null
    private var sourceKey: String? = null
    private var sourceBean: SourceBean? = null

    fun playNext(inProgress: Boolean) {
        val series = mVodInfo?.seriesMap?.get(mVodInfo?.playFlag ?: "")
        if (series == null || mVodInfo!!.playIndex + 1 >= series.size) {
            Toast.makeText(this, if (mVodInfo!!.reverseSort) "已经是第一集了" else "已经是最后一集了", Toast.LENGTH_SHORT).show()
            if (inProgress) finish()
            return
        }
        mVodInfo!!.playIndex++
        if (mVodInfo!!.playGroupCount > 0) {
            mVodInfo!!.playGroup += mVodInfo!!.playIndex / mVodInfo!!.playGroupCount
            mVodInfo!!.playIndex %= mVodInfo!!.playGroupCount
        }
        play(false)
    }

    fun playPrevious() {
        val series = mVodInfo?.seriesMap?.get(mVodInfo?.playFlag ?: "")
        if (series == null || mVodInfo!!.playIndex - 1 < 0) {
            Toast.makeText(this, if (mVodInfo!!.reverseSort) "已经是最后一集了" else "已经是第一集了", Toast.LENGTH_SHORT).show()
            return
        }
        if (mVodInfo!!.playIndex == 0) {
            if (mVodInfo!!.playGroup > 0) {
                mVodInfo!!.playGroup--
                mVodInfo!!.playIndex = mVodInfo!!.playGroupCount - 1
            }
        } else mVodInfo!!.playIndex--
        play(false)
    }

    private var autoRetryCount = 0
    fun autoRetry(): Boolean {
        switchPlayer()
        loadFoundVideoUrls.poll()?.let { url ->
            playUrl(url, loadFoundVideoUrlsHeader[url])
            return true
        }
        return if (autoRetryCount < 1) {
            autoRetryCount++
            play(false)
            true
        } else {
            autoRetryCount = 0
            false
        }
    }

    fun switchPlayer() {
        try {
            val playerType = if (mVodPlayerCfg?.optInt("pl", 1) == 1) 2 else 1
            mVodPlayerCfg?.put("pl", playerType)
            mController.setPlayerConfig(mVodPlayerCfg)
            mVodInfo?.playerCfg = mVodPlayerCfg.toString()
            EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_REFRESH, mVodPlayerCfg))
        } catch (ignored: Exception) {}
    }

    fun initParseLoadFound() {
        loadFoundCount.set(0)
        loadFoundVideoUrls = LinkedList()
        loadFoundVideoUrlsHeader = HashMap()
    }

    fun play(reset: Boolean) {
        val seriesMap = mVodInfo?.seriesMap
        val playFlag = mVodInfo?.playFlag ?: ""
        val vs = seriesMap?.get(playFlag)?.get(mVodInfo?.playIndex ?: 0) ?: return

        EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_REFRESH, mVodInfo!!.playIndex))
        EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_REFRESH_NOTIFY, (mVodInfo!!.name ?: "") + "&&" + (vs.name ?: "")))
        setTip("正在获取播放信息", true, false)
        mController.setTitle((mVodInfo!!.name ?: "") + " : " + (vs.name ?: ""))
        RemoteServer.vodName = mVodInfo!!.name
        RemoteServer.artist = vs.name
        stopParse()
        initParseLoadFound()
        mVideoView.release()
        subtitleCacheKey = (mVodInfo!!.sourceKey ?: "") + "-" + (mVodInfo!!.id ?: "") + "-" + (mVodInfo!!.playFlag ?: "") + "-" + mVodInfo!!.playIndex + "-" + (vs.name ?: "") + "-subt"
        progressKey = (mVodInfo!!.sourceKey ?: "") + (mVodInfo!!.id ?: "") + (mVodInfo!!.playFlag ?: "") + mVodInfo!!.playIndex

        val pKey = progressKey
        val sKey = subtitleCacheKey
        if (reset) {
            if (pKey != null) CacheManager.delete(MD5.string2MD5(pKey), 0)
            if (sKey != null) CacheManager.delete(MD5.string2MD5(sKey), "")
        }
        val vUrl = vs.url ?: ""
        if (vUrl.startsWith("tvbox-drive://")) {
            progressKey = vUrl.replace("tvbox-drive://", "")
            initPlayerDrive()
            mController.showParse(false)
            var headers: HashMap<String, String>? = null
            val playerCfg = mVodInfo?.playerCfg
            if (!playerCfg.isNullOrEmpty()) {
                val playerConfig = JsonParser.parseString(playerCfg).asJsonObject
                if (playerConfig.has("headers")) {
                    val heads = HashMap<String, String>()
                    playerConfig.getAsJsonArray("headers").forEach { el ->
                        val h = el.asJsonObject
                        heads[h.get("name").asString] = h.get("value").asString
                    }
                    headers = heads
                }
            }
            playUrl(vUrl.replace("tvbox-drive://", ""), headers)
            return
        }
        if (Jianpian.isJpUrl(vUrl)) {
            mController.showParse(false)
            playUrl(Jianpian.JPUrlDec(if (vUrl.startsWith("tvbox-xg:")) vUrl.substring(9) else vUrl), null)
            return
        }
        if (Thunder.play(vUrl, object : Thunder.ThunderCallback {
            override fun status(code: Int, info: String) = setTip(info, code >= 0, code < 0)
            override fun list(urlMap: Map<Int, String>) {}
            override fun play(url: String) = playUrl(url, null)
        })) {
            mController.showParse(false)
            return
        }
        sourceViewModel.getPlay(sourceKey, mVodInfo?.playFlag, progressKey, vs.url, subtitleCacheKey)
    }

    private var playSubtitle: String? = null
    private var subtitleCacheKey: String? = null
    private var progressKey: String? = null
    private var parseFlag: String? = null
    private var webUrl: String? = null
    private var webUserAgent: String? = null
    private var webHeaderMap: Map<String, String>? = null

    private fun initParse(flag: String, useParse: Boolean, playUrl: String, url: String) {
        parseFlag = flag
        webUrl = url
        var parseBean: ParseBean? = null
        mController.showParse(useParse)
        if (useParse) parseBean = ApiConfig.get().defaultParse else {
            if (playUrl.startsWith("json:")) {
                parseBean = ParseBean().apply { type = 1; this.url = playUrl.substring(5) }
            } else if (playUrl.startsWith("parse:")) {
                val redirect = playUrl.substring(6)
                parseBean = ApiConfig.get().parseBeanList.find { it.name == redirect }
            }
            if (parseBean == null) parseBean = ParseBean().apply { type = 0; this.url = playUrl }
        }
        parseBean?.let { doParse(it) }
    }

    @Throws(JSONException::class)
    fun jsonParse(input: String?, json: String?): JSONObject? {
        val data = JSONObject(json ?: "{}")
        var url = if (data.has("data")) data.getJSONObject("data").getString("url") else data.getString("url")
        if (url.startsWith("//")) url = "http:$url"
        if (!url.startsWith("http")) return null
        val headers = JSONObject()
        data.optString("user-agent", "").trim().let { if (it.isNotEmpty()) headers.put("User-Agent", " $it") }
        data.optString("referer", "").trim().let { if (it.isNotEmpty()) headers.put("Referer", " $it") }
        return JSONObject().apply { put("header", headers); put("url", url) }
    }

    fun stopParse() {
        mHandler.removeMessages(100)
        stopLoadWebView(false)
        OkGo.getInstance().cancelTag("json_jx")
        parseThreadPool?.shutdownNow()
        parseThreadPool = null
    }

    private var parseThreadPool: ExecutorService? = null
    private fun encodeUrl(url: String): String = try { URLEncoder.encode(url, "UTF-8") } catch (e: Exception) { url }

    private fun doParse(pb: ParseBean) {
        stopParse()
        initParseLoadFound()
        when (pb.type) {
            4 -> parseMix(pb, true)
            0 -> {
                setTip("正在嗅探播放地址", true, false)
                mHandler.sendEmptyMessageDelayed(100, 20000)
                pb.ext?.let { ext ->
                    try {
                        val obj = JSONObject(ext)
                        if (obj.has("header")) {
                            val hds = obj.optJSONObject("header")
                            val hm = HashMap<String, String>()
                            hds?.keys()?.forEach { key ->
                                if (key.equals("user-agent", true)) webUserAgent = hds.getString(key).trim() else hm[key] = hds.optString(key, "")
                            }
                            if (hm.isNotEmpty()) webHeaderMap = hm
                        }
                    } catch (ignored: Throwable) {}
                }
                loadWebView(pb.url + (webUrl ?: ""))
            }
            1 -> {
                setTip("正在解析播放地址", true, false)
                val hds = HttpHeaders()
                pb.ext?.let { ext ->
                    try {
                        val obj = JSONObject(ext)
                        if (obj.has("header")) {
                            val head = obj.optJSONObject("header")
                            head?.keys()?.forEach { key -> hds.put(key, head.optString(key, "")) }
                        }
                    } catch (ignored: Throwable) {}
                }
                OkGo.get<String>(pb.url + encodeUrl(webUrl!!)).tag("json_jx").headers(hds).execute(object : AbsCallback<String>() {
                    override fun convertResponse(response: okhttp3.Response): String? = response.body()?.string() ?: throw IllegalStateException("网络请求错误")
                    override fun onSuccess(response: Response<String>) {
                        try {
                            val rs = jsonParse(webUrl, response.body())!!
                            var headers: HashMap<String, String>? = null
                            if (rs.has("header")) {
                                val head = rs.getJSONObject("header")
                                val hMap = HashMap<String, String>()
                                head.keys().forEach { key -> hMap[key] = head.getString(key) }
                                headers = hMap
                            }
                            playUrl(rs.getString("url"), headers)
                        } catch (e: Exception) {
                            errorWithRetry("解析错误", false)
                        }
                    }
                    override fun onError(response: Response<String>) {
                        super.onError(response)
                        errorWithRetry("解析错误", false)
                    }
                })
            }
            2 -> {
                setTip("正在解析播放地址", true, false)
                parseThreadPool = Executors.newSingleThreadExecutor()
                val jxs = LinkedHashMap<String, String>()
                ApiConfig.get().parseBeanList.filter { it.type == 1 }.forEach { jxs[it.name] = it.mixUrl() }
                parseThreadPool?.execute {
                    val rs = ApiConfig.get().jsonExt(pb.url, jxs, webUrl)
                    if (rs == null || !rs.has("url") || rs.optString("url").isEmpty()) setTip("解析错误", false, true) else {
                        var headers: HashMap<String, String>? = null
                        if (rs.has("header")) {
                            val head = rs.getJSONObject("header")
                            val hMap = HashMap<String, String>()
                            head.keys().forEach { key -> hMap[key] = head.getString(key) }
                            headers = hMap
                        }
                        if (rs.has("jxFrom")) runOnUiThread { Toast.makeText(mContext, "解析来自:${rs.optString("jxFrom")}", Toast.LENGTH_SHORT).show() }
                        if (rs.optInt("parse", 0) == 1) loadUrl(DefaultConfig.checkReplaceProxy(rs.optString("url", ""))) else playUrl(rs.optString("url", ""), headers)
                    }
                }
            }
            3 -> parseMix(pb, false)
        }
    }

    private fun parseMix(pb: ParseBean, isSuper: Boolean) {
        setTip("正在解析播放地址", true, false)
        parseThreadPool = Executors.newSingleThreadExecutor()
        val jxs = LinkedHashMap<String, HashMap<String, String>>()
        var extendName = ""
        ApiConfig.get().parseBeanList.forEach { p ->
            val data = HashMap<String, String>()
            data["url"] = p.url
            if (p.url == pb.url) extendName = p.name
            data["type"] = p.type.toString()
            data["ext"] = p.ext ?: ""
            jxs[p.name] = data
        }
        val finalExtendName = extendName
        parseThreadPool?.execute {
            if (isSuper) {
                val rs = SuperParse.parse(jxs, parseFlag + "123", webUrl)
                if (!rs.has("url") || rs.optString("url").isEmpty()) setTip("解析错误", false, true) else {
                    if (rs.has("parse") && rs.optInt("parse", 0) == 1) {
                        rs.optString("ua").trim().let { if (it.isNotEmpty()) webUserAgent = it }
                        setTip("超级解析中", true, false)
                        runOnUiThread {
                            val url = DefaultConfig.checkReplaceProxy(rs.optString("url", ""))
                            stopParse()
                            mHandler.sendEmptyMessageDelayed(100, 20000)
                            loadWebView(url)
                        }
                        parseThreadPool?.execute { rsJsonJx(SuperParse.doJsonJx(webUrl), true) }
                    } else rsJsonJx(rs, false)
                }
            } else {
                val rs = ApiConfig.get().jsonExtMix(parseFlag + "111", pb.url, finalExtendName, jxs, webUrl)
                if (rs == null || !rs.has("url") || rs.optString("url").isEmpty()) setTip("解析错误", false, true) else {
                    if (rs.has("parse") && rs.optInt("parse", 0) == 1) {
                        rs.optString("ua").trim().let { if (it.isNotEmpty()) webUserAgent = it }
                        runOnUiThread {
                            val url = DefaultConfig.checkReplaceProxy(rs.optString("url", ""))
                            stopParse()
                            setTip("正在嗅探播放地址", true, false)
                            mHandler.sendEmptyMessageDelayed(100, 20000)
                            loadWebView(url)
                        }
                    } else rsJsonJx(rs, false)
                }
            }
        }
    }

    private fun rsJsonJx(rs: JSONObject?, isSuper: Boolean) {
        if (isSuper) {
            if (rs == null || !rs.has("url")) return
            stopLoadWebView(false)
        }
        var headers: HashMap<String, String>? = null
        if (rs?.has("header") == true) {
            val head = rs.getJSONObject("header")
            val hMap = HashMap<String, String>()
            head.keys().forEach { key -> hMap[key] = head.getString(key) }
            headers = hMap
        }
        if (rs?.has("jxFrom") == true) runOnUiThread { Toast.makeText(mContext, "解析来自:${rs.optString("jxFrom")}", Toast.LENGTH_SHORT).show() }
        playUrl(rs?.optString("url", "") ?: "", headers)
    }

    private var mXwalkWebView: XWalkView? = null
    private var mSysWebView: WebView? = null
    private val loadedUrls = HashMap<String, Boolean>()
    private var loadFoundVideoUrls = LinkedList<String>()
    private var loadFoundVideoUrlsHeader = HashMap<String, HashMap<String, String>>()
    private val loadFoundCount = AtomicInteger(0)

    fun loadWebView(url: String) {
        if (mSysWebView == null && mXwalkWebView == null) {
            if (!Hawk.get(HawkConfig.PARSE_WEBVIEW, true)) {
                XWalkUtils.tryUseXWalk(mContext, object : XWalkUtils.XWalkState {
                    override fun success() { initWebView((sourceBean?.clickSelector ?: "").isEmpty()); loadUrl(url) }
                    override fun fail() { Toast.makeText(mContext, "XWalkView不兼容，已替换为系统自带WebView", Toast.LENGTH_SHORT).show(); initWebView(true); loadUrl(url) }
                    override fun ignore() { Toast.makeText(mContext, "XWalkView运行组件未下载，已替换为系统自带WebView", Toast.LENGTH_SHORT).show(); initWebView(true); loadUrl(url) }
                })
            } else {
                initWebView(true)
                loadUrl(url)
            }
        } else loadUrl(url)
    }

    fun initWebView(useSystemWebView: Boolean) {
        if (useSystemWebView) {
            mSysWebView = MyWebView(mContext).also { configWebViewSys(it) }
        } else {
            mXwalkWebView = MyXWalkView(mContext).also { configWebViewX5(it) }
        }
    }

    fun loadUrl(url: String) {
        runOnUiThread {
            val headers = webHeaderMap
            mXwalkWebView?.let {
                it.stopLoading()
                webUserAgent?.let { ua -> it.settings.userAgentString = ua }
                if (headers != null) it.loadUrl(url, headers) else it.loadUrl(url)
            }
            mSysWebView?.let {
                it.stopLoading()
                webUserAgent?.let { ua -> it.settings.userAgentString = ua }
                if (headers != null) it.loadUrl(url, headers) else it.loadUrl(url)
            }
        }
    }

    fun stopLoadWebView(destroy: Boolean) {
        runOnUiThread {
            mXwalkWebView?.let {
                it.stopLoading()
                it.loadUrl("about:blank")
                if (destroy) {
                    it.removeAllViews()
                    it.onDestroy()
                    mXwalkWebView = null
                }
            }
            mSysWebView?.let {
                it.stopLoading()
                it.loadUrl("about:blank")
                if (destroy) {
                    it.removeAllViews()
                    it.destroy()
                    mSysWebView = null
                }
            }
        }
    }

    fun checkVideoFormat(url: String): Boolean {
        if (url.contains("url=http") || url.contains(".html")) return false
        try {
            if (sourceBean?.type == 3) {
                ApiConfig.get().getCSP(sourceBean)?.let { sp -> if (sp.manualVideoCheck()) return sp.isVideoFormat(url) }
            }
        } catch (ignored: Exception) {}
        return VideoParseRuler.checkIsVideoForParse(webUrl, url)
    }

    inner class MyWebView(context: Context) : WebView(context) {
        override fun setOverScrollMode(mode: Int) {
            super.setOverScrollMode(mode)
            if (mContext is Activity) AutoSize.autoConvertDensityOfCustomAdapt(mContext as Activity, this@PlayActivity)
        }
        override fun dispatchKeyEvent(event: KeyEvent?): Boolean = false
    }

    inner class MyXWalkView(context: Context) : XWalkView(context) {
        override fun setOverScrollMode(mode: Int) {
            super.setOverScrollMode(mode)
            if (mContext is Activity) AutoSize.autoConvertDensityOfCustomAdapt(mContext as Activity, this@PlayActivity)
        }
        override fun dispatchKeyEvent(event: KeyEvent?): Boolean = false
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configWebViewSys(webView: WebView) {
        val layoutParams = if (Hawk.get(HawkConfig.DEBUG_OPEN, false)) ViewGroup.LayoutParams(800, 400) else ViewGroup.LayoutParams(1, 1)
        webView.isFocusable = false
        webView.isFocusableInTouchMode = false
        webView.clearFocus()
        webView.overScrollMode = View.OVER_SCROLL_ALWAYS
        addContentView(webView, layoutParams)
        webView.settings.apply {
            javaScriptEnabled = true
            setNeedInitialFocus(false)
            allowContentAccess = true
            allowFileAccess = true
            allowUniversalAccessFromFileURLs = true
            allowFileAccessFromFileURLs = true
            databaseEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            blockNetworkImage = !Hawk.get(HawkConfig.DEBUG_OPEN, false)
            useWideViewPort = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(false)
            loadWithOverviewMode = true
            builtInZoomControls = true
            setSupportZoom(false)
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            defaultTextEncodingName = "utf-8"
            cacheMode = WebSettings.LOAD_DEFAULT
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean = false
            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean = true
            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean = true
            override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult?): Boolean = true
        }
        webView.webViewClient = SysWebClient()
        webView.setBackgroundColor(Color.BLACK)
    }

    private inner class SysWebClient : WebViewClient() {
        override fun onReceivedSslError(webView: WebView?, handler: SslErrorHandler?, error: SslError?) { handler?.proceed() }
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean = false
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            LOG.i("echo-onPageFinished url:$url")
            if (url != "about:blank" && sourceBean != null) mController.evaluateScript(sourceBean!!, url!!, view, null)
            mHandler.sendEmptyMessage(200)
        }

        fun checkIsVideo(url: String, headers: HashMap<String, String>): WebResourceResponse? {
            if (url.endsWith("/favicon.ico")) return if (url.startsWith("http://127.0.0.1")) WebResourceResponse("image/x-icon", "UTF-8", null) else null
            if (VideoParseRuler.isFilter(webUrl, url)) { LOG.i("shouldInterceptLoadRequest filter:$url"); return null }
            val ad = loadedUrls.getOrPut(url) { AdBlocker.isAd(url) }
            if (!ad) {
                if (yxdm(url, headers)) return null
                if (checkVideoFormat(url)) {
                    loadFoundVideoUrls.add(url)
                    val hdrs = HashMap(headers)
                    loadFoundVideoUrlsHeader[url] = hdrs
                    LOG.i("loadFoundVideoUrl:$url")
                    if (loadFoundCount.incrementAndGet() == 1) {
                        val vUrl = loadFoundVideoUrls.poll() ?: return null
                        mHandler.removeMessages(100)
                        CookieManager.getInstance().getCookie(vUrl)?.let { if (it.isNotEmpty()) hdrs["Cookie"] = " $it" }
                        playUrl(vUrl, hdrs)
                        SuperParse.stopJsonJx()
                        stopLoadWebView(false)
                    }
                }
            }
            return if (ad || loadFoundCount.get() > 0) AdBlocker.createEmptyResource() else null
        }

        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
            val url = request?.url.toString()
            val webHeaders = HashMap<String, String>()
            request?.requestHeaders?.forEach { (k, v) ->
                if (k.equals("user-agent", true) || k.equals("referer", true) || k.equals("origin", true)) webHeaders[k] = " $v"
            }
            return checkIsVideo(url, webHeaders)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configWebViewX5(webView: XWalkView) {
        val layoutParams = if (Hawk.get(HawkConfig.DEBUG_OPEN, false)) ViewGroup.LayoutParams(800, 400) else ViewGroup.LayoutParams(1, 1)
        webView.isFocusable = false
        webView.isFocusableInTouchMode = false
        webView.clearFocus()
        webView.overScrollMode = View.OVER_SCROLL_ALWAYS
        addContentView(webView, layoutParams)
        webView.settings.apply {
            javaScriptEnabled = true
            allowContentAccess = true
            allowFileAccess = true
            allowUniversalAccessFromFileURLs = true
            allowFileAccessFromFileURLs = true
            databaseEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            blockNetworkImage = !Hawk.get(HawkConfig.DEBUG_OPEN, false)
            useWideViewPort = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(false)
            loadWithOverviewMode = true
            builtInZoomControls = true
            setSupportZoom(false)
            cacheMode = XWalkSettings.LOAD_DEFAULT
        }
        webView.setBackgroundColor(Color.BLACK)
        webView.setUIClient(object : XWalkUIClient(webView) {
            override fun onConsoleMessage(view: XWalkView?, message: String?, lineNumber: Int, sourceId: String?, messageType: ConsoleMessageType?): Boolean = false
            override fun onJsAlert(view: XWalkView?, url: String?, message: String?, result: XWalkJavascriptResult?): Boolean = true
            override fun onJsConfirm(view: XWalkView?, url: String?, message: String?, result: XWalkJavascriptResult?): Boolean = true
            override fun onJsPrompt(view: XWalkView?, url: String?, message: String?, defaultValue: String?, result: XWalkJavascriptResult?): Boolean = true
        })
        webView.setResourceClient(XWalkWebClient(webView))
    }

    private inner class XWalkWebClient(view: XWalkView) : XWalkResourceClient(view) {
        override fun onLoadFinished(view: XWalkView?, url: String?) {
            super.onLoadFinished(view, url)
            LOG.i("echo-onPageFinished url:$url")
            if (url != "about:blank" && sourceBean != null) mController.evaluateScript(sourceBean!!, url!!, null, view)
        }

        override fun shouldInterceptLoadRequest(view: XWalkView?, request: XWalkWebResourceRequest?): XWalkWebResourceResponse? {
            val url = request?.url.toString()
            if (url.endsWith("/favicon.ico")) return if (url.startsWith("http://127.0.0.1")) createXWalkWebResourceResponse("image/x-icon", "UTF-8", null) else null
            if (VideoParseRuler.isFilter(webUrl, url)) return null
            val ad = loadedUrls.getOrPut(url) { AdBlocker.isAd(url) }
            if (!ad && checkVideoFormat(url)) {
                val webHeaders = HashMap<String, String>()
                request?.requestHeaders?.forEach { (k, v) ->
                    if (k.equals("user-agent", true) || k.equals("referer", true) || k.equals("origin", true)) webHeaders[k] = " $v"
                }
                loadFoundVideoUrls.add(url)
                val hdrs = HashMap(webHeaders)
                loadFoundVideoUrlsHeader[url] = hdrs
                if (loadFoundCount.incrementAndGet() == 1) {
                    mHandler.removeMessages(100)
                    val vUrl = loadFoundVideoUrls.poll() ?: return null
                    CookieManager.getInstance().getCookie(vUrl)?.let { if (it.isNotEmpty()) hdrs["Cookie"] = " $it" }
                    playUrl(vUrl, hdrs)
                    SuperParse.stopJsonJx()
                    stopLoadWebView(false)
                }
            }
            return if (ad || loadFoundCount.get() > 0) createXWalkWebResourceResponse("text/plain", "utf-8", ByteArrayInputStream("".toByteArray())) else null
        }

        override fun onReceivedSslError(view: XWalkView?, callback: ValueCallback<Boolean>?, error: SslError?) { callback?.onReceiveValue(true) }
    }
}
