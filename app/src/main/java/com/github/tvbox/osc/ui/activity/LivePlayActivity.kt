package com.github.tvbox.osc.ui.activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.github.tvbox.osc.R
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.base.App
import com.github.tvbox.osc.base.BaseActivity
import com.github.tvbox.osc.bean.*
import com.github.tvbox.osc.event.RefreshEvent
import com.github.tvbox.osc.player.controller.LiveController
import com.github.tvbox.osc.ui.adapter.*
import com.github.tvbox.osc.ui.dialog.ApiHistoryDialog
import com.github.tvbox.osc.ui.dialog.LivePasswordDialog
import com.github.tvbox.osc.util.*
import com.github.tvbox.osc.util.live.TxtSubscribe
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.AbsCallback
import com.lzy.okgo.callback.StringCallback
import com.lzy.okgo.model.Response
import com.orhanobut.hawk.Hawk
import com.owen.tvrecyclerview.widget.TvRecyclerView
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager
import org.apache.commons.lang3.StringUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import xyz.doikki.videoplayer.player.VideoView
import xyz.doikki.videoplayer.util.PlayerUtils
import xyz.doikki.videoplayer.util.PlayerUtils.stringForTimeVod
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * @author pj567
 * @date :2021/1/12
 * @description:
 */
class LivePlayActivity : BaseActivity() {
    // Main View
    private lateinit var mVideoView: VideoView
    private lateinit var controller: LiveController

    // Left Channel View
    private lateinit var tvLeftChannelListLayout: LinearLayout
    private lateinit var mGroupGridView: TvRecyclerView
    private lateinit var mDivLeft: LinearLayout
    private lateinit var mChannelGridView: TvRecyclerView
    private lateinit var mDivRight: LinearLayout
    private lateinit var mGroupEPG: LinearLayout
    private lateinit var mEpgDateGridView: TvRecyclerView
    private lateinit var mEpgInfoGridView: TvRecyclerView

    // Left Channel View - Variables
    private lateinit var liveChannelGroupAdapter: LiveChannelGroupAdapter
    private lateinit var liveChannelItemAdapter: LiveChannelItemAdapter
    private val liveChannelGroupList: MutableList<LiveChannelGroup> = ArrayList()
    private val liveSettingGroupList: MutableList<LiveSettingGroup> = ArrayList()

    private var currentLiveChannelIndex = -1
    private var currentLiveChannelItem: LiveChannelItem? = null

    // 遥控器数字键输入的要切换的频道号码
    private var selectedChannelNumber = 0
    private lateinit var tvSelectedChannel: TextView

    // Right Channel View
    private lateinit var tvRightSettingLayout: LinearLayout
    private lateinit var mSettingGroupView: TvRecyclerView
    private lateinit var mSettingItemView: TvRecyclerView

    // Right Channel View - Variables
    private lateinit var liveSettingGroupAdapter: LiveSettingGroupAdapter
    private lateinit var liveSettingItemAdapter: LiveSettingItemAdapter
    private val livePlayerManager = LivePlayerManager()
    private val channelGroupPasswordConfirmed = ArrayList<Int>()
    private var currentLiveChangeSourceTimes = 0

    // Bottom Channel View
    private lateinit var tvBottomLayout: LinearLayout
    private lateinit var tv_logo: ImageView
    private lateinit var tv_sys_time: TextView
    private lateinit var tv_size: TextView
    private lateinit var tv_source: TextView

    // Bottom Channel View - Line 1 / 2 / 3
    private lateinit var tv_channelname: TextView
    private lateinit var tv_channelnum: TextView
    private lateinit var tv_curr_name: TextView
    private lateinit var tv_curr_time: TextView
    private lateinit var tv_next_name: TextView
    private lateinit var tv_next_time: TextView

    // Bottom Channel View - Variables
    private lateinit var epgDateAdapter: LiveEpgDateAdapter
    private lateinit var epgListAdapter: LiveEpgAdapter

    // Misc Variables
    var epgStringAddress = ""
    var timeFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val mHandler = Handler(Looper.getMainLooper())
    private var channel_Name: LiveChannelItem? = null
    private val hsEpg = Hashtable<String, ArrayList<Epginfo>>()
    private lateinit var tvTime: TextView
    private lateinit var tvNetSpeed: TextView

    // Seek Bar
    var mIsDragging = false
    private lateinit var llSeekBar: LinearLayout
    private lateinit var mCurrentTime: TextView
    private lateinit var mSeekBar: SeekBar
    private lateinit var mTotalTime: TextView
    var isVOD = false

    // center BACK button
    private lateinit var mBack: LinearLayout
    private var isSHIYI = false
    private var shiyi_time: String? = null //时移时间

    companion object {
        @JvmField
        var currentChannelGroupIndex = 0
    }

    private fun setPlayHeaders(url: String): HashMap<String, String> {
        val header = HashMap<String, String>()
        try {
            var matchTo = false
            val livePlayHeaders = JSONArray(ApiConfig.get().livePlayHeaders.toString())
            for (i in 0 until livePlayHeaders.length()) {
                val headerObj = livePlayHeaders.getJSONObject(i)
                val flags = headerObj.getJSONArray("flag")
                val headerData = headerObj.getJSONObject("header")
                for (j in 0 until flags.length()) {
                    val flag = flags.getString(j)
                    if (url.contains(flag)) {
                        matchTo = true
                        break
                    }
                }
                if (matchTo) {
                    val keys = headerData.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val value = headerData.getString(key)
                        header[key] = value
                    }
                    break
                }
            }
            if (!matchTo) {
                header["User-Agent"] = "Lavf/59.27.100"
            }
        } catch (e: Exception) {
            header["User-Agent"] = "Lavf/59.27.100"
        }
        return header
    }

    override fun getLayoutResID(): Int {
        return R.layout.activity_live_play
    }

    override fun init() {
        // takagen99 : Hide only when video playing
        hideSystemUI(false)

        // Getting EPG Address
        epgStringAddress = Hawk.get(HawkConfig.EPG_URL, "")
        if (StringUtils.isBlank(epgStringAddress)) {
            epgStringAddress = "https://epg.112114.xyz/"
        }

        EventBus.getDefault().register(this)
        setLoadSir(findViewById(R.id.live_root))
        mVideoView = findViewById(R.id.mVideoView)
        tvSelectedChannel = findViewById(R.id.tv_selected_channel)
        tv_size = findViewById(R.id.tv_size) // Resolution
        tv_source = findViewById(R.id.tv_source) // Source/Total Source
        tv_sys_time = findViewById(R.id.tv_sys_time) // System Time

        // VOD SeekBar
        llSeekBar = findViewById(R.id.ll_seekbar)
        mCurrentTime = findViewById(R.id.curr_time)
        mSeekBar = findViewById(R.id.seekBar)
        mTotalTime = findViewById(R.id.total_time)

        // Center Back Button
        mBack = findViewById(R.id.tvBackButton)
        mBack.visibility = View.INVISIBLE

        // Bottom Info
        tvBottomLayout = findViewById(R.id.tvBottomLayout)
        tvBottomLayout.visibility = View.INVISIBLE
        tv_channelname = findViewById(R.id.tv_channel_name) //底部名称
        tv_channelnum = findViewById(R.id.tv_channel_number) //底部数字
        tv_logo = findViewById(R.id.tv_logo)
        tv_curr_time = findViewById(R.id.tv_current_program_time)
        tv_curr_name = findViewById(R.id.tv_current_program_name)
        tv_next_time = findViewById(R.id.tv_next_program_time)
        tv_next_name = findViewById(R.id.tv_next_program_name)

        // EPG Info
        mGroupEPG = findViewById(R.id.mGroupEPG)
        mDivRight = findViewById(R.id.mDivRight)
        mDivLeft = findViewById(R.id.mDivLeft)
        mEpgDateGridView = findViewById(R.id.mEpgDateGridView)
        mEpgInfoGridView = findViewById(R.id.mEpgInfoGridView)

        // Left Layout
        tvLeftChannelListLayout = findViewById(R.id.tvLeftChannelListLayout)
        mGroupGridView = findViewById(R.id.mGroupGridView)
        mChannelGridView = findViewById(R.id.mChannelGridView)

        // Right Layout
        tvRightSettingLayout = findViewById(R.id.tvRightSettingLayout)
        mSettingGroupView = findViewById(R.id.mSettingGroupView)
        mSettingItemView = findViewById(R.id.mSettingItemView)

        // Not in Used
        tvTime = findViewById(R.id.tvTime)
        tvNetSpeed = findViewById(R.id.tvNetSpeed)

        // Initialization
        initEpgDateView()
        initEpgListView()
        initVideoView()
        initChannelGroupView()
        initLiveChannelView()
        initSettingGroupView()
        initSettingItemView()
        initLiveChannelList()
        initLiveSettingGroupList()

        // takagen99 : Add SeekBar for VOD
        mSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser) {
                    return
                }
                mHandler.removeCallbacks(mHideChannelInfoRun)
                mHandler.postDelayed(mHideChannelInfoRun, 6000)
                val duration = mVideoView.duration
                val newPosition = duration * progress / seekBar.max
                mCurrentTime.text = stringForTimeVod(newPosition.toInt())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                mIsDragging = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                mIsDragging = false
                val duration = mVideoView.duration
                val newPosition = duration * seekBar.progress / seekBar.max
                mVideoView.seekTo(newPosition.toInt().toLong())
            }
        })
        mSeekBar.setOnKeyListener { arg0, keycode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (keycode == KeyEvent.KEYCODE_DPAD_LEFT || keycode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    mIsDragging = true
                }
            } else if (event.action == KeyEvent.ACTION_UP) {
                mIsDragging = false
                val duration = mVideoView.duration
                val newPosition = duration * mSeekBar.progress / mSeekBar.max
                mVideoView.seekTo(newPosition.toInt().toLong())
            }
            false
        }
        // Button: BACK click to go back to previous page -------------------
        mBack.setOnClickListener { finish() }
    }

    private val piPON: Boolean
        get() = Hawk.get(HawkConfig.BACKGROUND_PLAY_TYPE, 0) == 2

    // takagen99 : Enter PIP if supported
    override fun onUserLeaveHint() {
        if (supportsPiPMode() && piPON) {
            // Hide controls when entering PIP
            mHandler.post(mHideChannelListRun)
            mHandler.post(mHideChannelInfoRun)
            mHandler.post(mHideSettingLayoutRun)
            enterPictureInPictureMode()
        }
    }

    override fun onBackPressed() {
        if (tvLeftChannelListLayout.visibility == View.VISIBLE) {
            mHandler.removeCallbacks(mHideChannelListRun)
            mHandler.post(mHideChannelListRun)
        } else if (tvRightSettingLayout.visibility == View.VISIBLE) {
            mHandler.removeCallbacks(mHideSettingLayoutRun)
            mHandler.post(mHideSettingLayoutRun)
        } else if (tvBottomLayout.visibility == View.VISIBLE) {
            mHandler.removeCallbacks(mHideChannelInfoRun)
            mHandler.post(mHideChannelInfoRun)
        } else {
            mHandler.removeCallbacks(mConnectTimeoutChangeSourceRun)
            mHandler.removeCallbacks(mUpdateNetSpeedRun)
            mHandler.removeCallbacks(mUpdateTimeRun)
            mHandler.removeCallbacks(tvSysTimeRunnable)
            exit()
        }
    }

    private var mExitTime: Long = 0
    private fun exit() {
        if (System.currentTimeMillis() - mExitTime < 2000) {
            super.onBackPressed()
        } else {
            mExitTime = System.currentTimeMillis()
            Toast.makeText(mContext, getString(R.string.hm_exit_live), Toast.LENGTH_SHORT).show()
        }
    }

    private val mPlaySelectedChannel = Runnable {
        tvSelectedChannel.visibility = View.GONE
        tvSelectedChannel.text = ""
        var grpIndx = 0
        var chaIndx = 0
        var getMin = 1
        var getMax: Int
        for (j in 0 until 20) {
            getMax = getMin + getLiveChannels(j).size - 1
            if (selectedChannelNumber in getMin..getMax) {
                grpIndx = j
                chaIndx = selectedChannelNumber - getMin + 1
                break
            } else {
                getMin = getMax + 1
            }
        }
        if (selectedChannelNumber > 0) {
            playChannel(grpIndx, chaIndx - 1, false)
        }
        selectedChannelNumber = 0
    }

    private fun numericKeyDown(digit: Int) {
        selectedChannelNumber = selectedChannelNumber * 10 + digit
        tvSelectedChannel.text = selectedChannelNumber.toString()
        tvSelectedChannel.visibility = View.VISIBLE
        mHandler.removeCallbacks(mPlaySelectedChannel)
        mHandler.postDelayed(mPlaySelectedChannel, 2000)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val keyCode = event.keyCode
            if (keyCode == KeyEvent.KEYCODE_MENU) {
                showSettingGroup()
            } else if (!isListOrSettingLayoutVisible) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> if (Hawk.get(HawkConfig.LIVE_CHANNEL_REVERSE, false)) playNext() else playPrevious()
                    KeyEvent.KEYCODE_DPAD_DOWN -> if (Hawk.get(HawkConfig.LIVE_CHANNEL_REVERSE, false)) playPrevious() else playNext()
                    KeyEvent.KEYCODE_DPAD_LEFT -> if (!isVOD) {
                        showSettingGroup()
                    } else {
                        showChannelInfo()
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> if (!isVOD) {
                        playNextSource()
                    } else {
                        showChannelInfo()
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> showChannelList()
                    else -> {
                        var digit = -1
                        if (keyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9) {
                            digit = keyCode - KeyEvent.KEYCODE_0
                        } else if (keyCode in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9) {
                            digit = keyCode - KeyEvent.KEYCODE_NUMPAD_0
                        }
                        if (digit != -1) {
                            numericKeyDown(digit)
                        }
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    // takagen99 : Use onStopCalled to track close activity
    private var onStopCalled = false
    override fun onResume() {
        super.onResume()
        mVideoView.resume()
    }

    override fun onStop() {
        super.onStop()
        onStopCalled = true
    }

    override fun onPause() {
        super.onPause()
        if (supportsPiPMode()) {
            if (isInPictureInPictureMode) {
                mVideoView.resume()
            } else {
                mVideoView.pause()
            }
        } else {
            mVideoView.pause()
        }
    }

    // takagen99 : PIP fix to close video when close window
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (supportsPiPMode()) {
            if (!isInPictureInPictureMode) {
                if (onStopCalled) {
                    mVideoView.release()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mVideoView.release()
        EventBus.getDefault().unregister(this)
    }

    private fun showChannelList() {
        mBack.visibility = View.INVISIBLE
        if (tvBottomLayout.visibility == View.VISIBLE) {
            mHandler.removeCallbacks(mHideChannelInfoRun)
            mHandler.post(mHideChannelInfoRun)
        } else if (tvRightSettingLayout.visibility == View.VISIBLE) {
            mHandler.removeCallbacks(mHideSettingLayoutRun)
            mHandler.post(mHideSettingLayoutRun)
        } else if (tvLeftChannelListLayout.visibility == View.INVISIBLE && tvRightSettingLayout.visibility == View.INVISIBLE) {
            //重新载入上一次状态
            liveChannelItemAdapter.setNewData(getLiveChannels(currentChannelGroupIndex))
            if (currentLiveChannelIndex > -1) mChannelGridView.scrollToPosition(currentLiveChannelIndex)
            mChannelGridView.setSelectedPosition(currentLiveChannelIndex)
            mGroupGridView.scrollToPosition(currentChannelGroupIndex)
            mGroupGridView.setSelectedPosition(currentChannelGroupIndex)
            mHandler.postDelayed(mFocusCurrentChannelAndShowChannelList, 200)
            mHandler.post(tvSysTimeRunnable)
        } else {
            mBack.visibility = View.INVISIBLE
            mHandler.removeCallbacks(mHideChannelListRun)
            mHandler.post(mHideChannelListRun)
            mHandler.removeCallbacks(tvSysTimeRunnable)
        }
    }

    //频道列表
    fun divLoadEpgR(view: View?) {
        mGroupGridView.visibility = View.GONE
        mEpgInfoGridView.visibility = View.VISIBLE
        mGroupEPG.visibility = View.VISIBLE
        mDivLeft.visibility = View.VISIBLE
        mDivRight.visibility = View.GONE
        tvLeftChannelListLayout.visibility = View.INVISIBLE
        showChannelList()
    }

    fun divLoadEpgL(view: View?) {
        mGroupGridView.visibility = View.VISIBLE
        mEpgInfoGridView.visibility = View.GONE
        mGroupEPG.visibility = View.GONE
        mDivLeft.visibility = View.GONE
        mDivRight.visibility = View.VISIBLE
        tvLeftChannelListLayout.visibility = View.INVISIBLE
        showChannelList()
    }

    private val mFocusCurrentChannelAndShowChannelList = object : Runnable {
        override fun run() {
            if (mGroupGridView.isScrolling || mChannelGridView.isScrolling || mGroupGridView.isComputingLayout || mChannelGridView.isComputingLayout) {
                mHandler.postDelayed(this, 100)
            } else {
                liveChannelGroupAdapter.selectedGroupIndex = currentChannelGroupIndex
                liveChannelItemAdapter.selectedChannelIndex = currentLiveChannelIndex
                val holder = mChannelGridView.findViewHolderForAdapterPosition(currentLiveChannelIndex)
                holder?.itemView?.requestFocus()
                tvLeftChannelListLayout.visibility = View.VISIBLE
                tvLeftChannelListLayout.alpha = 0.0f
                tvLeftChannelListLayout.translationX = (-tvLeftChannelListLayout.width / 2).toFloat()
                tvLeftChannelListLayout.animate()
                        .translationX(0f)
                        .alpha(1.0f)
                        .setDuration(250)
                        .setInterpolator(DecelerateInterpolator())
                        .setListener(null)
                mHandler.removeCallbacks(mHideChannelListRun)
                mHandler.postDelayed(mHideChannelListRun, 6000)
                mHandler.postDelayed(mUpdateLayout, 255) // Workaround Fix : SurfaceView
            }
        }
    }
    private val mUpdateLayout = Runnable {
        tvLeftChannelListLayout.requestLayout()
        tvRightSettingLayout.requestLayout()
    }
    private val mHideChannelListRun = Runnable {
        if (tvLeftChannelListLayout.visibility == View.VISIBLE) {
            tvLeftChannelListLayout.animate()
                    .translationX((-tvLeftChannelListLayout.width / 2).toFloat())
                    .alpha(0.0f)
                    .setDuration(250)
                    .setInterpolator(DecelerateInterpolator())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            tvLeftChannelListLayout.visibility = View.INVISIBLE
                            tvLeftChannelListLayout.clearAnimation()
                        }
                    })
        }
    }

    private fun showChannelInfo() {
        // takagen99: Check if Touch Screen, show back button
        if (supportsTouch()) {
            mBack.visibility = View.VISIBLE
        }
        if (tvBottomLayout.visibility == View.GONE || tvBottomLayout.visibility == View.INVISIBLE) {
            tvBottomLayout.visibility = View.VISIBLE
            tvBottomLayout.translationY = (tvBottomLayout.height / 2).toFloat()
            tvBottomLayout.alpha = 0.0f
            tvBottomLayout.animate()
                    .alpha(1.0f)
                    .setDuration(250)
                    .setInterpolator(DecelerateInterpolator())
                    .translationY(0f)
                    .setListener(null)
        }
        mHandler.removeCallbacks(mHideChannelInfoRun)
        mHandler.postDelayed(mHideChannelInfoRun, 6000)
        mHandler.postDelayed(mUpdateLayout, 255) // Workaround Fix : SurfaceView
    }

    private val mHideChannelInfoRun = Runnable {
        mBack.visibility = View.INVISIBLE
        if (tvBottomLayout.visibility == View.VISIBLE) {
            tvBottomLayout.animate()
                    .alpha(0.0f)
                    .setDuration(250)
                    .setInterpolator(DecelerateInterpolator())
                    .translationY((tvBottomLayout.height / 2).toFloat())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            tvBottomLayout.visibility = View.INVISIBLE
                            tvBottomLayout.clearAnimation()
                        }
                    })
        }
    }

    private fun toggleChannelInfo() {
        if (tvLeftChannelListLayout.visibility == View.VISIBLE) {
            mHandler.removeCallbacks(mHideChannelListRun)
            mHandler.post(mHideChannelListRun)
        } else if (tvRightSettingLayout.visibility == View.VISIBLE) {
            mHandler.removeCallbacks(mHideSettingLayoutRun)
            mHandler.post(mHideSettingLayoutRun)
        } else if (tvBottomLayout.visibility == View.INVISIBLE) {
            showChannelInfo()
        } else {
            mBack.visibility = View.INVISIBLE
            mHandler.removeCallbacks(mHideChannelInfoRun)
            mHandler.post(mHideChannelInfoRun)
            mHandler.post(mUpdateLayout) // Workaround Fix : SurfaceView
        }
    }

    //显示侧边EPG
    private fun showEpg(date: Date, arrayList: ArrayList<Epginfo>?) {
        if (arrayList != null && arrayList.size > 0) {
            epgdata = arrayList
            epgListAdapter.CanBack(currentLiveChannelItem?.include_back ?: false)
            epgListAdapter.setNewData(epgdata)
            var i = -1
            var size = epgdata.size - 1
            while (size >= 0) {
                if (Date().compareTo(epgdata[size].startdateTime) >= 0) {
                    break
                }
                size--
            }
            i = size
            if (i >= 0 && Date().compareTo(epgdata[i].enddateTime) <= 0) {
                mEpgInfoGridView.setSelectedPosition(i)
                epgListAdapter.selectedEpgIndex = i
                val finalI = i
                mEpgInfoGridView.post { mEpgInfoGridView.smoothScrollToPosition(finalI) }
            }
        } else {
            val epgbcinfo = Epginfo(date, "暂无节目信息", date, "00:00", "23:59", 0)
            val list = ArrayList<Epginfo>()
            list.add(epgbcinfo)
            epgdata = list
            epgListAdapter.setNewData(epgdata)
        }
    }

    private val tvSysTimeRunnable = object : Runnable {
        override fun run() {
            val date = Date()
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
            tv_sys_time.text = timeFormat.format(date)
            mHandler.postDelayed(this, 1000)

            // takagen99 : Update SeekBar
            if (!mIsDragging) {
                val currentPosition = mVideoView.currentPosition.toInt()
                mCurrentTime.text = stringForTimeVod(currentPosition)
                mSeekBar.progress = currentPosition
            }
        }
    }

    //显示底部EPG
    private fun showBottomEpg() {
        if (isSHIYI) return
        val channelItem = channel_Name ?: return
        val channelName = channelItem.channelName
        if (channelName != null) {
            showChannelInfo()
            val datePresented = epgDateAdapter.getItem(epgDateAdapter.selectedIndex)?.datePresented
            val savedEpgKey = channelName + "_" + datePresented
            if (hsEpg.containsKey(savedEpgKey)) {
                val epgInfo = EpgUtil.getEpgInfo(channelName)
                getTvLogo(channelName, epgInfo?.get(0))
                val arrayList = hsEpg[savedEpgKey]
                if (arrayList != null && arrayList.size > 0) {
                    val date = Date()
                    var size = arrayList.size - 1
                    while (size >= 0) {
                        val epg = arrayList[size]
                        if (date.after(epg.startdateTime) && date.before(epg.enddateTime)) {
                            tv_curr_time.text = "${epg.start} - ${epg.end}"
                            tv_curr_name.text = epg.title
                            if (size != arrayList.size - 1) {
                                val nextEpg = arrayList[size + 1]
                                tv_next_time.text = "${nextEpg.start} - ${nextEpg.end}"
                                tv_next_name.text = nextEpg.title
                            } else {
                                tv_next_time.text = "00:00 - 23:59"
                                tv_next_name.text = "No Information"
                            }
                            break
                        } else {
                            size--
                        }
                    }
                }
                epgListAdapter.CanBack(currentLiveChannelItem?.include_back ?: false)
                epgListAdapter.setNewData(arrayList)
            } else {
                val selectedIndex = epgDateAdapter.selectedIndex
                if (selectedIndex < 0) getEpg(Date()) else getEpg(epgDateAdapter.data[selectedIndex].dateParamVal)
            }
        }
    }

    // 获取EPG并存储 // 百川epg
    private var epgdata: List<Epginfo> = ArrayList()

    // Get Channel Logo
    private fun getTvLogo(channelName: String, logoUrl: String?) {
        val options = RequestOptions()
        options.diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .placeholder(R.drawable.img_logo_placeholder)
        Glide.with(App.getInstance())
                .load(logoUrl)
                .apply(options)
                .into(tv_logo)
    }

    fun getEpg(date: Date) {
        val channelItem = channel_Name ?: return
        val channelName = channelItem.channelName
        val timeFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        timeFormat.timeZone = TimeZone.getTimeZone("GMT+8:00")
        val epgInfo = EpgUtil.getEpgInfo(channelName)
        var epgTagName = channelName
        getTvLogo(channelName, epgInfo?.get(0))
        if (epgInfo != null && !epgInfo[1].isEmpty()) {
            epgTagName = epgInfo[1]
        }
        epgListAdapter.CanBack(currentLiveChannelItem?.include_back ?: false)
        val epgUrl: String = if (epgStringAddress.contains("{name}") && epgStringAddress.contains("{date}")) {
            epgStringAddress.replace("{name}", URLEncoder.encode(epgTagName, "UTF-8")).replace("{date}", timeFormat.format(date))
        } else {
            epgStringAddress + "?ch=" + URLEncoder.encode(epgTagName, "UTF-8") + "&date=" + timeFormat.format(date)
        }
        OkGo.get<String>(epgUrl).execute(object : StringCallback() {
            override fun onSuccess(response: Response<String>) {
                val paramString = response.body()
                val arrayList = ArrayList<Epginfo>()
                try {
                    if (paramString.contains("epg_data")) {
                        val jSONArray = JSONObject(paramString).optJSONArray("epg_data")
                        if (jSONArray != null) for (b in 0 until jSONArray.length()) {
                            val jSONObject = jSONArray.getJSONObject(b)
                            val epgbcinfo = Epginfo(date, jSONObject.optString("title"), date, jSONObject.optString("start"), jSONObject.optString("end"), b)
                            arrayList.add(epgbcinfo)
                        }
                    }
                } catch (jSONException: JSONException) {
                    jSONException.printStackTrace()
                }
                showEpg(date, arrayList)
                val datePresented = epgDateAdapter.getItem(epgDateAdapter.selectedIndex)?.datePresented
                val savedEpgKey = channelName + "_" + datePresented
                if (!hsEpg.containsKey(savedEpgKey)) hsEpg[savedEpgKey] = arrayList
                showBottomEpg()
            }

            override fun onError(response: Response<String>) {
                showEpg(date, ArrayList())
                showBottomEpg()
            }
        })
    }

    private fun replayChannel(): Boolean {
        mVideoView.release()
        val channels = getLiveChannels(currentChannelGroupIndex)
        if (currentLiveChannelIndex < 0 || currentLiveChannelIndex >= channels.size) return true
        val item = channels[currentLiveChannelIndex]
        currentLiveChannelItem = item
        Hawk.put(HawkConfig.LIVE_CHANNEL, item.channelName)
        HawkUtils.setLastLiveChannelGroup(liveChannelGroupList[currentChannelGroupIndex].groupName)
        livePlayerManager.getLiveChannelPlayer(mVideoView, item.channelName)
        channel_Name = item
        item.include_back = item.url.indexOf("PLTV/8888") != -1
        mHandler.post(tvSysTimeRunnable)
        tv_channelname.text = item.channelName
        tv_channelnum.text = item.channelNum.toString()
        tv_source.text = "线路 " + (item.sourceIndex + 1) + "/" + item.sourceNum
        getEpg(Date())
        mVideoView.setUrl(item.url, setPlayHeaders(item.url))
        showChannelInfo()
        mVideoView.start()
        return true
    }

    //节目播放
    private fun playChannel(channelGroupIndex: Int, liveChannelIndex: Int, changeSource: Boolean): Boolean {
        if (channelGroupIndex == currentChannelGroupIndex && liveChannelIndex == currentLiveChannelIndex && !changeSource || changeSource && currentLiveChannelItem?.sourceNum == 1) {
            showChannelInfo()
            return true
        }
        mVideoView.release()
        if (!changeSource) {
            currentChannelGroupIndex = channelGroupIndex
            currentLiveChannelIndex = liveChannelIndex
            currentLiveChannelItem = getLiveChannels(currentChannelGroupIndex)[currentLiveChannelIndex]
            val item = currentLiveChannelItem!!
            Hawk.put(HawkConfig.LIVE_CHANNEL, item.channelName)
            HawkUtils.setLastLiveChannelGroup(liveChannelGroupList[currentChannelGroupIndex].groupName)
            livePlayerManager.getLiveChannelPlayer(mVideoView, item.channelName)
        }
        val item = currentLiveChannelItem!!
        channel_Name = item
        item.include_back = item.url.indexOf("PLTV/8888") != -1

        // takagen99 : Moved update of Channel Info here before getting EPG (no dependency on EPG)
        mHandler.post(tvSysTimeRunnable)

        // Channel Name & No. + Source No.
        tv_channelname.text = item.channelName
        tv_channelnum.text = item.channelNum.toString()
        tv_source.text = "线路 " + (item.sourceIndex + 1) + "/" + item.sourceNum
        getEpg(Date())
        mVideoView.setUrl(item.url, setPlayHeaders(item.url))
        showChannelInfo()
        mVideoView.start()
        return true
    }

    private fun playNext() {
        if (!isCurrentLiveChannelValid) return
        val groupChannelIndex = getNextChannel(1)
        playChannel(groupChannelIndex[0], groupChannelIndex[1], false)
    }

    private fun playPrevious() {
        if (!isCurrentLiveChannelValid) return
        val groupChannelIndex = getNextChannel(-1)
        playChannel(groupChannelIndex[0], groupChannelIndex[1], false)
    }

    fun playPreSource() {
        if (!isCurrentLiveChannelValid) return
        currentLiveChannelItem?.preSource()
        playChannel(currentChannelGroupIndex, currentLiveChannelIndex, true)
    }

    fun playNextSource() {
        if (!isCurrentLiveChannelValid) return
        currentLiveChannelItem?.nextSource()
        playChannel(currentChannelGroupIndex, currentLiveChannelIndex, true)
    }

    //显示设置列表
    private fun showSettingGroup() {
        mBack.visibility = View.INVISIBLE
        if (tvLeftChannelListLayout.visibility == View.VISIBLE) {
            mHandler.removeCallbacks(mHideChannelListRun)
            mHandler.post(mHideChannelListRun)
        } else if (tvBottomLayout.visibility == View.VISIBLE) {
            mHandler.removeCallbacks(mHideChannelInfoRun)
            mHandler.post(mHideChannelInfoRun)
        } else if (tvRightSettingLayout.visibility == View.INVISIBLE) {
            if (!isCurrentLiveChannelValid) return
            //重新载入默认状态
            loadCurrentSourceList()
            liveSettingGroupAdapter.setNewData(liveSettingGroupList)
            selectSettingGroup(0, false)
            mSettingGroupView.scrollToPosition(0)
            mSettingItemView.scrollToPosition(currentLiveChannelItem?.sourceIndex ?: 0)
            mHandler.postDelayed(mFocusAndShowSettingGroup, 200)
        } else {
            mBack.visibility = View.INVISIBLE
            mHandler.removeCallbacks(mHideSettingLayoutRun)
            mHandler.post(mHideSettingLayoutRun)
        }
    }

    private val mFocusAndShowSettingGroup = object : Runnable {
        override fun run() {
            if (mSettingGroupView.isScrolling || mSettingItemView.isScrolling || mSettingGroupView.isComputingLayout || mSettingItemView.isComputingLayout) {
                mHandler.postDelayed(this, 100)
            } else {
                val holder = mSettingGroupView.findViewHolderForAdapterPosition(0)
                holder?.itemView?.requestFocus()
                tvRightSettingLayout.visibility = View.VISIBLE
                tvRightSettingLayout.alpha = 0.0f
                tvRightSettingLayout.translationX = (tvRightSettingLayout.width / 2).toFloat()
                tvRightSettingLayout.animate()
                        .translationX(0f)
                        .alpha(1.0f)
                        .setDuration(250)
                        .setInterpolator(DecelerateInterpolator())
                        .setListener(null)
                mHandler.removeCallbacks(mHideSettingLayoutRun)
                mHandler.postDelayed(mHideSettingLayoutRun, 6000)
                mHandler.postDelayed(mUpdateLayout, 255) // Workaround Fix : SurfaceView
            }
        }
    }
    private val mHideSettingLayoutRun = Runnable {
        if (tvRightSettingLayout.visibility == View.VISIBLE) {
            tvRightSettingLayout.animate()
                    .translationX((tvRightSettingLayout.width / 2).toFloat())
                    .alpha(0.0f)
                    .setDuration(250)
                    .setInterpolator(DecelerateInterpolator())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            tvRightSettingLayout.visibility = View.INVISIBLE
                            tvRightSettingLayout.clearAnimation()
                            liveSettingGroupAdapter.selectedGroupIndex = -1
                        }
                    })
        }
    }

    private fun initVideoView() {
        controller = LiveController(this)
        controller.setListener(object : LiveController.LiveControlListener {
            override fun singleTap(e: MotionEvent): Boolean {
                val fiveScreen = PlayerUtils.getScreenWidth(mContext, true) / 5
                if (e.x > 0 && e.x < fiveScreen * 2) {
                    showChannelList()
                } else if (e.x > fiveScreen * 2 && e.x < fiveScreen * 3) {
                    toggleChannelInfo()
                } else if (e.x > fiveScreen * 3) {
                    showSettingGroup()
                }
                return true
            }

            override fun longPress() {
                showSettingGroup()
            }

            override fun playStateChanged(playState: Int) {
                when (playState) {
                    VideoView.STATE_IDLE, VideoView.STATE_PAUSED -> {}
                    VideoView.STATE_PREPARED -> {
                        if (mVideoView.videoSize.size >= 2) {
                            tv_size.text = "${mVideoView.videoSize[0]} x ${mVideoView.videoSize[1]}"
                        }
                        val duration = mVideoView.duration.toInt()
                        if (duration > 0) {
                            isVOD = true
                            llSeekBar.visibility = View.VISIBLE
                            mSeekBar.progress = 10
                            mSeekBar.max = duration
                            mSeekBar.progress = 0
                            mTotalTime.text = stringForTimeVod(duration)
                        } else {
                            isVOD = false
                            llSeekBar.visibility = View.GONE
                        }
                    }
                    VideoView.STATE_BUFFERED, VideoView.STATE_PLAYING -> {
                        currentLiveChangeSourceTimes = 0
                        mHandler.removeCallbacks(mConnectTimeoutChangeSourceRun)
                        mHandler.removeCallbacks(mConnectTimeoutReplayRun)
                    }
                    VideoView.STATE_ERROR, VideoView.STATE_PLAYBACK_COMPLETED -> {
                        mHandler.removeCallbacks(mConnectTimeoutChangeSourceRun)
                        mHandler.removeCallbacks(mConnectTimeoutReplayRun)
                        if (Hawk.get(HawkConfig.LIVE_CONNECT_TIMEOUT, 2) == 0) {
                            mHandler.postDelayed(mConnectTimeoutReplayRun, 30 * 1000L)
                        } else {
                            mHandler.post(mConnectTimeoutChangeSourceRun)
                        }
                    }
                    VideoView.STATE_PREPARING, VideoView.STATE_BUFFERING -> {
                        mHandler.removeCallbacks(mConnectTimeoutChangeSourceRun)
                        mHandler.removeCallbacks(mConnectTimeoutReplayRun)
                        if (Hawk.get(HawkConfig.LIVE_CONNECT_TIMEOUT, 2) == 0) {
                            mHandler.postDelayed(mConnectTimeoutReplayRun, 30 * 1000L)
                        } else {
                            mHandler.postDelayed(mConnectTimeoutChangeSourceRun, Hawk.get(HawkConfig.LIVE_CONNECT_TIMEOUT, 2) * 5000L)
                        }
                    }
                }
            }

            override fun changeSource(direction: Int) {
                if (direction > 0) playNextSource() else playPreSource()
            }
        })
        controller.setCanChangePosition(false)
        controller.setEnableInNormal(true)
        controller.setGestureEnabled(true)
        controller.setDoubleTapTogglePlayEnabled(false)
        mVideoView.setVideoController(controller)
        mVideoView.setProgressManager(null)
    }

    private val mConnectTimeoutChangeSourceRun = Runnable {
        currentLiveChangeSourceTimes++
        if (currentLiveChannelItem?.sourceNum == currentLiveChangeSourceTimes) {
            currentLiveChangeSourceTimes = 0
            val groupChannelIndex = getNextChannel(if (Hawk.get(HawkConfig.LIVE_CHANNEL_REVERSE, false)) -1 else 1)
            playChannel(groupChannelIndex[0], groupChannelIndex[1], false)
        } else {
            playNextSource()
        }
    }
    private val mConnectTimeoutReplayRun = Runnable { replayChannel() }
    private fun initEpgListView() {
        mEpgInfoGridView.setHasFixedSize(true)
        mEpgInfoGridView.layoutManager = V7LinearLayoutManager(mContext, 1, false)
        epgListAdapter = LiveEpgAdapter()
        mEpgInfoGridView.adapter = epgListAdapter
        mEpgInfoGridView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                mHandler.removeCallbacks(mHideChannelListRun)
                mHandler.postDelayed(mHideChannelListRun, 6000)
            }
        })
        //电视
        mEpgInfoGridView.setOnItemListener(object : TvRecyclerView.OnItemListener {
            override fun onItemPreSelected(parent: TvRecyclerView, itemView: View, position: Int) {
                epgListAdapter.focusedEpgIndex = -1
            }

            override fun onItemSelected(parent: TvRecyclerView, itemView: View, position: Int) {
                mHandler.removeCallbacks(mHideChannelListRun)
                mHandler.postDelayed(mHideChannelListRun, 6000)
                epgListAdapter.focusedEpgIndex = position
            }

            override fun onItemClick(parent: TvRecyclerView, itemView: View, position: Int) {
                val date = if (epgDateAdapter.selectedIndex < 0) Date() else epgDateAdapter.data[epgDateAdapter.selectedIndex].dateParamVal
                val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                dateFormat.timeZone = TimeZone.getTimeZone("GMT+8:00")
                val selectedData = epgListAdapter.getItem(position)
                val targetDate = dateFormat.format(date)
                val shiyiStartdate = targetDate + selectedData?.originStart?.replace(":", "") + "30"
                val shiyiEnddate = targetDate + selectedData?.originEnd?.replace(":", "") + "30"
                val now = Date()
                if (selectedData == null || now.compareTo(selectedData.startdateTime) < 0) {
                    return
                }
                epgListAdapter.selectedEpgIndex = position
                if (now.compareTo(selectedData.startdateTime) >= 0 && now.compareTo(selectedData.enddateTime) <= 0) {
                    mVideoView.release()
                    isSHIYI = false
                    mVideoView.setUrl(currentLiveChannelItem!!.url, setPlayHeaders(currentLiveChannelItem!!.url))
                    mVideoView.start()
                    epgListAdapter.setShiyiSelection(-1, false, timeFormat.format(date))
                } else {
                    mVideoView.release()
                    shiyi_time = "$shiyiStartdate-$shiyiEnddate"
                    isSHIYI = true
                    mVideoView.setUrl(currentLiveChannelItem!!.url + "?playseek=" + shiyi_time, setPlayHeaders(currentLiveChannelItem!!.url))
                    mVideoView.start()
                    epgListAdapter.setShiyiSelection(position, true, timeFormat.format(date))
                    mEpgInfoGridView.setSelectedPosition(position)
                }
            }
        })

        //手机/模拟器
        epgListAdapter.onItemClickListener = BaseQuickAdapter.OnItemClickListener { adapter, view, position ->
            val date = if (epgDateAdapter.selectedIndex < 0) Date() else epgDateAdapter.data[epgDateAdapter.selectedIndex].dateParamVal
            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            dateFormat.timeZone = TimeZone.getTimeZone("GMT+8:00")
            val selectedData = epgListAdapter.getItem(position)
            val targetDate = dateFormat.format(date)
            val shiyiStartdate = targetDate + selectedData?.originStart?.replace(":", "") + "30"
            val shiyiEnddate = targetDate + selectedData?.originEnd?.replace(":", "") + "30"
            val now = Date()
            if (selectedData == null || now.compareTo(selectedData.startdateTime) < 0) {
                return@OnItemClickListener
            }
            epgListAdapter.selectedEpgIndex = position
            if (now.compareTo(selectedData.startdateTime) >= 0 && now.compareTo(selectedData.enddateTime) <= 0) {
                mVideoView.release()
                isSHIYI = false
                mVideoView.setUrl(currentLiveChannelItem!!.url, setPlayHeaders(currentLiveChannelItem!!.url))
                mVideoView.start()
                epgListAdapter.setShiyiSelection(-1, false, timeFormat.format(date))
            } else {
                mVideoView.release()
                shiyi_time = "$shiyiStartdate-$shiyiEnddate"
                isSHIYI = true
                mVideoView.setUrl(currentLiveChannelItem!!.url + "?playseek=" + shiyi_time, setPlayHeaders(currentLiveChannelItem!!.url))
                mVideoView.start()
                epgListAdapter.setShiyiSelection(position, true, timeFormat.format(date))
                mEpgInfoGridView.setSelectedPosition(position)
            }
        }
    }

    private fun initEpgDateView() {
        mEpgDateGridView.setHasFixedSize(true)
        mEpgDateGridView.layoutManager = V7LinearLayoutManager(mContext, 1, false)
        epgDateAdapter = LiveEpgDateAdapter()
        val calendar = Calendar.getInstance()
        calendar.time = Date()
        val datePresentFormat = SimpleDateFormat("EEEE", Locale.SIMPLIFIED_CHINESE)
        calendar.add(Calendar.DAY_OF_MONTH, -6)
        for (i in 0..8) {
            val dateIns = calendar.time
            val epgDate = LiveEpgDate()
            epgDate.index = i
            when (i) {
                5 -> epgDate.datePresented = "昨天"
                6 -> epgDate.datePresented = "今天"
                7 -> epgDate.datePresented = "明天"
                8 -> epgDate.datePresented = "后天"
                else -> epgDate.datePresented = datePresentFormat.format(dateIns)
            }
            epgDate.dateParamVal = dateIns
            epgDateAdapter.addData(epgDate)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        mEpgDateGridView.adapter = epgDateAdapter
        mEpgDateGridView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                mHandler.removeCallbacks(mHideChannelListRun)
                mHandler.postDelayed(mHideChannelListRun, 6000)
            }
        })

        //电视
        mEpgDateGridView.setOnItemListener(object : TvRecyclerView.OnItemListener {
            override fun onItemPreSelected(parent: TvRecyclerView, itemView: View, position: Int) {
                epgDateAdapter.focusedIndex = -1
            }

            override fun onItemSelected(parent: TvRecyclerView, itemView: View, position: Int) {
                mHandler.removeCallbacks(mHideChannelListRun)
                mHandler.postDelayed(mHideChannelListRun, 6000)
                epgDateAdapter.focusedIndex = position
            }

            override fun onItemClick(parent: TvRecyclerView, itemView: View, position: Int) {
                mHandler.removeCallbacks(mHideChannelListRun)
                mHandler.postDelayed(mHideChannelListRun, 6000)
                epgDateAdapter.selectedIndex = position
                getEpg(epgDateAdapter.data[position].dateParamVal)
            }
        })

        //手机/模拟器
        epgDateAdapter.onItemClickListener = BaseQuickAdapter.OnItemClickListener { adapter, view, position ->
            FastClickCheckUtil.check(view)
            mHandler.removeCallbacks(mHideChannelListRun)
            mHandler.postDelayed(mHideChannelListRun, 6000)
            epgDateAdapter.selectedIndex = position
            getEpg(epgDateAdapter.data[position].dateParamVal)
        }
        epgDateAdapter.selectedIndex = 1
    }

    private fun initChannelGroupView() {
        mGroupGridView.setHasFixedSize(true)
        mGroupGridView.layoutManager = V7LinearLayoutManager(mContext, 1, false)
        liveChannelGroupAdapter = LiveChannelGroupAdapter()
        mGroupGridView.adapter = liveChannelGroupAdapter
        mGroupGridView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                mHandler.removeCallbacks(mHideChannelListRun)
                mHandler.postDelayed(mHideChannelListRun, 6000)
            }
        })

        //电视
        mGroupGridView.setOnItemListener(object : TvRecyclerView.OnItemListener {
            override fun onItemPreSelected(parent: TvRecyclerView, itemView: View, position: Int) {}
            override fun onItemSelected(parent: TvRecyclerView, itemView: View, position: Int) {
                selectChannelGroup(position, true, -1)
            }

            override fun onItemClick(parent: TvRecyclerView, itemView: View, position: Int) {
                if (isNeedInputPassword(position)) {
                    showPasswordDialog(position, -1)
                }
            }
        })

        //手机/模拟器
        liveChannelGroupAdapter.onItemClickListener = BaseQuickAdapter.OnItemClickListener { adapter, view, position ->
            FastClickCheckUtil.check(view)
            selectChannelGroup(position, false, -1)
        }
    }

    private fun selectChannelGroup(groupIndex: Int, focus: Boolean, liveChannelIndex: Int) {
        if (focus) {
            liveChannelGroupAdapter.focusedGroupIndex = groupIndex
            liveChannelItemAdapter.focusedChannelIndex = -1
        }
        if (groupIndex > -1 && groupIndex != liveChannelGroupAdapter.selectedGroupIndex || isNeedInputPassword(groupIndex)) {
            liveChannelGroupAdapter.selectedGroupIndex = groupIndex
            if (isNeedInputPassword(groupIndex)) {
                showPasswordDialog(groupIndex, liveChannelIndex)
                return
            }
            loadChannelGroupDataAndPlay(groupIndex, liveChannelIndex)
        }
        if (tvLeftChannelListLayout.visibility == View.VISIBLE) {
            mHandler.removeCallbacks(mHideChannelListRun)
            mHandler.postDelayed(mHideChannelListRun, 6000)
        }
    }

    private fun initLiveChannelView() {
        mChannelGridView.setHasFixedSize(true)
        mChannelGridView.layoutManager = V7LinearLayoutManager(mContext, 1, false)
        liveChannelItemAdapter = LiveChannelItemAdapter()
        mChannelGridView.adapter = liveChannelItemAdapter
        mChannelGridView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                mHandler.removeCallbacks(mHideChannelListRun)
                mHandler.postDelayed(mHideChannelListRun, 6000)
            }
        })

        //电视
        mChannelGridView.setOnItemListener(object : TvRecyclerView.OnItemListener {
            override fun onItemPreSelected(parent: TvRecyclerView, itemView: View, position: Int) {}
            override fun onItemSelected(parent: TvRecyclerView, itemView: View, position: Int) {
                if (position < 0) return
                liveChannelGroupAdapter.focusedGroupIndex = -1
                liveChannelItemAdapter.focusedChannelIndex = position
                mHandler.removeCallbacks(mHideChannelListRun)
                mHandler.postDelayed(mHideChannelListRun, 6000)
            }

            override fun onItemClick(parent: TvRecyclerView, itemView: View, position: Int) {
                clickLiveChannel(position)
            }
        })

        //手机/模拟器
        liveChannelItemAdapter.onItemClickListener = BaseQuickAdapter.OnItemClickListener { adapter, view, position ->
            FastClickCheckUtil.check(view)
            clickLiveChannel(position)
        }
    }

    private fun clickLiveChannel(position: Int) {
        liveChannelItemAdapter.selectedChannelIndex = position
        // Set default as Today
        epgDateAdapter.selectedIndex = 6
        if (tvLeftChannelListLayout.visibility == View.VISIBLE) {
            mHandler.removeCallbacks(mHideChannelListRun)
            mHandler.post(mHideChannelListRun)
        }
        playChannel(liveChannelGroupAdapter.selectedGroupIndex, position, false)
    }

    private fun initSettingGroupView() {
        mSettingGroupView.setHasFixedSize(true)
        mSettingGroupView.layoutManager = V7LinearLayoutManager(mContext, 1, false)
        liveSettingGroupAdapter = LiveSettingGroupAdapter()
        mSettingGroupView.adapter = liveSettingGroupAdapter
        mSettingGroupView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                mHandler.removeCallbacks(mHideSettingLayoutRun)
                mHandler.postDelayed(mHideSettingLayoutRun, 5000)
            }
        })

        //电视
        mSettingGroupView.setOnItemListener(object : TvRecyclerView.OnItemListener {
            override fun onItemPreSelected(parent: TvRecyclerView, itemView: View, position: Int) {}
            override fun onItemSelected(parent: TvRecyclerView, itemView: View, position: Int) {
                selectSettingGroup(position, true)
            }

            override fun onItemClick(parent: TvRecyclerView, itemView: View, position: Int) {}
        })

        //手机/模拟器
        liveSettingGroupAdapter.onItemClickListener = BaseQuickAdapter.OnItemClickListener { adapter, view, position ->
            FastClickCheckUtil.check(view)
            selectSettingGroup(position, false)
        }
    }

    private fun selectSettingGroup(position: Int, focus: Boolean) {
        if (!isCurrentLiveChannelValid) return
        if (focus) {
            liveSettingGroupAdapter.focusedGroupIndex = position
            liveSettingItemAdapter.focusedItemIndex = -1
        }
        if (position == liveSettingGroupAdapter.selectedGroupIndex || position < -1) return
        liveSettingGroupAdapter.selectedGroupIndex = position
        liveSettingItemAdapter.setNewData(liveSettingGroupList[position].liveSettingItems)
        when (position) {
            0 -> liveSettingItemAdapter.selectItem(currentLiveChannelItem?.sourceIndex ?: 0, true, false)
            1 -> liveSettingItemAdapter.selectItem(livePlayerManager.livePlayerScale, true, true)
            2 -> liveSettingItemAdapter.selectItem(livePlayerManager.livePlayerType, true, true)
        }
        var scrollToPosition = liveSettingItemAdapter.selectedItemIndexInternal
        if (scrollToPosition < 0) scrollToPosition = 0
        mSettingItemView.scrollToPosition(scrollToPosition)
        mHandler.removeCallbacks(mHideSettingLayoutRun)
        mHandler.postDelayed(mHideSettingLayoutRun, 5000)
    }

    private fun initSettingItemView() {
        mSettingItemView.setHasFixedSize(true)
        mSettingItemView.layoutManager = V7LinearLayoutManager(mContext, 1, false)
        liveSettingItemAdapter = LiveSettingItemAdapter()
        mSettingItemView.adapter = liveSettingItemAdapter
        mSettingItemView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                mHandler.removeCallbacks(mHideSettingLayoutRun)
                mHandler.postDelayed(mHideSettingLayoutRun, 5000)
            }
        })

        //电视
        mSettingItemView.setOnItemListener(object : TvRecyclerView.OnItemListener {
            override fun onItemPreSelected(parent: TvRecyclerView, itemView: View, position: Int) {}
            override fun onItemSelected(parent: TvRecyclerView, itemView: View, position: Int) {
                if (position < 0) return
                liveSettingGroupAdapter.focusedGroupIndex = -1
                liveSettingItemAdapter.focusedItemIndex = position
                mHandler.removeCallbacks(mHideSettingLayoutRun)
                mHandler.postDelayed(mHideSettingLayoutRun, 5000)
            }

            override fun onItemClick(parent: TvRecyclerView, itemView: View, position: Int) {
                clickSettingItem(position)
            }
        })

        //手机/模拟器
        liveSettingItemAdapter.onItemClickListener = BaseQuickAdapter.OnItemClickListener { adapter, view, position ->
            FastClickCheckUtil.check(view)
            clickSettingItem(position)
        }
    }

    private fun clickSettingItem(position: Int) {
        val settingGroupIndex = liveSettingGroupAdapter.selectedGroupIndex
        if (settingGroupIndex < 4) {
            if (position == liveSettingItemAdapter.selectedItemIndexInternal) return
            liveSettingItemAdapter.selectItem(position, true, true)
        }
        when (settingGroupIndex) {
            0 -> {
                currentLiveChannelItem?.sourceIndex = position
                playChannel(currentChannelGroupIndex, currentLiveChannelIndex, true)
            }
            1 -> livePlayerManager.changeLivePlayerScale(mVideoView, position, currentLiveChannelItem?.channelName)
            2 -> {
                mVideoView.release()
                livePlayerManager.changeLivePlayerType(mVideoView, position, currentLiveChannelItem?.channelName)
                mVideoView.setUrl(currentLiveChannelItem!!.url, setPlayHeaders(currentLiveChannelItem!!.url))
                mVideoView.start()
            }
            3 -> Hawk.put(HawkConfig.LIVE_CONNECT_TIMEOUT, position)
            4 -> {
                var select = false
                when (position) {
                    0 -> {
                        select = !Hawk.get(HawkConfig.LIVE_SHOW_TIME, false)
                        Hawk.put(HawkConfig.LIVE_SHOW_TIME, select)
                        showTime()
                    }
                    1 -> {
                        select = !Hawk.get(HawkConfig.LIVE_SHOW_NET_SPEED, false)
                        Hawk.put(HawkConfig.LIVE_SHOW_NET_SPEED, select)
                        showNetSpeed()
                    }
                    2 -> {
                        select = !Hawk.get(HawkConfig.LIVE_CHANNEL_REVERSE, false)
                        Hawk.put(HawkConfig.LIVE_CHANNEL_REVERSE, select)
                    }
                    3 -> {
                        select = !Hawk.get(HawkConfig.LIVE_CROSS_GROUP, false)
                        Hawk.put(HawkConfig.LIVE_CROSS_GROUP, select)
                    }
                    4 -> {
                        select = !Hawk.get(HawkConfig.LIVE_SKIP_PASSWORD, false)
                        Hawk.put(HawkConfig.LIVE_SKIP_PASSWORD, select)
                    }
                }
                liveSettingItemAdapter.selectItem(position, select, false)
            }
            5 -> when (position) {
                0 -> {
                    val liveHistory = Hawk.get<ArrayList<String>>(HawkConfig.LIVE_HISTORY, ArrayList())
                    if (liveHistory.isEmpty()) return
                    val current = Hawk.get(HawkConfig.LIVE_URL, "")
                    var idx = 0
                    if (liveHistory.contains(current)) idx = liveHistory.indexOf(current)
                    val dialog = ApiHistoryDialog(this@LivePlayActivity)
                    dialog.setTip(getString(R.string.dia_history_live))
                    dialog.setAdapter(object : ApiHistoryDialogAdapter.SelectDialogInterface {
                        override fun click(liveURL: String) {
                            var url = liveURL
                            Hawk.put(HawkConfig.LIVE_URL, url)
                            liveChannelGroupList.clear()
                            try {
                                url = Base64.encodeToString(url.toByteArray(charset("UTF-8")), Base64.DEFAULT or Base64.URL_SAFE or Base64.NO_WRAP)
                                url = "http://127.0.0.1:9978/proxy?do=live&type=txt&ext=$url"
                                loadProxyLives(url)
                            } catch (th: Throwable) {
                                th.printStackTrace()
                            }
                            dialog.dismiss()
                        }

                        override fun del(value: String, data: ArrayList<String>) {
                            Hawk.put(HawkConfig.LIVE_HISTORY, data)
                        }
                    }, liveHistory, idx)
                    dialog.show()
                }
            }
            6 -> when (position) {
                0 -> finish()
            }
        }
        mHandler.removeCallbacks(mHideSettingLayoutRun)
        mHandler.postDelayed(mHideSettingLayoutRun, 5000)
    }

    private fun initLiveChannelList() {
        val list = ApiConfig.get().channelGroupList
        if (list.isEmpty()) {
            Toast.makeText(App.getInstance(), getString(R.string.act_live_play_empty_channel), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        if (list.size == 1 && list[0].groupName.startsWith("http://127.0.0.1")) {
            loadProxyLives(list[0].groupName)
        } else {
            liveChannelGroupList.clear()
            liveChannelGroupList.addAll(list)
            showSuccess()
            initLiveState()
        }
    }

    //加载列表
    fun loadProxyLives(url: String) {
        var liveUrl = url
        try {
            val parsedUrl = Uri.parse(liveUrl)
            liveUrl = String(Base64.decode(parsedUrl.getQueryParameter("ext"), Base64.DEFAULT or Base64.URL_SAFE or Base64.NO_WRAP), charset("UTF-8"))
            if (liveUrl == "") {
                Toast.makeText(App.getInstance(), getString(R.string.act_live_play_empty_live_url), Toast.LENGTH_LONG).show()
                finish()
                return
            }
        } catch (th: Throwable) {
            Toast.makeText(App.getInstance(), getString(R.string.act_live_play_empty_channel), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        showLoading()
        OkGo.get<String>(liveUrl).execute(object : AbsCallback<String>() {
            override fun convertResponse(response: okhttp3.Response): String? {
                return response.body()?.string()
            }

            override fun onSuccess(response: Response<String>) {
                val linkedHashMap = LinkedHashMap<String, LinkedHashMap<String, ArrayList<String>>>()
                TxtSubscribe.parse(linkedHashMap, response.body())
                val livesArray = TxtSubscribe.live2JsonArray(linkedHashMap)
                ApiConfig.get().loadLives(livesArray)
                val list = ApiConfig.get().channelGroupList
                if (list.isEmpty()) {
                    Toast.makeText(App.getInstance(), getString(R.string.act_live_play_empty_channel), Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }
                liveChannelGroupList.clear()
                liveChannelGroupList.addAll(list)
                mHandler.post {
                    this@LivePlayActivity.showSuccess()
                    initLiveState()
                }
            }

            override fun onError(response: Response<String>) {
                super.onError(response)
                Toast.makeText(App.getInstance(), getString(R.string.act_live_play_network_error), Toast.LENGTH_LONG).show()
                finish()
            }
        })
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refresh(event: RefreshEvent) {
        if (event.type == RefreshEvent.TYPE_LIVEPLAY_UPDATE) {
            val bundle = event.obj as Bundle
            val channelGroupIndex = bundle.getInt("groupIndex", 0)
            val liveChannelIndex = bundle.getInt("channelIndex", 0)
            if (channelGroupIndex != liveChannelGroupAdapter.selectedGroupIndex) selectChannelGroup(channelGroupIndex, true, liveChannelIndex) else {
                clickLiveChannel(liveChannelIndex)
                mGroupGridView.scrollToPosition(channelGroupIndex)
                mChannelGridView.scrollToPosition(liveChannelIndex)
                playChannel(channelGroupIndex, liveChannelIndex, false)
            }
        }
    }

    private fun initLiveState() {
        var lastChannelGroupIndex = -1
        var lastLiveChannelIndex = -1
        val intent = intent
        if (intent != null && intent.extras != null) {
            val bundle = intent.extras!!
            lastChannelGroupIndex = bundle.getInt("groupIndex", 0)
            lastLiveChannelIndex = bundle.getInt("channelIndex", 0)
        } else {
            val lastChannel = JavaUtil.findLiveLastChannel(liveChannelGroupList)
            lastChannelGroupIndex = lastChannel.first
            lastLiveChannelIndex = lastChannel.second
        }
        livePlayerManager.init(mVideoView)
        showTime()
        showNetSpeed()
        tvLeftChannelListLayout.visibility = View.INVISIBLE
        tvRightSettingLayout.visibility = View.INVISIBLE
        liveChannelGroupAdapter.setNewData(liveChannelGroupList)
        selectChannelGroup(lastChannelGroupIndex, false, lastLiveChannelIndex)
    }

    private val isListOrSettingLayoutVisible: Boolean
        get() = tvLeftChannelListLayout.visibility == View.VISIBLE || tvRightSettingLayout.visibility == View.VISIBLE

    private fun initLiveSettingGroupList() {
        val groupNames = ArrayList(Arrays.asList("线路选择", "画面比例", "播放解码", "超时换源", "偏好设置", "直播地址", "退出直播"))
        val itemsArrayList = ArrayList<ArrayList<String>>()
        val sourceItems = ArrayList<String>()
        val scaleItems = ArrayList(Arrays.asList("默认", "16:9", "4:3", "填充", "原始", "裁剪"))
        val playerDecoderItems = ArrayList(Arrays.asList("系统", "ijk硬解", "ijk软解", "exo"))
        val timeoutItems = ArrayList(Arrays.asList("关", "5s", "10s", "15s", "20s", "25s", "30s"))
        val personalSettingItems = ArrayList(Arrays.asList("显示时间", "显示网速", "换台反转", "跨选分类", "关闭密码"))
        val liveAdd = ArrayList(Arrays.asList("列表历史"))
        val exitConfirm = ArrayList(Arrays.asList("确定"))
        itemsArrayList.add(sourceItems)
        itemsArrayList.add(scaleItems)
        itemsArrayList.add(playerDecoderItems)
        itemsArrayList.add(timeoutItems)
        itemsArrayList.add(personalSettingItems)
        itemsArrayList.add(liveAdd)
        itemsArrayList.add(exitConfirm)
        liveSettingGroupList.clear()
        for (i in 0 until groupNames.size) {
            val liveSettingGroup = LiveSettingGroup()
            val liveSettingItemList = ArrayList<LiveSettingItem>()
            liveSettingGroup.groupIndex = i
            liveSettingGroup.groupName = groupNames[i]
            for (j in 0 until itemsArrayList[i].size) {
                val liveSettingItem = LiveSettingItem()
                liveSettingItem.itemIndex = j
                liveSettingItem.itemName = itemsArrayList[i][j]
                liveSettingItemList.add(liveSettingItem)
            }
            liveSettingGroup.liveSettingItems = liveSettingItemList
            liveSettingGroupList.add(liveSettingGroup)
        }
        liveSettingGroupList[3].liveSettingItems[Hawk.get(HawkConfig.LIVE_CONNECT_TIMEOUT, 2)].isItemSelected = true
        liveSettingGroupList[4].liveSettingItems[0].isItemSelected = Hawk.get(HawkConfig.LIVE_SHOW_TIME, false)
        liveSettingGroupList[4].liveSettingItems[1].isItemSelected = Hawk.get(HawkConfig.LIVE_SHOW_NET_SPEED, false)
        liveSettingGroupList[4].liveSettingItems[2].isItemSelected = Hawk.get(HawkConfig.LIVE_CHANNEL_REVERSE, false)
        liveSettingGroupList[4].liveSettingItems[3].isItemSelected = Hawk.get(HawkConfig.LIVE_CROSS_GROUP, false)
        liveSettingGroupList[4].liveSettingItems[4].isItemSelected = Hawk.get(HawkConfig.LIVE_SKIP_PASSWORD, false)
    }

    private fun loadCurrentSourceList() {
        val currentSourceNames = currentLiveChannelItem!!.channelSourceNames
        val liveSettingItemList = ArrayList<LiveSettingItem>()
        for (j in 0 until currentSourceNames.size) {
            val liveSettingItem = LiveSettingItem()
            liveSettingItem.itemIndex = j
            liveSettingItem.itemName = currentSourceNames[j]
            liveSettingItemList.add(liveSettingItem)
        }
        liveSettingGroupList[0].liveSettingItems = liveSettingItemList
    }

    fun showTime() {
        if (Hawk.get(HawkConfig.LIVE_SHOW_TIME, false)) {
            mHandler.post(mUpdateTimeRun)
            tvTime.visibility = View.VISIBLE
        } else {
            mHandler.removeCallbacks(mUpdateTimeRun)
            tvTime.visibility = View.GONE
        }
    }

    private val mUpdateTimeRun = object : Runnable {
        override fun run() {
            val day = Date()
            val df = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            tvTime.text = df.format(day)
            mHandler.postDelayed(this, 1000)
        }
    }

    fun showNetSpeed() {
        if (Hawk.get(HawkConfig.LIVE_SHOW_NET_SPEED, false)) {
            mHandler.post(mUpdateNetSpeedRun)
            tvNetSpeed.visibility = View.VISIBLE
        } else {
            mHandler.removeCallbacks(mUpdateNetSpeedRun)
            tvNetSpeed.visibility = View.GONE
        }
    }

    private val mUpdateNetSpeedRun = object : Runnable {
        override fun run() {
            tvNetSpeed.text = String.format("%.2fMB/s", mVideoView.tcpSpeed.toFloat() / 1024.0 / 1024.0)
            mHandler.postDelayed(this, 1000)
        }
    }

    private fun showPasswordDialog(groupIndex: Int, liveChannelIndex: Int) {
        if (tvLeftChannelListLayout.visibility == View.VISIBLE) mHandler.removeCallbacks(mHideChannelListRun)
        val dialog = LivePasswordDialog(this)
        dialog.setOnListener(object : LivePasswordDialog.OnListener {
            override fun onChange(password: String) {
                if (password == liveChannelGroupList[groupIndex].groupPassword) {
                    channelGroupPasswordConfirmed.add(groupIndex)
                    loadChannelGroupDataAndPlay(groupIndex, liveChannelIndex)
                } else {
                    Toast.makeText(App.getInstance(), "密码错误", Toast.LENGTH_SHORT).show()
                }
                if (tvLeftChannelListLayout.visibility == View.VISIBLE) mHandler.postDelayed(mHideChannelListRun, 6000)
            }

            override fun onCancel() {
                if (tvLeftChannelListLayout.visibility == View.VISIBLE) {
                    val groupIdx = liveChannelGroupAdapter.selectedGroupIndex
                    liveChannelItemAdapter.setNewData(getLiveChannels(groupIdx))
                }
            }
        })
        dialog.show()
    }

    private fun loadChannelGroupDataAndPlay(groupIndex: Int, liveChannelIndex: Int) {
        liveChannelItemAdapter.setNewData(getLiveChannels(groupIndex))
        if (groupIndex == currentChannelGroupIndex) {
            if (currentLiveChannelIndex > -1) mChannelGridView.scrollToPosition(currentLiveChannelIndex)
            liveChannelItemAdapter.selectedChannelIndex = currentLiveChannelIndex
        } else {
            mChannelGridView.scrollToPosition(0)
            liveChannelItemAdapter.selectedChannelIndex = -1
        }
        if (liveChannelIndex > -1) {
            clickLiveChannel(liveChannelIndex)
            mGroupGridView.scrollToPosition(groupIndex)
            mChannelGridView.scrollToPosition(liveChannelIndex)
            playChannel(groupIndex, liveChannelIndex, false)
        }
    }

    private fun isNeedInputPassword(groupIndex: Int): Boolean {
        return !liveChannelGroupList[groupIndex].groupPassword.isEmpty() && !isPasswordConfirmed(groupIndex)
    }

    private fun isPasswordConfirmed(groupIndex: Int): Boolean {
        if (Hawk.get(HawkConfig.LIVE_SKIP_PASSWORD, false)) {
            return true
        } else {
            for (confirmedNum in channelGroupPasswordConfirmed) {
                if (confirmedNum == groupIndex) return true
            }
            return false
        }
    }

    private fun getLiveChannels(groupIndex: Int): ArrayList<LiveChannelItem> {
        return if (!isNeedInputPassword(groupIndex)) {
            liveChannelGroupList[groupIndex].liveChannels
        } else {
            ArrayList()
        }
    }

    private fun getNextChannel(direction: Int): Array<Int> {
        var channelGroupIndex = currentChannelGroupIndex
        var liveChannelIndex = currentLiveChannelIndex

        //跨选分组模式下跳过加密频道分组（遥控器上下键换台/超时换源）
        if (direction > 0) {
            liveChannelIndex++
            if (liveChannelIndex >= getLiveChannels(channelGroupIndex).size) {
                liveChannelIndex = 0
                if (Hawk.get(HawkConfig.LIVE_CROSS_GROUP, false)) {
                    do {
                        channelGroupIndex++
                        if (channelGroupIndex >= liveChannelGroupList.size) channelGroupIndex = 0
                    } while (!liveChannelGroupList[channelGroupIndex].groupPassword.isEmpty() || channelGroupIndex == currentChannelGroupIndex)
                }
            }
        } else {
            liveChannelIndex--
            if (liveChannelIndex < 0) {
                if (Hawk.get(HawkConfig.LIVE_CROSS_GROUP, false)) {
                    do {
                        channelGroupIndex--
                        if (channelGroupIndex < 0) channelGroupIndex = liveChannelGroupList.size - 1
                    } while (!liveChannelGroupList[channelGroupIndex].groupPassword.isEmpty() || channelGroupIndex == currentChannelGroupIndex)
                }
                liveChannelIndex = getLiveChannels(channelGroupIndex).size - 1
            }
        }
        return arrayOf(channelGroupIndex, liveChannelIndex)
    }

    private val isCurrentLiveChannelValid: Boolean
        get() {
            if (currentLiveChannelItem == null) {
                Toast.makeText(App.getInstance(), "请先选择频道", Toast.LENGTH_SHORT).show()
                return false
            }
            return true
        }
}
