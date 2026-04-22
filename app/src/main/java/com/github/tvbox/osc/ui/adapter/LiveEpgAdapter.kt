package com.github.tvbox.osc.ui.adapter

import android.view.View
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.github.tvbox.osc.R
import com.github.tvbox.osc.base.BaseActivity
import com.github.tvbox.osc.bean.Epginfo
import com.github.tvbox.osc.ui.tv.widget.AudioWaveView
import java.text.SimpleDateFormat
import java.util.*

class LiveEpgAdapter : BaseQuickAdapter<Epginfo, BaseViewHolder>(R.layout.item_epglist, ArrayList()) {
    var selectedEpgIndex = -1
        set(value) {
            if (value == field) return
            field = value
            if (field != -1) notifyItemChanged(field)
        }
    var focusedEpgIndex = -1
        set(value) {
            field = value
            if (field != -1) notifyItemChanged(field)
        }
    private var shiyiSelection = false
    private var sourceIncludeBack = false
    private var shiyiDate: String? = null
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun CanBack(sourceIncludeBack: Boolean) {
        this.sourceIncludeBack = sourceIncludeBack
    }

    override fun convert(holder: BaseViewHolder, value: Epginfo) {
        val now = Date()
        val textview = holder.getView<TextView>(R.id.tv_epg_name)
        val timeview = holder.getView<TextView>(R.id.tv_epg_time)
        val shiyi = holder.getView<TextView>(R.id.shiyi)
        val audioWaveView = holder.getView<AudioWaveView>(R.id.wqddg_AudioWaveView)
        audioWaveView.visibility = View.GONE

        if (value.index == selectedEpgIndex && value.index != focusedEpgIndex && (value.currentEpgDate == shiyiDate || value.currentEpgDate == timeFormat.format(now))) {
            textview.setTextColor((mContext as BaseActivity).themeColor)
            timeview.setTextColor((mContext as BaseActivity).themeColor)
        } else {
            textview.setTextColor(mContext.resources.getColor(R.color.color_FFFFFF))
            timeview.setTextColor(mContext.resources.getColor(R.color.color_FFFFFF))
        }

        if (now.compareTo(value.startdateTime) >= 0 && now.compareTo(value.enddateTime) <= 0) {
            shiyi.visibility = View.VISIBLE
            shiyi.setBackgroundColor(mContext.resources.getColor(R.color.color_32364E))
            shiyi.setTextColor(mContext.resources.getColor(R.color.color_FFFFFF))
            shiyi.text = "直播"
        } else if (now.compareTo(value.enddateTime) > 0 && sourceIncludeBack) {
            shiyi.visibility = View.VISIBLE
            shiyi.setBackgroundColor(mContext.resources.getColor(R.color.color_32364E_40))
            shiyi.setTextColor(mContext.resources.getColor(R.color.color_FFFFFF))
            shiyi.text = "回看"
        } else if (now.compareTo(value.startdateTime) < 0 && sourceIncludeBack) {
            shiyi.visibility = View.VISIBLE
            shiyi.setBackgroundColor(mContext.resources.getColor(R.color.color_3D3D3D))
            shiyi.setTextColor(mContext.resources.getColor(R.color.color_FFFFFF))
            shiyi.text = "预约"
        } else {
            shiyi.visibility = View.GONE
        }

        textview.text = value.title
        timeview.text = "${value.start} - ${value.end}"
        if (shiyiSelection == false) {
            if (now.compareTo(value.startdateTime) >= 0 && now.compareTo(value.enddateTime) <= 0) {
                audioWaveView.visibility = View.VISIBLE
                textview.setTextColor((mContext as BaseActivity).themeColor)
                timeview.setTextColor((mContext as BaseActivity).themeColor)
                textview.freezesText = true
                timeview.freezesText = true
                shiyi.text = "直播中"
            } else {
                audioWaveView.visibility = View.GONE
            }
        } else {
            if (value.index == this.selectedEpgIndex && value.currentEpgDate == shiyiDate) {
                audioWaveView.visibility = View.VISIBLE
                textview.setTextColor((mContext as BaseActivity).themeColor)
                timeview.setTextColor((mContext as BaseActivity).themeColor)
                textview.freezesText = true
                timeview.freezesText = true
                shiyi.text = "回看中"
                if (now.compareTo(value.startdateTime) >= 0 && now.compareTo(value.enddateTime) <= 0) {
                    shiyi.text = "直播中"
                }
                audioWaveView.visibility = View.VISIBLE
            } else {
                audioWaveView.visibility = View.GONE
            }
        }
    }

    fun setShiyiSelection(i: Int, t: Boolean, currentEpgDate: String?) {
        this.selectedEpgIndex = i
        this.shiyiDate = if (t) currentEpgDate else null
        shiyiSelection = t
        notifyItemChanged(this.selectedEpgIndex)
    }
}
