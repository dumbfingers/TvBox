package com.github.tvbox.osc.player.controller

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.TargetApi
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Message
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.webkit.WebView
import android.widget.*
import androidx.recyclerview.widget.DiffUtil
import androidx.transition.TransitionManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.github.tvbox.osc.R
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.base.BaseActivity
import com.github.tvbox.osc.bean.IJKCode
import com.github.tvbox.osc.bean.ParseBean
import com.github.tvbox.osc.bean.SourceBean
import com.github.tvbox.osc.event.RefreshEvent
import com.github.tvbox.osc.player.thirdparty.Kodi
import com.github.tvbox.osc.player.thirdparty.MXPlayer
import com.github.tvbox.osc.player.thirdparty.ReexPlayer
import com.github.tvbox.osc.subtitle.widget.SimpleSubtitleView
import com.github.tvbox.osc.ui.activity.DetailActivity
import com.github.tvbox.osc.ui.activity.HomeActivity
import com.github.tvbox.osc.ui.adapter.ParseAdapter
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter
import com.github.tvbox.osc.ui.dialog.SelectDialog
import com.github.tvbox.osc.util.*
import com.orhanobut.hawk.Hawk
import com.owen.tvrecyclerview.widget.TvRecyclerView
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager
import org.greenrobot.eventbus.EventBus
import org.json.JSONException
import org.json.JSONObject
import org.xwalk.core.XWalkView
import xyz.doikki.videoplayer.player.VideoView
import xyz.doikki.videoplayer.util.PlayerUtils
import xyz.doikki.videoplayer.util.PlayerUtils.stringForTimeVod
import java.text.SimpleDateFormat
import java.util.*

class VodController(context: Context) : BaseController(context) {
    // top container
    private lateinit var mTopHide: LinearLayout
    private lateinit var mTopRoot: LinearLayout
    private lateinit var mPlayTitle: TextView
    private lateinit var mPlayerResolution: TextView
    private lateinit var mSpeedHidell: LinearLayout
    private lateinit var mSpeedll: LinearLayout

    // pause container
    lateinit var mProgressTop: FrameLayout
    private lateinit var mPauseIcon: ImageView
    private lateinit var mTapSeek: LinearLayout

    // progress container
    private lateinit var mProgressRoot: LinearLayout
    private lateinit var mProgressIcon: ImageView
    private lateinit var mProgressText: TextView
    private lateinit var mDialogVideoProgressBar: ProgressBar
    private lateinit var mDialogVideoPauseBar: ProgressBar

    // center BACK button
    private lateinit var mBack: LinearLayout
    private lateinit var mDanmuSetting: LinearLayout
    private var hasDanmu = false

    // center LOCK button
    private var isLock = false
    private lateinit var mLockView: ImageView
    private val lockRunnable = LockRunnable()

    // screen_display
    private lateinit var mPlayPauseTime: TextView
    private lateinit var mPlayLoadNetSpeedRightTop: TextView
    private lateinit var mTopRoot2: LinearLayout
    private lateinit var seekTime: TextView //右上角进度时间显示
    private lateinit var mScreendisplay: LinearLayout //增加屏显开关

    // bottom container
    private lateinit var mBottomRoot: LinearLayout
    private lateinit var mTime: TextView
    private lateinit var mTimeEnd: TextView
    private lateinit var mCurrentTime: TextView
    private lateinit var mSeekBar: SeekBar
    private lateinit var mTotalTime: TextView
    private var mIsDragging = false

    // 1. media control
    private lateinit var mPreBtn: LinearLayout
    private lateinit var mPauseBtn: LinearLayout
    private lateinit var mPauseImg: ImageView
    private lateinit var mNextBtn: LinearLayout
    private var mSpeed = 0f
    private lateinit var mPlayerRetry: LinearLayout

    // Fast Forward Buttons
    private lateinit var mFFwdBtn: LinearLayout
    private lateinit var mFFwdImg: ImageView
    private lateinit var mFFwdTxt: TextView

    // Scale Buttons
    private lateinit var mPlayerScaleBtn: LinearLayout
    private lateinit var mPlayerScaleImg: ImageView
    private lateinit var mPlayerScaleTxt: TextView

    // Player Buttons
    private lateinit var mPlayerBtn: LinearLayout
    private lateinit var mPlayerImg: ImageView
    private lateinit var mPlayerTxt: TextView
    private lateinit var mPlayerIJKBtn: TextView
    private lateinit var mSubtitleBtn: LinearLayout
    lateinit var mSubtitleView: SimpleSubtitleView
    private lateinit var mAudioTrackBtn: LinearLayout
    private lateinit var mPlayerTimeStartBtn: TextView
    private lateinit var mPlayerTimeSkipBtn: TextView
    private lateinit var mPlayerTimeStepBtn: TextView
    lateinit var mPlayerTimeResetBtn: TextView
    lateinit var mLvPortraitBtn: ImageView
    lateinit var mLandscapePortraitBtn: LinearLayout

    // parse container
    private lateinit var mParseRoot: LinearLayout
    private lateinit var mGridView: TvRecyclerView

    private var mPlayerConfig: JSONObject? = null
    private var mxPlayerExist = false
    private var reexPlayerExist = false
    private var KodiExist = false
    private var listener: VodControlListener? = null
    private var skipEnd = true
    private var isPaused = false
    private var isKeyUp = false

    private val mTimeRunnable = object : Runnable {
        override fun run() {
            val date = Date()
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
            mPlayPauseTime.text = timeFormat.format(date)
            mTime.text = timeFormat.format(date)
            mHandler.postDelayed(this, 1000)
        }
    }

    private val mUpdateLayout = Runnable { mBottomRoot.requestLayout() }

    init {
        mHandlerCallback = HandlerCallback { msg ->
            when (msg.what) {
                1000 -> { // seek 刷新
                    mProgressRoot.visibility = VISIBLE
                    if (isPaused) {
                        mProgressTop.visibility = GONE
                    }
                }
                1001 -> { // seek 关闭
                    mProgressRoot.visibility = GONE
                    if (isPaused) {
                        mProgressTop.visibility = VISIBLE
                    }
                }
                1002 -> { // 显示底部菜单
                    mTopRoot.visibility = VISIBLE
                    mTopRoot.alpha = 0.0f
                    mTopRoot.translationY = (-mTopRoot.height / 2).toFloat()
                    mTopRoot.animate()
                        .translationY(0f)
                        .alpha(1.0f)
                        .setDuration(250)
                        .setInterpolator(DecelerateInterpolator())
                        .setListener(null)
                    mBottomRoot.visibility = VISIBLE
                    mBottomRoot.alpha = 0.0f
                    mBottomRoot.translationY = (mBottomRoot.height / 2).toFloat()
                    mBottomRoot.animate()
                        .translationY(0f)
                        .alpha(1.0f)
                        .setDuration(250)
                        .setInterpolator(DecelerateInterpolator())
                        .setListener(null)
                    mBottomRoot.requestFocus()
                    mHandler.postDelayed(mUpdateLayout, 255)
                    if ((mActivity as? BaseActivity)?.supportsTouch() == true) {
                        mBack.visibility = VISIBLE
                    }
                    updateDanmuBtn()
                    showLockView()
                    if (isKeyUp) {
                        mPlayerTimeStartBtn.requestFocus()
                        isKeyUp = false
                    } else {
                        mPauseBtn.requestFocus()
                    }
                }
                1003 -> { // 隐藏底部菜单
                    mTopRoot.animate()
                        .translationY((-mTopRoot.height / 2).toFloat())
                        .alpha(0.0f)
                        .setDuration(250)
                        .setInterpolator(DecelerateInterpolator())
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                super.onAnimationEnd(animation)
                                mTopRoot.visibility = GONE
                                mTopRoot.clearAnimation()
                            }
                        })
                    mBottomRoot.animate()
                        .translationY((mBottomRoot.height / 2).toFloat())
                        .alpha(0.0f)
                        .setDuration(250)
                        .setInterpolator(DecelerateInterpolator())
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                super.onAnimationEnd(animation)
                                mBottomRoot.visibility = GONE
                                mBottomRoot.clearAnimation()
                            }
                        })
                    mDanmuSetting.visibility = GONE
                    mBack.visibility = GONE
                    mLockView.visibility = GONE
                }
                1004 -> { // 设置速度
                    if (isInPlaybackState) {
                        try {
                            val speed = mPlayerConfig!!.optDouble("sp", 1.0).toFloat()
                            mControlWrapper.speed = speed
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    } else mHandler.sendEmptyMessageDelayed(1004, 100)
                }
            }
        }
    }

    private fun showLockView() {
        mLockView.visibility = if (ScreenUtils.isTv(context)) INVISIBLE else VISIBLE
        mHandler.removeCallbacks(lockRunnable)
        mHandler.postDelayed(lockRunnable, 3000)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mHandler.removeCallbacks(mTimeRunnable)
    }

    override fun initView() {
        super.initView()
        mTopHide = findViewById(R.id.top_container_hide)
        mTopRoot = findViewById(R.id.top_container)
        mPlayTitle = findViewById(R.id.tv_title_top)
        mPlayerResolution = findViewById(R.id.tv_resolution)
        mSpeedHidell = findViewById(R.id.tv_speed_top_hide)
        mSpeedll = findViewById(R.id.tv_speed_top)
        mProgressTop = findViewById(R.id.tv_pause_container)
        mPauseIcon = findViewById(R.id.tv_pause_icon)
        mTapSeek = findViewById(R.id.ll_ddtap)
        mProgressRoot = findViewById(R.id.tv_progress_container)
        mProgressIcon = findViewById(R.id.tv_progress_icon)
        mProgressText = findViewById(R.id.tv_progress_text)
        mDialogVideoProgressBar = findViewWithTag("progressbar_video")
        mDialogVideoPauseBar = findViewWithTag("pausebar_video")
        mBack = findViewById(R.id.tvBackButton)
        mDanmuSetting = findViewById(R.id.ll_danmu_setting)
        mLockView = findViewById(R.id.tv_lock)
        mBottomRoot = findViewById(R.id.bottom_container)
        mTime = findViewById(R.id.tv_sys_time)
        mTimeEnd = findViewById(R.id.tv_time_end)
        mCurrentTime = findViewById(R.id.curr_time)
        mSeekBar = findViewById(R.id.seekBar)
        mTotalTime = findViewById(R.id.total_time)
        mPreBtn = findViewById(R.id.play_prev)
        mPauseBtn = findViewById(R.id.play_pause)
        mPauseImg = findViewById(R.id.play_pauseImg)
        mNextBtn = findViewById(R.id.play_next)
        mPlayerRetry = findViewById(R.id.play_retry)
        mFFwdBtn = findViewById(R.id.play_speed)
        mFFwdImg = findViewById(R.id.play_speed_img)
        mFFwdTxt = findViewById(R.id.play_speed_txt)
        mPlayerScaleBtn = findViewById(R.id.play_scale)
        mPlayerScaleImg = findViewById(R.id.play_scale_img)
        mPlayerScaleTxt = findViewById(R.id.play_scale_txt)
        mPlayerBtn = findViewById(R.id.play_player)
        mPlayerImg = findViewById(R.id.play_player_img)
        mPlayerTxt = findViewById(R.id.play_player_txt)
        mPlayerIJKBtn = findViewById(R.id.play_ijk)
        mSubtitleBtn = findViewById(R.id.play_subtitle)
        mSubtitleView = findViewById(R.id.subtitle_view)
        mAudioTrackBtn = findViewById(R.id.play_audio)
        mPlayerTimeStartBtn = findViewById(R.id.play_time_start)
        mPlayerTimeSkipBtn = findViewById(R.id.play_time_end)
        mPlayerTimeStepBtn = findViewById(R.id.play_time_step)
        mPlayerTimeResetBtn = findViewById(R.id.play_time_reset)
        mLandscapePortraitBtn = findViewById(R.id.landscape_portrait)
        mLvPortraitBtn = findViewById(R.id.lv_portrait)
        mParseRoot = findViewById(R.id.parse_root)
        mGridView = findViewById(R.id.mGridView)
        mTopRoot.visibility = INVISIBLE
        mBottomRoot.visibility = INVISIBLE
        mBack.visibility = INVISIBLE
        mDanmuSetting.visibility = INVISIBLE
        initSubtitleInfo()
        mPlayPauseTime = findViewById(R.id.tv_system_time)
        mPlayLoadNetSpeedRightTop = findViewById(R.id.tv_play_load_net_speed_right_top)
        mTopRoot2 = findViewById(R.id.tv_top_r_container)
        seekTime = findViewById(R.id.tv_seek_time)
        mScreendisplay = findViewById(R.id.screen_display)
        mLockView.setOnClickListener {
            isLock = !isLock
            mLockView.setImageResource(if (isLock) R.drawable.icon_lock else R.drawable.icon_unlock)
            if (isLock) {
                mHandler.sendEmptyMessage(1003)
            }
            showLockView()
        }
        val rootView = findViewById<View>(R.id.rootView)
        rootView.setOnTouchListener { _, event ->
            if (isLock) {
                if (event.action == MotionEvent.ACTION_UP) {
                    if (mLockView.visibility == VISIBLE) {
                        mLockView.visibility = GONE
                    } else {
                        showLockView()
                    }
                }
            }
            isLock
        }
        mPlayPauseTime.post { mHandler.post(mTimeRunnable) }
        mGridView.layoutManager = V7LinearLayoutManager(context, 0, false)
        val parseAdapter = ParseAdapter()
        parseAdapter.onItemClickListener = BaseQuickAdapter.OnItemClickListener { _, _, position ->
            val parseBean = parseAdapter.getItem(position)
            val currentDefault = parseAdapter.data.indexOf(ApiConfig.get().defaultParse)
            parseAdapter.notifyItemChanged(currentDefault)
            ApiConfig.get().defaultParse = parseBean
            parseAdapter.notifyItemChanged(position)
            listener?.changeParse(parseBean)
            hideBottom()
        }
        mGridView.adapter = parseAdapter
        parseAdapter.setNewData(ApiConfig.get().parseBeanList)
        mParseRoot.visibility = VISIBLE
        mSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val duration = mControlWrapper.duration
                val newPosition = duration * progress / seekBar.max
                mCurrentTime.text = stringForTimeVod(newPosition.toInt())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                mIsDragging = true
                mControlWrapper.stopProgress()
                mControlWrapper.stopFadeOut()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val duration = mControlWrapper.duration
                val newPosition = duration * seekBar.progress / seekBar.max
                mControlWrapper.seekTo(newPosition.toInt().toLong())
                mIsDragging = false
                mControlWrapper.startProgress()
                mControlWrapper.startFadeOut()
            }
        })
        mPlayTitle.setOnClickListener { view ->
            FastClickCheckUtil.check(view)
            listener?.openVideo()
        }
        mPreBtn.setOnClickListener {
            listener?.playPre()
            hideBottom()
        }
        mPauseBtn.setOnClickListener {
            togglePlay()
            if (!isPaused) hideBottom()
        }
        mNextBtn.setOnClickListener {
            listener?.playNext(false)
            hideBottom()
        }
        mFFwdBtn.setOnClickListener {
            mHandler.removeCallbacks(mHideBottomRunnable)
            mHandler.postDelayed(mHideBottomRunnable, 8000)
            try {
                val speed = mPlayerConfig!!.optDouble("sp", 1.0).toFloat()
                increasePlaySpeed(speed)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        mFFwdBtn.setOnLongClickListener {
            val currentSpeed = mControlWrapper.speed
            if (currentSpeed == 1.0f) setPlaySpeed(5.0f) else setPlaySpeed(1.0f)
            true
        }
        mPlayerRetry.setOnClickListener {
            listener?.replay(false)
            hideBottom()
        }
        mPlayerRetry.setOnLongClickListener {
            listener?.replay(true)
            hideBottom()
            true
        }
        mPlayerScaleBtn.setOnClickListener {
            mHandler.removeCallbacks(mHideBottomRunnable)
            mHandler.postDelayed(mHideBottomRunnable, 8000)
            try {
                var scaleType = mPlayerConfig!!.getInt("sc")
                scaleType++
                if (scaleType > 5) scaleType = 0
                mPlayerConfig!!.put("sc", scaleType)
                updatePlayerCfgView()
                listener?.updatePlayerCfg()
                mControlWrapper.setScreenScaleType(scaleType)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        mPlayerScaleBtn.setOnLongClickListener {
            val checkOrientation = mActivity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            if (checkOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || checkOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE || checkOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                mActivity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            } else if (checkOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT || checkOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT || checkOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
                mActivity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
            true
        }
        mPlayerBtn.setOnClickListener { view ->
            FastClickCheckUtil.check(view)
            try {
                val defaultPos = mPlayerConfig!!.getInt("pl")
                val players = ArrayList<Int>()
                players.add(0)
                players.add(1)
                players.add(2)
                players.add(3)
                if (mxPlayerExist) players.add(10)
                if (reexPlayerExist) players.add(11)
                if (KodiExist) players.add(12)
                val context = mActivity ?: return@setOnClickListener
                val dialog = SelectDialog<Int>(context)
                dialog.setTip(HomeActivity.getRes().getString(R.string.dia_player))
                dialog.setAdapter(null, object : SelectDialogAdapter.SelectDialogInterface<Int> {
                    override fun click(value: Int, pos: Int) {
                        try {
                            dialog.cancel()
                            val thisPlayType = players[pos]
                            mPlayerConfig!!.put("pl", thisPlayType)
                            updatePlayerCfgView()
                            listener?.updatePlayerCfg()
                            listener?.replay(false)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    override fun getDisplay(val_: Int): String = PlayerHelper.getPlayerName(val_)
                }, object : DiffUtil.ItemCallback<Int>() {
                    override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean = oldItem == newItem
                    override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean = oldItem == newItem
                }, players, defaultPos)
                dialog.show()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        mPlayerIJKBtn.setOnClickListener {
            try {
                var ijk = mPlayerConfig!!.getString("ijk")
                val codecs = ApiConfig.get().ijkCodes
                for (i in codecs.indices) {
                    if (ijk == codecs[i].name) {
                        ijk = if (i >= codecs.size - 1) codecs[0].name else codecs[i + 1].name
                        break
                    }
                }
                mPlayerConfig!!.put("ijk", ijk)
                updatePlayerCfgView()
                listener?.updatePlayerCfg()
                listener?.replay(false)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            mPlayerIJKBtn.requestFocus()
        }
        mSubtitleBtn.setOnClickListener { view ->
            FastClickCheckUtil.check(view)
            listener?.selectSubtitle()
        }
        mSubtitleBtn.setOnLongClickListener {
            if (mSubtitleView.visibility == GONE) {
                mSubtitleView.visibility = VISIBLE
                hideBottom()
                Toast.makeText(context, HomeActivity.getRes().getString(R.string.vod_sub_on), Toast.LENGTH_SHORT).show()
            } else {
                mSubtitleView.visibility = GONE
                hideBottom()
                Toast.makeText(context, HomeActivity.getRes().getString(R.string.vod_sub_off), Toast.LENGTH_SHORT).show()
            }
            true
        }
        mAudioTrackBtn.setOnClickListener { view ->
            FastClickCheckUtil.check(view)
            listener?.selectAudioTrack()
        }
        mPlayerTimeResetBtn.setOnClickListener {
            mHandler.removeCallbacks(mHideBottomRunnable)
            mHandler.postDelayed(mHideBottomRunnable, 8000)
            try {
                mPlayerConfig!!.put("et", 0)
                mPlayerConfig!!.put("st", 0)
                updatePlayerCfgView()
                listener?.updatePlayerCfg()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        mPlayerTimeResetBtn.setOnLongClickListener {
            try {
                mPlayerConfig!!.put("st", 110)
                mPlayerConfig!!.put("et", 150)
                updatePlayerCfgView()
                listener?.updatePlayerCfg()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            Toast.makeText(context, "已预设片头片尾", Toast.LENGTH_SHORT).show()
            true
        }
        mPlayerTimeStartBtn.setOnClickListener {
            mHandler.removeCallbacks(mHideBottomRunnable)
            mHandler.postDelayed(mHideBottomRunnable, 8000)
            try {
                val current = mControlWrapper.currentPosition.toInt()
                val duration = mControlWrapper.duration.toInt()
                if (current > duration / 2) return@setOnClickListener
                mPlayerConfig!!.put("st", current / 1000)
                updatePlayerCfgView()
                listener?.updatePlayerCfg()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        mPlayerTimeStartBtn.setOnLongClickListener {
            try {
                mPlayerConfig!!.put("st", 0)
                updatePlayerCfgView()
                listener?.updatePlayerCfg()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            true
        }
        mPlayerTimeSkipBtn.setOnClickListener {
            mHandler.removeCallbacks(mHideBottomRunnable)
            mHandler.postDelayed(mHideBottomRunnable, 8000)
            try {
                val current = mControlWrapper.currentPosition.toInt()
                val duration = mControlWrapper.duration.toInt()
                if (current < duration / 2) return@setOnClickListener
                mPlayerConfig!!.put("et", (duration - current) / 1000)
                updatePlayerCfgView()
                listener?.updatePlayerCfg()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        mPlayerTimeSkipBtn.setOnLongClickListener {
            try {
                mPlayerConfig!!.put("et", 0)
                updatePlayerCfgView()
                listener?.updatePlayerCfg()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            true
        }
        mPlayerTimeStepBtn.setOnClickListener {
            var step = Hawk.get(HawkConfig.PLAY_TIME_STEP, 5)
            step += 5
            if (step > 30) step = 5
            Hawk.put(HawkConfig.PLAY_TIME_STEP, step)
            updatePlayerCfgView()
        }
        mPlayerTimeStepBtn.setOnLongClickListener {
            Hawk.put(HawkConfig.PLAY_TIME_STEP, 5)
            updatePlayerCfgView()
            true
        }
        mLandscapePortraitBtn.setOnClickListener { view ->
            FastClickCheckUtil.check(view)
            setLandscapePortrait()
            hideBottom()
        }
        mBack.setOnClickListener {
            val showPreview = Hawk.get(HawkConfig.SHOW_PREVIEW, true)
            if (showPreview) {
                mTopRoot.visibility = GONE
                mBottomRoot.visibility = GONE
                mBack.visibility = GONE
                mLockView.visibility = GONE
                mProgressTop.visibility = GONE
                mDanmuSetting.visibility = GONE
                mHandler.removeCallbacks(mHideBottomRunnable)
                if (mActivity != null) {
                    if (mActivity?.javaClass?.simpleName == "DetailActivity") {
                        (mActivity as? DetailActivity)?.toggleFullPreview()
                    } else {
                        mActivity?.finish()
                    }
                }
            } else {
                mActivity?.finish()
            }
        }
        mTopRoot2.visibility = Hawk.get(HawkConfig.SCREEN_DISPLAY, GONE)
        mScreendisplay.setOnClickListener {
            mTopRoot2.visibility = if (mTopRoot2.visibility == VISIBLE) GONE else VISIBLE
            Hawk.put(HawkConfig.SCREEN_DISPLAY, mTopRoot2.visibility)
            hideBottom()
        }
        mDanmuSetting.setOnClickListener { listener?.showDanmuSetting() }
    }

    private fun initLandscapePortraitBtnInfo() {
        if (mControlWrapper != null) {
            val width = mControlWrapper.videoSize[0]
            val height = mControlWrapper.videoSize[1]
            if (width < height) {
                mLandscapePortraitBtn.visibility = VISIBLE
                mLvPortraitBtn.setImageResource(R.drawable.htov)
            }
        }
    }

    private fun setLandscapePortrait() {
        val requestedOrientation = mActivity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE || requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
            mLvPortraitBtn.setImageResource(R.drawable.vtoh)
            mActivity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        } else if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT || requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT || requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
            mLvPortraitBtn.setImageResource(R.drawable.htov)
            mActivity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    private fun initSubtitleInfo() {
        val subtitleTextSize = SubtitleHelper.getTextSize(mActivity)
        mSubtitleView.setTextSize(subtitleTextSize.toFloat())
        SubtitleHelper.upTextStyle(mSubtitleView)
    }

    override fun getLayoutId(): Int = R.layout.player_vod_control_view

    fun showParse(userJxList: Boolean) {
        mParseRoot.visibility = if (userJxList) VISIBLE else GONE
    }

    fun setPlayerConfig(playerCfg: JSONObject?) {
        mPlayerConfig = playerCfg
        updatePlayerCfgView()
        mxPlayerExist = PlayerHelper.getPlayerName(10) != ""
        reexPlayerExist = PlayerHelper.getPlayerName(11) != ""
        KodiExist = PlayerHelper.getPlayerName(12) != ""
    }

    private fun updatePlayerCfgView() {
        try {
            val playerType = mPlayerConfig!!.getInt("pl")
            mPlayerTxt.text = PlayerHelper.getPlayerName(playerType)
            mPlayerScaleTxt.text = PlayerHelper.getScaleName(mPlayerConfig!!.getInt("sc"))
            mPlayerIJKBtn.text = mPlayerConfig!!.getString("ijk")
            mPlayerIJKBtn.visibility = if (playerType == 1) VISIBLE else GONE
            mFFwdTxt.text = "x" + mPlayerConfig!!.getDouble("sp")
            mPlayerTimeStartBtn.text = PlayerUtils.stringForTime(mPlayerConfig!!.getInt("st") * 1000)
            mPlayerTimeSkipBtn.text = PlayerUtils.stringForTime(mPlayerConfig!!.getInt("et") * 1000)
            mPlayerTimeStepBtn.text = "${Hawk.get(HawkConfig.PLAY_TIME_STEP, 5)}s"
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun setTitle(playTitleInfo: String?) {
        mPlayTitle.text = playTitleInfo
    }

    fun resetSpeed() {
        skipEnd = true
        mHandler.removeMessages(1004)
        mHandler.sendEmptyMessageDelayed(1004, 100)
    }

    interface VodControlListener {
        fun playNext(rmProgress: Boolean)
        fun playPre()
        fun prepared()
        fun changeParse(pb: ParseBean?)
        fun updatePlayerCfg()
        fun replay(replay: Boolean)
        fun errReplay()
        fun selectSubtitle()
        fun selectAudioTrack()
        fun openVideo()
        fun showDanmuSetting()
    }

    fun setListener(listener: VodControlListener?) {
        this.listener = listener
    }

    override fun setProgress(duration: Int, position: Int) {
        if (mIsDragging) return
        super.setProgress(duration, position)
        if (skipEnd && position != 0 && duration != 0) {
            var et = 0
            try {
                et = mPlayerConfig!!.getInt("et")
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            if (et > 0 && position + et * 1000 >= duration) {
                skipEnd = false
                listener?.playNext(true)
            }
        }
        val timeRemaining = mControlWrapper.duration - mControlWrapper.currentPosition
        val date = Calendar.getInstance()
        val t = date.timeInMillis
        val afterAdd = Date(t + timeRemaining)
        val timeEnd = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
        if (isPaused) {
            mTimeEnd.text = "${context.getString(R.string.vod_remaining_time)} ${PlayerUtils.stringForTime(timeRemaining.toInt())} | ${context.getString(R.string.vod_ends_at)} ${timeEnd.format(afterAdd)}"
        } else {
            mTimeEnd.text = "${context.getString(R.string.vod_ends_at)} ${timeEnd.format(afterAdd)}"
        }
        mCurrentTime.text = stringForTimeVod(position)
        mTotalTime.text = stringForTimeVod(duration)
        seekTime.text = "${PlayerUtils.stringForTime(position)} | ${PlayerUtils.stringForTime(duration)}"
        if (duration > 0) {
            mSeekBar.isEnabled = true
            val pos = (position * 1.0 / duration * mSeekBar.max).toInt()
            mSeekBar.progress = pos
        } else {
            mSeekBar.isEnabled = false
        }
        val percent = mControlWrapper.bufferedPercentage
        if (percent >= 95) {
            mSeekBar.secondaryProgress = mSeekBar.max
        } else {
            mSeekBar.secondaryProgress = percent * 10
        }
    }

    private var simSlideStart = false
    private var simSeekPosition = 0
    private var simSlideOffset: Long = 0
    private var tapDirection = 0
    private var lastSlideTime: Long = 0

    private fun tvSlideStop() {
        if (!simSlideStart) return
        mControlWrapper.seekTo(simSeekPosition.toLong())
        if (!mControlWrapper.isPlaying) mControlWrapper.start()
        simSlideStart = false
        simSeekPosition = 0
        simSlideOffset = 0
    }

    private fun tvSlideStart(dir: Int) {
        val duration = mControlWrapper.duration.toInt()
        if (duration <= 0) return
        val currentTime = System.currentTimeMillis()
        val baseSkip = 10000
        val accelerationFactor = 1.5f
        val threshold: Long = 500
        if (!simSlideStart) {
            simSlideStart = true
            simSlideOffset = (baseSkip * dir).toLong()
        } else {
            if (currentTime - lastSlideTime <= threshold) {
                simSlideOffset += (baseSkip * accelerationFactor * dir).toLong()
            } else {
                simSlideOffset = (baseSkip * dir).toLong()
            }
        }
        lastSlideTime = currentTime
        val currentPosition = mControlWrapper.currentPosition.toInt()
        var position = (currentPosition + simSlideOffset).toInt()
        if (position > duration) position = duration
        if (position < 0) position = 0
        updateSeekUI(currentPosition, position, duration)
        simSeekPosition = position
    }

    override fun updateSeekUI(curr: Int, seekTo: Int, duration: Int) {
        super.updateSeekUI(curr, seekTo, duration)
        if (seekTo > curr) {
            mProgressIcon.setImageResource(R.drawable.play_ffwd)
        } else {
            mProgressIcon.setImageResource(R.drawable.play_rewind)
        }
        mProgressText.text = "${PlayerUtils.stringForTime(seekTo)} / ${PlayerUtils.stringForTime(duration)}"
        val percent = (seekTo.toDouble() / duration * 100).toInt()
        mDialogVideoPauseBar.progress = percent
        mDialogVideoProgressBar.progress = percent
        mHandler.sendEmptyMessage(1000)
        mHandler.removeMessages(1001)
        mHandler.sendEmptyMessageDelayed(1001, 1000)
    }

    override fun onPlayStateChanged(playState: Int) {
        super.onPlayStateChanged(playState)
        EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_REFRESH_NOTIFY, null))
        when (playState) {
            VideoView.STATE_IDLE -> {}
            VideoView.STATE_PLAYING -> {
                isPaused = false
                mPauseImg.setImageDrawable(resources.getDrawable(R.drawable.v_pause))
                startProgress()
            }
            VideoView.STATE_PAUSED -> {
                isPaused = true
                mPauseImg.setImageDrawable(resources.getDrawable(R.drawable.v_play))
            }
            VideoView.STATE_ERROR -> listener?.errReplay()
            VideoView.STATE_PREPARED -> {
                listener?.prepared()
                if (mControlWrapper.videoSize.size >= 2) {
                    mPlayerResolution.text = "${mControlWrapper.videoSize[0]} x ${mControlWrapper.videoSize[1]}"
                    initLandscapePortraitBtnInfo()
                }
            }
            VideoView.STATE_BUFFERED -> {}
            VideoView.STATE_PREPARING, VideoView.STATE_BUFFERING -> {}
            VideoView.STATE_PLAYBACK_COMPLETED -> listener?.playNext(true)
        }
    }

    private fun isBottomVisible(): Boolean = mBottomRoot.visibility == VISIBLE

    private fun showBottom() {
        mHandler.removeMessages(1003)
        mHandler.sendEmptyMessage(1002)
        mHandler.post(mTimeRunnable)
        mHandler.postDelayed(mHideBottomRunnable, 8000)
    }

    private val mHideBottomRunnable = Runnable { hideBottom() }

    fun hideBottom() {
        mHandler.removeMessages(1002)
        mHandler.sendEmptyMessage(1003)
        mHandler.removeCallbacks(mHideBottomRunnable)
    }

    private fun increasePlaySpeed(speed: Float) {
        var s = speed
        if (s == 5f) {
            s = 0.25f
        } else if (s >= 2 && s < 3) {
            s += 0.5f
        } else if (s >= 3) {
            s += 1.0f
        } else {
            s += 0.25f
        }
        setPlaySpeed(s)
    }

    private fun decreasePlaySpeed(speed: Float) {
        var s = speed
        if (s == 0.25f) {
            s = 5.0f
        } else if (s > 3) {
            s -= 1.0f
        } else if (s > 2 && s <= 3) {
            s -= 0.5f
        } else {
            s -= 0.25f
        }
        setPlaySpeed(s)
    }

    private fun setPlaySpeed(value: Float) {
        try {
            mPlayerConfig!!.put("sp", value.toDouble())
            updatePlayerCfgView()
            listener?.updatePlayerCfg()
            mControlWrapper.speed = value
        } catch (err: JSONException) {
            err.printStackTrace()
        }
    }

    private fun increaseTime(type: String) {
        try {
            val step = Hawk.get(HawkConfig.PLAY_TIME_STEP, 5)
            var time = mPlayerConfig!!.getInt(type)
            time += step
            if (time > 30 * 10) time = 0
            mPlayerConfig!!.put(type, time)
            updatePlayerCfgView()
            listener?.updatePlayerCfg()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun decreaseTime(type: String) {
        try {
            val step = Hawk.get(HawkConfig.PLAY_TIME_STEP, 5)
            var time = mPlayerConfig!!.getInt(type)
            time -= step
            if (time < 0) time = 30 * 10
            mPlayerConfig!!.put(type, time)
            updatePlayerCfgView()
            listener?.updatePlayerCfg()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val action = event.action
        val isInPlayback = isInPlaybackState
        if (super.onKeyEvent(event)) {
            return true
        }
        if (isBottomVisible()) {
            mHandler.removeCallbacks(mHideBottomRunnable)
            mHandler.postDelayed(mHideBottomRunnable, 8000)
            return super.dispatchKeyEvent(event)
        }
        if (action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_LEFT -> if (isInPlayback) {
                    tvSlideStart(if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) 1 else -1)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> if (isInPlayback) {
                    togglePlay()
                    if (!isBottomVisible() && isPaused) {
                        showBottom()
                    }
                    return true
                }
                KeyEvent.KEYCODE_DPAD_UP -> if (!isBottomVisible()) {
                    showBottom()
                    isKeyUp = true
                    return true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> if (!isBottomVisible()) {
                    showBottom()
                    return true
                }
            }
        } else if (action == KeyEvent.ACTION_UP) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (isInPlayback) {
                    tvSlideStop()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (isBottomVisible() && mFFwdBtn.isFocused) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    try {
                        val speed = mPlayerConfig!!.optDouble("sp", 1.0).toFloat()
                        increasePlaySpeed(speed)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    try {
                        val speed = mPlayerConfig!!.optDouble("sp", 1.0).toFloat()
                        decreasePlaySpeed(speed)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        } else if (isBottomVisible() && mPlayerTimeStartBtn.isFocused) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    increaseTime("st")
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    decreaseTime("st")
                }
            }
        } else if (isBottomVisible() && mPlayerTimeSkipBtn.isFocused) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    increaseTime("et")
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    decreaseTime("et")
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        if (!isBottomVisible()) showBottom() else hideBottom()
        return true
    }

    private var fromLongPress = false
    private var currentSpeed = 0f

    override fun onLongPress(e: MotionEvent) {
        if (!isPaused) {
            fromLongPress = true
            try {
                currentSpeed = mPlayerConfig!!.optDouble("sp", 1.0).toFloat()
                circularReveal(mTapSeek, 1)
                mProgressTop.visibility = VISIBLE
                mPauseIcon.setImageResource(R.drawable.play_ffwd)
                mSpeed = if (currentSpeed < 3.0f) 3.0f else currentSpeed
                setPlaySpeed(mSpeed)
            } catch (f: JSONException) {
                f.printStackTrace()
            }
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_UP) {
            if (fromLongPress) {
                mProgressTop.visibility = GONE
                mPauseIcon.setImageResource(R.drawable.play_pause)
                mSpeed = currentSpeed
                setPlaySpeed(mSpeed)
                fromLongPress = false
            }
        }
        return super.onTouchEvent(e)
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        val threeScreen = PlayerUtils.getScreenWidth(context, true) / 3
        if (e.x > 0 && e.x < threeScreen) {
            tapDirection = -1
        } else if (e.x > threeScreen && e.x < threeScreen * 2) {
            tapDirection = 0
        } else if (e.x > threeScreen * 2) {
            tapDirection = 1
        }
        if (tapDirection == 0 || isPaused) {
            togglePlay()
        } else {
            circularReveal(mTapSeek, tapDirection)
            val duration = mControlWrapper.duration.toInt()
            val currentPosition = mControlWrapper.currentPosition.toInt()
            var position = (10000.0f * tapDirection).toInt() + currentPosition
            if (position > duration) position = duration
            if (position < 0) position = 0
            updateSeekUI(currentPosition, position, duration)
            mControlWrapper.seekTo(position.toLong())
        }
        return true
    }

    private class LockRunnable : Runnable {
        override fun run() {}
    }

    override fun onBackPressed(): Boolean {
        if (super.onBackPressed()) return true
        if (isBottomVisible()) {
            hideBottom()
            return true
        }
        val checkOrientation = mActivity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        if (checkOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT || checkOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT || checkOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
            mActivity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        return false
    }

    fun updateDanmuBtn() {
        mDanmuSetting.visibility = if (hasDanmu) VISIBLE else GONE
    }

    fun setHasDanmu(hasDanmu: Boolean) {
        this.hasDanmu = hasDanmu
    }

    fun evaluateScript(sourceBean: SourceBean, url: String, web_view: WebView?, xWalk_view: XWalkView?) {
        var clickSelector = sourceBean.clickSelector?.trim { it <= ' ' } ?: ""
        clickSelector = if (clickSelector.isEmpty()) VideoParseRuler.getHostScript(url) else clickSelector
        if (!clickSelector.isEmpty()) {
            val selector: String
            if (clickSelector.contains(";") && !clickSelector.endsWith(";")) {
                val parts = clickSelector.split(";", limit = 2).toTypedArray()
                if (!url.contains(parts[0])) return
                selector = parts[1].trim { it <= ' ' }
            } else {
                selector = clickSelector.trim { it <= ' ' }
            }
            val js = selector
            if (web_view != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    web_view.evaluateJavascript(js, null)
                } else {
                    web_view.loadUrl("javascript:$js")
                }
            }
            xWalk_view?.evaluateJavascript(js, null)
        }
    }

    companion object {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        fun circularReveal(v: View, direction: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val radius = Math.max(v.width, v.height) / 2
                var width = 0
                if (direction == 1) {
                    width = v.width
                }
                TransitionManager.beginDelayedTransition(v as ViewGroup)
                val anim = ViewAnimationUtils.createCircularReveal(v, width, v.height / 2, 0f, radius.toFloat())
                anim.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {
                        v.visibility = VISIBLE
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        v.visibility = GONE
                    }

                    override fun onAnimationCancel(animation: Animator) {}
                    override fun onAnimationRepeat(animation: Animator) {}
                })
                anim.duration = 600
                anim.start()
            }
        }
    }
}
