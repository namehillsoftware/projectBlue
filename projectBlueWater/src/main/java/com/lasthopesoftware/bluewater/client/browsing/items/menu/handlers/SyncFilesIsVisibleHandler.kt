package com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers

import android.view.View
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.AccessStoredItems
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise.Companion.response

class SyncFilesIsVisibleHandler(
	private val notifyOnFlipViewAnimator: NotifyOnFlipViewAnimator,
	private val syncButton: ImageButton,
	private val storedItemAccess: AccessStoredItems,
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

						with (syncButton) {
							alpha = if (isSynced) .9f else .6f
							setColorFilter(
								ContextCompat.getColor(
									context,
									if (isSynced) R.color.project_blue_primary else R.color.gray_clickable
								),
								android.graphics.PorterDuff.Mode.SRC_IN
							)

							setOnClickListener(
								SyncFilesClickHandler(
									notifyOnFlipViewAnimator,
									libraryId,
									item,
									isSynced
								)
							)

							isEnabled = true
						}
					}, v.context
				)
			)
	}
}
