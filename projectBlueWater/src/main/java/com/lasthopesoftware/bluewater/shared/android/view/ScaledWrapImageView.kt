package com.lasthopesoftware.bluewater.shared.android.view

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.roundToInt

class ScaledWrapImageView : AppCompatImageView {
    private var isLandscape = false
    private lateinit var bitmap: Bitmap

    constructor(context: Context?) : super(context!!) {
        updateIsLandscape()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    ) {
        updateIsLandscape()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!, attrs, defStyleAttr
    ) {
        updateIsLandscape()
    }

    override fun setImageBitmap(bm: Bitmap) {
        bitmap = bm
        super.setImageBitmap(bm)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        updateIsLandscape(newConfig)
        super.onConfigurationChanged(newConfig)
    }

    private fun updateIsLandscape(configuration: Configuration = resources.configuration) {
        isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (!::bitmap.isInitialized) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        scaleType = ScaleType.FIT_XY
        var width = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        var height = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        if (isLandscape) {
            val newHeight = scaleInteger(
                bitmap.height, width.toDouble() / bitmap.width.toDouble()
            )
            if (newHeight > height) {
                width = scaleInteger(width, height.toDouble() / newHeight.toDouble())
            } else {
                height = newHeight
            }
        } else {
            val newWidth = scaleInteger(
                bitmap.width, height.toDouble() / bitmap.height.toDouble()
            )
            if (newWidth > width) {
                height = scaleInteger(height, width.toDouble() / newWidth.toDouble())
            } else {
                width = newWidth
            }
        }
        setMeasuredDimension(width, height)
    }

    companion object {
        private fun scaleInteger(srcInt: Int, scaleRatio: Double): Int {
            return (srcInt.toDouble() * scaleRatio).roundToInt()
        }
    }
}
