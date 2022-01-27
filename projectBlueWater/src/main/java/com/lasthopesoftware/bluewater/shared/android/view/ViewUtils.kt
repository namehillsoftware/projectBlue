package com.lasthopesoftware.bluewater.shared.android.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.IntDef
import androidx.core.app.NavUtils
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsActivity

object ViewUtils {
    fun Activity.buildStandardMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_blue_water, menu)
        return true
    }

    fun handleMenuClicks(context: Context, item: MenuItem): Boolean {
        if (item.itemId != R.id.menu_connection_settings) return false
        context.startActivity(Intent(context, ApplicationSettingsActivity::class.java))
        return true
    }

    fun handleNavMenuClicks(activity: Activity, item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                val upIntent = NavUtils.getParentActivityIntent(activity)
                if (NavUtils.shouldUpRecreateTask(activity, upIntent!!)) {
                    // This activity is NOT part of this app's task, so create a new task
                    // when navigating up, with a synthesized back stack.
                    TaskStackBuilder.create(activity) // Add all of this activity's parents to the back stack
                        .addNextIntentWithParentStack(upIntent) // Navigate up to the closest parent
                        .startActivities()
                } else {
                    // This activity is part of this app's task, so simply
                    // navigate up to the logical parent activity.
                    NavUtils.navigateUpTo(activity, upIntent)
                }
                return true
            }
        }
        return handleMenuClicks(activity, item)
    }

    @Visibility
    fun getVisibility(isVisible: Boolean): Int {
        return if (isVisible) View.VISIBLE else View.INVISIBLE
    }

	@JvmStatic
	fun dpToPx(context: Context, dp: Int): Int {
        val densityDpi = context.resources.displayMetrics.density
        return (dp * densityDpi + .5f).toInt()
    }

	fun Context.getThemedDrawable(id: Int): Drawable? {
        return ContextCompat.getDrawable(this, id)
    }

    fun getActiveListItemTextViewStyle(isActive: Boolean): Int {
        return if (isActive) Typeface.BOLD else Typeface.NORMAL
    }

    @IntDef(View.VISIBLE, View.INVISIBLE, View.GONE)
	@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
	annotation class Visibility
}
