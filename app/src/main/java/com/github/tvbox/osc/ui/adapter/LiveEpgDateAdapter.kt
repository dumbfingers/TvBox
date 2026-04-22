package com.github.tvbox.osc.ui.adapter

import android.graphics.Color
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.github.tvbox.osc.R
import com.github.tvbox.osc.base.BaseActivity
import com.github.tvbox.osc.bean.LiveEpgDate

class LiveEpgDateAdapter : BaseQuickAdapter<LiveEpgDate, BaseViewHolder>(R.layout.item_live_channel_group, ArrayList()) {
    var selectedIndex = -1
        set(value) {
            if (value == field) return
            val preSelectedIndex = field
            field = value
            if (preSelectedIndex != -1) notifyItemChanged(preSelectedIndex)
            if (field != -1) notifyItemChanged(field)
        }
    var focusedIndex = -1
        set(value) {
            val preFocusedIndex = field
            field = value
            if (preFocusedIndex != -1) notifyItemChanged(preFocusedIndex)
            if (field != -1) notifyItemChanged(field) else if (selectedIndex != -1) notifyItemChanged(selectedIndex)
        }

    override fun convert(holder: BaseViewHolder, item: LiveEpgDate) {
        val tvGroupName = holder.getView<TextView>(R.id.tvChannelGroupName)
        tvGroupName.text = item.datePresented
        tvGroupName.setBackgroundColor(Color.TRANSPARENT)
        if (item.index == selectedIndex && item.index != focusedIndex) {
            tvGroupName.setTextColor((mContext as BaseActivity).themeColor)
        } else {
            tvGroupName.setTextColor(mContext.resources.getColor(R.color.color_FFFFFF))
        }
    }
}
