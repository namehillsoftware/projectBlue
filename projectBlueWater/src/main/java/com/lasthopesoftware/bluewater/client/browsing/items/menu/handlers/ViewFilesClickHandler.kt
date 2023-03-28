package com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers

import android.view.View
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.startItemBrowserActivity

class ViewFilesClickHandler(menuContainer: NotifyOnFlipViewAnimator, private val libraryId: LibraryId, private val item: IItem) : AbstractMenuClickHandler(menuContainer) {
    override fun onClick(v: View) {
        v.context.startItemBrowserActivity(libraryId, item)
        super.onClick(v)
    }
}
