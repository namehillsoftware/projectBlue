package com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers

import android.view.View
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.list.FileListActivity.Companion.startFileListActivity
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator

class ViewFilesClickHandler(menuContainer: NotifyOnFlipViewAnimator, private val item: IItem) : AbstractMenuClickHandler(menuContainer) {
    override fun onClick(v: View) {
        startFileListActivity(v.context, item)
        super.onClick(v)
    }
}
