package com.github.tvbox.osc.ui.adapter

import android.graphics.Color
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.github.tvbox.osc.R
import com.github.tvbox.osc.base.BaseActivity
import com.github.tvbox.osc.bean.LiveSettingItem

/**
 * @author pj567
 * @date :2021/1/12
 * @description:
 */
class LiveSettingItemAdapter : BaseQuickAdapter<LiveSettingItem, BaseViewHolder>(R.layout.item_live_setting, ArrayList()) {
    var focusedItemIndex = -1
        set(value) {
            val preFocusItemIndex = field
            field = value
            if (preFocusItemIndex != -1) notifyItemChanged(preFocusItemIndex)
            if (field != -1) notifyItemChanged(field)
        }

    override fun convert(holder: BaseViewHolder, item: LiveSettingItem) {
        val tvItemName = holder.getView<TextView>(R.id.tvSettingItemName)
        tvItemName.text = item.itemName
        val itemIndex = item.itemIndex
        if (item.isItemSelected && itemIndex != focusedItemIndex) {
            tvItemName.setTextColor((mContext as BaseActivity).themeColor)
        } else {
            tvItemName.setTextColor(Color.WHITE)
        }
    }

    fun selectItem(selectedItemIndex: Int, select: Boolean, unselectPreItemIndex: Boolean) {
        if (unselectPreItemIndex) {
            val preSelectedItemIndex = selectedItemIndexInternal
            if (preSelectedItemIndex != -1) {
                data[preSelectedItemIndex].isItemSelected = false
                notifyItemChanged(preSelectedItemIndex)
            }
        }
        if (selectedItemIndex != -1) {
            if (data.size <= selectedItemIndex) return
            data[selectedItemIndex].isItemSelected = select
            notifyItemChanged(selectedItemIndex)
        }
    }

    val selectedItemIndexInternal: Int
        get() {
            for (item in data) {
                if (item.isItemSelected) return item.itemIndex
            }
            return -1
        }

    // Compatibility for Java
    fun getSelectedItemIndex(): Int = selectedItemIndexInternal
}
