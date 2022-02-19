package com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers

import android.view.View
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.ItemStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.access.LaunchPlaybackFromResult
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.access.OnGetFileStringListForClickErrorListener
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise.Companion.response

class ShuffleClickHandler(
	menuContainer: NotifyOnFlipViewAnimator,
	private val itemStringListProvider: ItemStringListProvider,
	private val libraryId: LibraryId,
	private val item: Item
) : AbstractMenuClickHandler(menuContainer) {
	override fun onClick(v: View) {
		itemStringListProvider
			.promiseFileStringList(libraryId, item, FileListParameters.Options.Shuffled)
			.then(LaunchPlaybackFromResult(v.context))
			.excuse(OnGetFileStringListForClickErrorListener(v, this))
			.eventuallyExcuse(response(UnexpectedExceptionToasterResponse(v.context), v.context))
		super.onClick(v)
	}
}
