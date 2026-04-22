package com.github.tvbox.osc.bean

import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import com.github.tvbox.osc.base.App
import java.util.*

class AppInfo(name: String, pack: String, icon: Drawable) {
    @JvmField var name: String = name
    @JvmField var icon: Drawable = icon
    @JvmField var pack: String = pack

    fun getName(): String = name
    fun getPack(): String = pack
    fun getIcon(): Drawable = icon

    companion object {
        @JvmStatic
        fun get(info: ApplicationInfo): AppInfo {
            val pm = App.getInstance().packageManager
            val icon = info.loadIcon(pm)
            val name = info.loadLabel(pm).toString()
            val pack = info.packageName
            return AppInfo(name, pack, icon)
        }
    }

    class Sorter : Comparator<AppInfo> {
        override fun compare(info1: AppInfo, info12: AppInfo): Int {
            return info1.name.lowercase(Locale.getDefault()).compareTo(info12.name.lowercase(Locale.getDefault()))
        }

        companion object {
            @JvmStatic
            fun sort(items: MutableList<AppInfo>) {
                Collections.sort(items, Sorter())
            }
        }
    }
}
