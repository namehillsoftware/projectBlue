package com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers

import android.view.View
import android.widget.ImageButton
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils.getThemedDrawable
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise.Companion.response

class SyncFilesIsVisibleHandler(
	private val notifyOnFlipViewAnimator: NotifyOnFlipViewAnimator,
	private val syncButton: ImageButton,
	private val storedItemAccess: StoredItemAccess,
	private val libraryId: LibraryId,
	private val item: IItem
) : View.OnLayoutChangeListener {
	override fun onLayoutChange(v: View, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
		if (!v.isShown) return
		storedItemAccess
			.isItemMarkedForSync(libraryId, item)
			.eventually(
				response(
					{ isSynced ->
						if (!v.isShown) return@response

						syncButton.setImageDrawable(
							v.context.getThemedDrawable(
								if (isSynced) R.drawable.ic_sync_on else R.drawable.ic_sync_off
							)
						)

						syncButton.setOnClickListener(
							SyncFilesClickHandler(
								notifyOnFlipViewAnimator,
								libraryId,
								item,
								isSynced
							)
						)
						syncButton.isEnabled = true
					}, v.context
				)
			)
	}
}
