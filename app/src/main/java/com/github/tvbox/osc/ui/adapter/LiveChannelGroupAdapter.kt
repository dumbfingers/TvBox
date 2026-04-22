package com.github.tvbox.osc.ui.adapter

import android.graphics.Color
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.github.tvbox.osc.R
import com.github.tvbox.osc.base.BaseActivity
import com.github.tvbox.osc.bean.LiveChannelGroup

/**
 * @author pj567
 * @date :2021/1/12
 * @description:
 */
class LiveChannelGroupAdapter : BaseQuickAdapter<LiveChannelGroup, BaseViewHolder>(R.layout.item_live_channel_group, ArrayList()) {
    var selectedGroupIndex = -1
        set(value) {
            if (value == field) return
            val preSelectedGroupIndex = field
            field = value
            if (preSelectedGroupIndex != -1) notifyItemChanged(preSelectedGroupIndex)
            if (field != -1) notifyItemChanged(field)
        }
    var focusedGroupIndex = -1
        set(value) {
            field = value
            if (field != -1) notifyItemChanged(field) else if (selectedGroupIndex != -1) notifyItemChanged(selectedGroupIndex)
        }

    override fun convert(holder: BaseViewHolder, item: LiveChannelGroup) {
        val tvGroupName = holder.getView<TextView>(R.id.tvChannelGroupName)
        tvGroupName.text = item.groupName
        val groupIndex = item.groupIndex
        if (groupIndex == selectedGroupIndex && groupIndex != focusedGroupIndex) {
            tvGroupName.setTextColor((mContext as BaseActivity).themeColor)
        } else {
            tvGroupName.setTextColor(Color.WHITE)
        }
    }
}
