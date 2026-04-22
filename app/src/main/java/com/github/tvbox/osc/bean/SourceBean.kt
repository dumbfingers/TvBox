package com.github.tvbox.osc.bean

import java.util.ArrayList

class SourceBean {
    var key: String? = null
    var name: String? = null
    var api: String? = null
    var type: Int = 0 // 0 xml 1 json 3 Spider
    var searchable: Int = 0 // 是否可搜索
    var quickSearch: Int = 0 // 是否可以快速搜索
    var filterable: Int = 0 // 可筛选?
    var hide: Int = 0 // 设置的选择列表里隐藏
    var playerUrl: String? = null // 站点解析Url
    var ext: String? = null // 扩展数据
    var jar: String? = null // 自定义jar
    var categories: ArrayList<String>? = null // 分类&排序
    var playerType: Int = 0 // 0 system 1 ikj 2 exo 10 mxplayer -1 以参数设置页面的为准
    var clickSelector: String? = null // 需要点击播放的嗅探站点selector   ddrk.me;#id
    var style: String? = null // 展示风格

    fun isSearchable(): Boolean {
        return searchable != 0
    }

    fun isQuickSearch(): Boolean {
        return quickSearch != 0
    }
}
