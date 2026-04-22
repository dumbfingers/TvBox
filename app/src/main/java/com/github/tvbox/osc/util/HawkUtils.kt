package com.github.tvbox.osc.util

import android.content.Context
import androidx.media3.exoplayer.DefaultRenderersFactory
import com.github.tvbox.osc.R
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.base.App
import com.orhanobut.hawk.Hawk
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory

object HawkUtils {
    private const val DANMU_OPEN = "danmu_open"
    private const val DANMU_MAXLINE = "danmu_maxline"
    private const val DANMU_SPEED = "danmu_speed"
    private const val DANMU_ALPHA = "danmu_alpha"
    private const val DANMU_SIZESCALE = "danmu_sizescale"
    private const val DANMU_COLOR = "danmu_color"

    @JvmStatic
    fun getDanmuOpen(): Boolean = Hawk.get(DANMU_OPEN, true)

    @JvmStatic
    fun setDanmuOpen(danmuOpen: Boolean) {
        Hawk.put(DANMU_OPEN, danmuOpen)
    }

    @JvmStatic
    fun getDanmuMaxLine(): Int = Hawk.get(DANMU_MAXLINE, 3)

    @JvmStatic
    fun setDanmuMaxLine(danmuMaxLine: Int) {
        Hawk.put(DANMU_MAXLINE, danmuMaxLine)
    }

    @JvmStatic
    fun getDanmuSpeed(): Float = Hawk.get(DANMU_SPEED, 1.5f)

    @JvmStatic
    fun setDanmuSpeed(danmuSpeed: Float) {
        Hawk.put(DANMU_SPEED, danmuSpeed)
    }

    @JvmStatic
    fun getDanmuAlpha(): Float = Hawk.get(DANMU_ALPHA, 90 / 100.0f)

    @JvmStatic
    fun setDanmuAlpha(floatValue: Float) {
        Hawk.put(DANMU_ALPHA, floatValue)
    }

    @JvmStatic
    fun getDanmuSizeScale(): Float = Hawk.get(DANMU_SIZESCALE, 0.8f)

    @JvmStatic
    fun setDanmuSizeScale(floatValue: Float) {
        Hawk.put(DANMU_SIZESCALE, floatValue)
    }

    @JvmStatic
    fun getDanmuColor(): Boolean = Hawk.get(DANMU_COLOR, false)

    @JvmStatic
    fun setDanmuColor(color: Boolean) {
        Hawk.put(DANMU_COLOR, color)
    }

    @JvmStatic
    fun getIJKCodec(): String = Hawk.get(HawkConfig.IJK_CODEC, "")

    @JvmStatic
    fun nextIJKCodec() {
        val ijkCodes = ApiConfig.get().ijkCodes
        val ijkCodec = getIJKCodec()
        var index = 0
        for (i in ijkCodes.indices) {
            if (ijkCodes[i].name == ijkCodec) {
                index = i
                break
            }
        }
        ijkCodes[index].selected(false)
        index++
        index %= ijkCodes.size
        ijkCodes[index].selected(true)
    }

    @JvmStatic
    fun getIJKCache(): Boolean = Hawk.get(HawkConfig.IJK_CACHE_PLAY, false)

    @JvmStatic
    fun nextIJKCache() {
        Hawk.put(HawkConfig.IJK_CACHE_PLAY, !getIJKCache())
    }

    @JvmStatic
    fun getIJKCacheDesc(): String = if (getIJKCache()) "开启" else "关闭"

    @JvmStatic
    fun getExoRenderer(): Int = Hawk.get(HawkConfig.EXO_RENDERER, 0)

    @JvmStatic
    fun nextExoRenderer() {
        val array = App.getInstance().resources.getStringArray(R.array.media_content_ExoPlayer_renderer)
        var renderer = getExoRenderer()
        renderer++
        renderer %= array.size
        Hawk.put(HawkConfig.EXO_RENDERER, renderer)
    }

    @JvmStatic
    fun createExoRendererActualValue(context: Context): DefaultRenderersFactory {
        return when (getExoRenderer()) {
            1 -> NextRenderersFactory(context)
            else -> DefaultRenderersFactory(context)
        }
    }

    @JvmStatic
    fun getExoRendererDesc(): String {
        val array = App.getInstance().resources.getStringArray(R.array.media_content_ExoPlayer_renderer)
        return array[getExoRenderer()]
    }

    @JvmStatic
    fun getExoRendererMode(): Int = Hawk.get(HawkConfig.EXO_RENDERER_MODE, 1)

    @JvmStatic
    fun nextExoRendererMode() {
        var rendererMode = getExoRendererMode()
        val array = App.getInstance().resources.getStringArray(R.array.media_content_ExoPlayer_renderer_mode)
        rendererMode++
        rendererMode %= array.size
        Hawk.put(HawkConfig.EXO_RENDERER_MODE, rendererMode)
    }

    @JvmStatic
    fun getExoRendererModeActualValue(): Int {
        return when (getExoRendererMode()) {
            0 -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
            2 -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
            else -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
        }
    }

    @JvmStatic
    fun getExoRendererModeDesc(): String {
        val array = App.getInstance().resources.getStringArray(R.array.media_content_ExoPlayer_renderer_mode)
        return array[getExoRendererMode()]
    }

    @JvmStatic
    fun getVodPlayerPreferred(): Int = Hawk.get(HawkConfig.VOD_PLAYER_PREFERRED, 0)

    @JvmStatic
    fun nextVodPlayerPreferred() {
        var index = getVodPlayerPreferred()
        val array = App.getInstance().resources.getStringArray(R.array.media_content_General_VodPlayerPreferred)
        index++
        index %= array.size
        Hawk.put(HawkConfig.VOD_PLAYER_PREFERRED, index)
    }

    @JvmStatic
    fun getVodPlayerPreferredConfigurationFile(): Boolean = getVodPlayerPreferred() == 0

    @JvmStatic
    fun getVodPlayerPreferredDesc(): String {
        val array = App.getInstance().resources.getStringArray(R.array.media_content_General_VodPlayerPreferred)
        return array[getVodPlayerPreferred()]
    }

    @JvmStatic
    fun getLastLiveChannelGroup(): String = Hawk.get(HawkConfig.LIVE_CHANNEL_GROUP, "")

    @JvmStatic
    fun setLastLiveChannelGroup(group: String) {
        Hawk.put(HawkConfig.LIVE_CHANNEL_GROUP, group)
    }

    @JvmStatic
    fun getLastLiveChannel(): String = Hawk.get(HawkConfig.LIVE_CHANNEL, "")

    @JvmStatic
    fun setLastLiveChannel(channel: String) {
        Hawk.put(HawkConfig.LIVE_CHANNEL, channel)
    }
}
