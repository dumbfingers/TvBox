package com.github.tvbox.osc.bean

import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamAsAttribute
import com.thoughtworks.xstream.annotations.XStreamConverter
import com.thoughtworks.xstream.annotations.XStreamImplicit
import com.thoughtworks.xstream.converters.extended.ToAttributedValueConverter
import java.io.Serializable
import java.util.*

@XStreamAlias("class")
class MovieSort : Serializable {
    @XStreamImplicit(itemFieldName = "ty")
    @JvmField var sortList: List<SortData>? = null

    @XStreamAlias("ty")
    @XStreamConverter(value = ToAttributedValueConverter::class, strings = ["name"])
    class SortData : Serializable, Comparable<SortData> {
        @XStreamAsAttribute
        @JvmField var id: String? = null
        @JvmField var name: String? = null
        @JvmField var sort: Int = -1
        @JvmField var select: Boolean = false
        @JvmField var filters: ArrayList<SortFilter> = ArrayList()
        @JvmField var filterSelect: HashMap<String, String> = HashMap()
        @JvmField var flag: String? = null // 类型

        constructor()
        constructor(id: String?, name: String?) {
            this.id = id
            this.name = name
        }

        fun filterSelectCount(): Int {
            if (filterSelect == null) {
                return 0
            }
            var count = 0
            for (filter in filterSelect.values) {
                if (filter != null) {
                    count++
                }
            }
            return count
        }

        override fun compareTo(other: SortData): Int {
            return this.sort - other.sort
        }
    }

    class SortFilter {
        @JvmField var key: String? = null
        @JvmField var name: String? = null
        @JvmField var values: LinkedHashMap<String, String>? = null
    }
}
