package com.github.tvbox.osc.ui.adapter

import android.graphics.Color
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.github.tvbox.osc.R
import com.github.tvbox.osc.base.BaseActivity
import com.github.tvbox.osc.bean.LiveChannelItem

/**
 * @author pj567
 * @date :2021/1/12
 * @description:
 */
class LiveChannelItemAdapter : BaseQuickAdapter<LiveChannelItem, BaseViewHolder>(R.layout.item_live_channel, ArrayList()) {
    var selectedChannelIndex = -1
        set(value) {
            if (value == field) return
            val preSelectedChannelIndex = field
            field = value
            if (preSelectedChannelIndex != -1) notifyItemChanged(preSelectedChannelIndex)
            if (field != -1) notifyItemChanged(field)
        }
    var focusedChannelIndex = -1
        set(value) {
            val preFocusedChannelIndex = field
            field = value
            if (preFocusedChannelIndex != -1) notifyItemChanged(preFocusedChannelIndex)
            if (field != -1) notifyItemChanged(field) else if (selectedChannelIndex != -1) notifyItemChanged(selectedChannelIndex)
        }

    override fun convert(holder: BaseViewHolder, item: LiveChannelItem) {
        val tvChannelNum = holder.getView<TextView>(R.id.tvChannelNum)
        val tvChannel = holder.getView<TextView>(R.id.tvChannelName)
        tvChannelNum.text = String.format("%s", item.channelNum)
        tvChannel.text = item.channelName
        val channelIndex = item.channelIndex
        if (channelIndex == selectedChannelIndex && channelIndex != focusedChannelIndex) {
            tvChannelNum.setTextColor((mContext as BaseActivity).themeColor)
            tvChannel.setTextColor((mContext as BaseActivity).themeColor)
        } else {
            tvChannelNum.setTextColor(Color.WHITE)
            tvChannel.setTextColor(Color.WHITE)
        }
    }
}
