package com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu

import android.graphics.Typeface
import android.os.Handler
import android.view.View
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.FilePlayClickListener
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.details.ViewFileDetailsClickListener
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.AbstractMenuClickHandler
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.ProvideNowPlayingFiles
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils
import com.lasthopesoftware.bluewater.shared.android.view.getValue
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.namehillsoftware.handoff.promises.Promise

class FileListItemMenuBuilder(
	private val serviceFiles: Collection<ServiceFile>,
	private val nowPlayingFileProvider: ProvideNowPlayingFiles,
	private val fileListItemNowPlayingRegistrar: FileListItemNowPlayingRegistrar
)
	: AbstractFileListItemMenuBuilder<FileListItemMenuBuilder.ViewHolder>(R.layout.layout_file_item_menu) {

	override fun newViewHolder(fileItemMenu: FileListItemContainer) = ViewHolder(fileItemMenu)

	inner class ViewHolder(private val fileListItemContainer: FileListItemContainer) : RecyclerView.ViewHolder(fileListItemContainer.viewAnimator) {
		private val handler by lazy { Handler(itemView.context.mainLooper) }
		private val viewFileDetailsButton by LazyViewFinder<ImageButton>(itemView, R.id.btnViewFileDetails)
		private val playButton by LazyViewFinder<ImageButton>(itemView, R.id.btnPlaySong)
		private val addButton by LazyViewFinder<ImageButton>(itemView, R.id.btnAddToPlaylist)
		private val fileNameTextViewSetter = FileNameTextViewSetter(fileListItemContainer.textView)

		private var fileListItemNowPlayingHandler: AutoCloseable? = null
		private var promisedTextViewUpdate: Promise<*>? = null

		fun update(positionedFile: PositionedFile) {
			val serviceFile = positionedFile.serviceFile

			promisedTextViewUpdate = fileNameTextViewSetter.promiseTextViewUpdate(serviceFile)
			val textView = fileListItemContainer.textView
			textView.setTypeface(null, Typeface.NORMAL)
			nowPlayingFileProvider.nowPlayingFile
				.eventually(LoopedInPromise.response({ f ->
					textView.setTypeface(null, ViewUtils.getActiveListItemTextViewStyle(serviceFile == f))
				}, handler))

			fileListItemNowPlayingHandler?.close()
			fileListItemNowPlayingHandler = fileListItemNowPlayingRegistrar.registerNewHandler {
				textView.setTypeface(null, ViewUtils.getActiveListItemTextViewStyle(serviceFile == it.positionedFile.serviceFile))
			}

			val viewAnimator = fileListItemContainer.viewAnimator
			LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator)
			playButton.setOnClickListener(FilePlayClickListener(viewAnimator, positionedFile.playlistPosition, serviceFiles))
			viewFileDetailsButton.setOnClickListener(ViewFileDetailsClickListener(viewAnimator, serviceFile))
			addButton.setOnClickListener(AddClickListener(viewAnimator, serviceFile))
		}
	}

	private class AddClickListener(viewFlipper: NotifyOnFlipViewAnimator, private val serviceFile: ServiceFile) : AbstractMenuClickHandler(viewFlipper) {
		override fun onClick(v: View) {
			PlaybackService.addFileToPlaylist(v.context, serviceFile.key)
			super.onClick(v)
		}
	}
}
