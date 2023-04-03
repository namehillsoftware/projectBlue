package com.lasthopesoftware.bluewater.client.browsing.files.details

import android.view.View
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsActivity.Companion.launchFileDetailsActivity
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.AbstractMenuClickHandler
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile

class ViewFileDetailsClickListener(
	viewFlipper: NotifyOnFlipViewAnimator,
	private val libraryId: LibraryId,
	private val positionedFile: PositionedFile,
	private val playlist: Collection<ServiceFile>,
) : AbstractMenuClickHandler(viewFlipper)
{
    override fun onClick(v: View) {
		v.context.launchFileDetailsActivity(libraryId, playlist, positionedFile.playlistPosition)
        super.onClick(v)
    }
}
