package com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers

import android.view.View
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListActivity.Companion.startItemListActivity
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator

class ViewFilesClickHandler(menuContainer: NotifyOnFlipViewAnimator, private val item: IItem) : AbstractMenuClickHandler(menuContainer) {
    override fun onClick(v: View) {
        v.context.startItemListActivity(item)
        super.onClick(v)
    }
}
