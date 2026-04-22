package com.github.tvbox.osc.bean

import com.github.tvbox.osc.util.RegexUtils.getPattern
import com.google.gson.Gson
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap

class VodInfo : Serializable, Cloneable {
    @JvmField var last: String? = null //时间
    @JvmField var id: String? = null //内容id
    @JvmField var tid: Int = 0 //父级id
    @JvmField var name: String? = null //影片名称
    @JvmField var type: String? = null //类型名称
    @JvmField var dt: String? = null //视频分类
    @JvmField var pic: String? = null //图片
    @JvmField var lang: String? = null //语言
    @JvmField var area: String? = null //地区
    @JvmField var year: Int = 0 //年份
    @JvmField var state: String? = null
    @JvmField var note: String? = null //描述集数或者影片信息
    @JvmField var actor: String? = null //演员
    @JvmField var director: String? = null //导演
    @JvmField var seriesFlags: ArrayList<VodSeriesFlag>? = null
    @JvmField var seriesMap: LinkedHashMap<String, List<VodSeries>>? = null
    @JvmField var des: String? = null //描述
    @JvmField var playFlag: String? = null
    @JvmField var playIndex: Int = 0
    @JvmField var playGroup: Int = 0
    @JvmField var playGroupCount: Int = 0
    @JvmField var playNote: String = ""
    @JvmField var sourceKey: String? = null
    @JvmField var playerCfg: String = ""
    @JvmField var reverseSort: Boolean = false

    fun setVideo(video: Movie.Video) {
        last = video.last
        id = video.id
        tid = video.tid
        name = video.name
        type = video.type
        pic = video.pic
        lang = video.lang
        area = video.area
        year = video.year
        state = video.state
        note = video.note
        actor = video.actor
        director = video.director
        des = video.des
        if (video.urlBean != null && video.urlBean.infoList != null && video.urlBean.infoList.size > 0) {
            val tempSeriesMap = LinkedHashMap<String, List<VodSeries>>()
            seriesFlags = ArrayList()
            for (urlInfo in video.urlBean.infoList) {
                if (urlInfo.beanList != null && urlInfo.beanList.size > 0) {
                    val seriesList = ArrayList<VodSeries>()
                    for (infoBean in urlInfo.beanList) {
                        seriesList.add(VodSeries(infoBean.name, infoBean.url))
                    }
                    if (urlInfo.flag != null) {
                        tempSeriesMap[urlInfo.flag] = seriesList
                        seriesFlags!!.add(VodSeriesFlag(urlInfo.flag))
                    }
                }
            }
            seriesMap = LinkedHashMap()
            for (flag in seriesFlags!!) {
                val list = tempSeriesMap[flag.name]
                if (list != null && flag.name != null) {
                    if (seriesFlags!!.size <= 3) {
                        if (isReverse(list)) Collections.reverse(list)
                    }
                    seriesMap!![flag.name!!] = list
                }
            }
        }
    }

    private fun extractNumber(name: String): Int {
        val matcher = getPattern("\\d+").matcher(name)
        return if (matcher.find()) {
            matcher.group().toInt()
        } else 0
    }

    private fun isReverse(list: List<VodSeries>): Boolean {
        if (list.size > 300) {
            return false
        }
        var ascCount = 0
        var descCount = 0
        val limit = Math.min(list.size - 1, 6)
        for (i in 0 until limit) {
            val current = extractNumber(list[i].name ?: "")
            val next = extractNumber(list[i + 1].name ?: "")
            if (current < next) {
                ascCount++
                if (ascCount == 2) return false
            } else if (current > next) {
                descCount++
                if (descCount == 2) return true
            }
        }
        return false
    }

    fun reverse() {
        val flags = seriesMap?.keys ?: return
        for (flag in flags) {
            Collections.reverse(seriesMap!![flag])
        }
    }

    fun getplayIndex(): Int {
        return playGroup * playGroupCount + playIndex
    }

    class VodSeriesFlag : Serializable {
        @JvmField var name: String? = null
        @JvmField var selected: Boolean = false

        constructor()
        constructor(name: String?) {
            this.name = name
        }
    }

    class VodSeries : Serializable {
        @JvmField var name: String? = null
        @JvmField var url: String? = null
        @JvmField var selected: Boolean = false

        constructor()
        constructor(name: String?, url: String?) {
            this.name = name
            this.url = url
        }
    }

    public override fun clone(): Any {
        return try {
            val gson = Gson()
            val json = gson.toJson(this)
            gson.fromJson(json, VodInfo::class.java)
        } catch (ignored: Exception) {
            this
        }
    }

    fun isSeriesEmpty(): Boolean {
        return seriesMap?.isEmpty() ?: true
    }

    fun getFlagSeries(playFlag: String?): List<VodSeries>? {
        return if (!isSeriesEmpty() && playFlag != null) {
            seriesMap?.get(playFlag)
        } else {
            ArrayList()
        }
    }

    fun isFlagSeriesEmpty(playFlag: String?): Boolean {
        val list = getFlagSeries(playFlag)
        return list?.isEmpty() ?: true
    }

    fun getVodSeries(playFlag: String?, playIndex: Int): VodSeries? {
        var vodSeries: VodSeries? = null
        val list = getFlagSeries(playFlag)
        if (list != null && list.isNotEmpty()) {
            if (playIndex >= 0 && playIndex < list.size) {
                vodSeries = list[playIndex]
            }
        }
        return vodSeries
    }
}
