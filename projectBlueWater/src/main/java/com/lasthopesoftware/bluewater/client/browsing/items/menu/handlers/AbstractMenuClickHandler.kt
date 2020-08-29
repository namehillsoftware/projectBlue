package com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers

import android.view.View
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator

/**
 * Created by david on 3/31/15.
 */
abstract class AbstractMenuClickHandler(private val menuContainer: NotifyOnFlipViewAnimator) : View.OnClickListener {
	override fun onClick(v: View) {
		if (menuContainer.displayedChild > 0) menuContainer.showPrevious()
	}
}
