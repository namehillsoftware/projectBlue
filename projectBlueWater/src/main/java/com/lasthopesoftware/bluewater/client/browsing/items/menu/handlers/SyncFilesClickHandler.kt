package com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers

import android.view.View
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.scheduling.SyncWorker

/**
 * Created by david on 7/18/15.
 */
class SyncFilesClickHandler internal constructor(
    menuContainer: NotifyOnFlipViewAnimator,
    private val libraryId: LibraryId,
    private val item: IItem,
    private var isSynced: Boolean
) : AbstractMenuClickHandler(menuContainer) {
    private val syncListManager = StoredItemAccess(menuContainer.context)

	override fun onClick(v: View) {
        isSynced = !isSynced
        syncListManager.toggleSync(libraryId, item, isSynced)
        SyncWorker.syncImmediately(v.context)
        super.onClick(v)
    }
}
