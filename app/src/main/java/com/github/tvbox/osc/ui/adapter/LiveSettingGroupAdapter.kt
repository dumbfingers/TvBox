package com.github.tvbox.osc.ui.adapter

import android.graphics.Color
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.github.tvbox.osc.R
import com.github.tvbox.osc.base.BaseActivity
import com.github.tvbox.osc.bean.LiveSettingGroup

/**
 * @author pj567
 * @date :2021/1/12
 * @description:
 */
class LiveSettingGroupAdapter : BaseQuickAdapter<LiveSettingGroup, BaseViewHolder>(R.layout.item_live_setting_group, ArrayList()) {
    var selectedGroupIndex = -1
        set(value) {
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

    override fun convert(holder: BaseViewHolder, group: LiveSettingGroup) {
        val tvGroupName = holder.getView<TextView>(R.id.tvSettingGroupName)
        tvGroupName.text = group.groupName
        val groupIndex = group.groupIndex
        if (groupIndex == selectedGroupIndex && groupIndex != focusedGroupIndex) {
            tvGroupName.setTextColor((mContext as BaseActivity).themeColor)
        } else {
            tvGroupName.setTextColor(Color.WHITE)
        }
    }
}
