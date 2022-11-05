package com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers

import android.view.View
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator
import com.lasthopesoftware.bluewater.client.browsing.items.startItemBrowserActivity

class ViewFilesClickHandler(menuContainer: NotifyOnFlipViewAnimator, private val item: IItem) : AbstractMenuClickHandler(menuContainer) {
    override fun onClick(v: View) {
        v.context.startItemBrowserActivity(item)
        super.onClick(v)
    }
}
