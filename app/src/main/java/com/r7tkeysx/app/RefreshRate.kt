package com.r7tkeysx.app

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.Window

// Xin màn hình chạy ở tần số quét CAO NHẤT mà máy hỗ trợ (60 / 90 / 120 / 144 Hz...), giữ nguyên độ phân giải hiện tại
// để không bị chớp đen khi đổi chế độ. Máy chỉ có 60Hz thì không đổi gì.
object RefreshRate {
    fun requestMax(context: Context, window: Window) {
        val display = context.getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY) ?: return
        val current = display.mode
        val best = display.supportedModes
            .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
            .maxByOrNull { it.refreshRate } ?: return
        val attrs = window.attributes
        if (attrs.preferredDisplayModeId == best.modeId && attrs.preferredRefreshRate == best.refreshRate) return
        attrs.preferredDisplayModeId = best.modeId
        attrs.preferredRefreshRate = best.refreshRate
        window.attributes = attrs
    }
}
