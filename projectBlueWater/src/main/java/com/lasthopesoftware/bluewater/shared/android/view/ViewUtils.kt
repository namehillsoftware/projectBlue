package com.lasthopesoftware.bluewater.shared.android.view

import android.content.Context
import android.graphics.Typeface
import android.view.View
import androidx.annotation.IntDef

object ViewUtils {

	@Visibility
    fun getVisibility(isVisible: Boolean): Int {
        return if (isVisible) View.VISIBLE else View.INVISIBLE
    }

	@JvmStatic
	fun dpToPx(context: Context, dp: Int): Int {
        val densityDpi = context.resources.displayMetrics.density
        return (dp * densityDpi + .5f).toInt()
    }

	fun getActiveListItemTextViewStyle(isActive: Boolean): Int {
        return if (isActive) Typeface.BOLD else Typeface.NORMAL
    }

    @IntDef(View.VISIBLE, View.INVISIBLE, View.GONE)
	@Retention(AnnotationRetention.SOURCE)
	annotation class Visibility
}
