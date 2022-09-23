package com.lasthopesoftware.bluewater.client.browsing.files.menu

import android.graphics.Typeface
import android.os.Handler
import android.view.View
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.FilePlayClickListener
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.ViewFileDetailsClickListener
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertiesUpdatedMessage
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener.Companion.tryFlipToPreviousView
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.AbstractMenuClickHandler
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideScopedUrlKeyProvider
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.ProvideNowPlayingFiles
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils
import com.lasthopesoftware.bluewater.shared.android.view.getValue
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.namehillsoftware.handoff.promises.Promise

class FileListItemMenuBuilder(
	private val serviceFiles: Collection<ServiceFile>,
	private val nowPlayingFileProvider: ProvideNowPlayingFiles,
	private val registerForApplicationMessages: RegisterForApplicationMessages,
	private val scopedUrlKeyProvider: ProvideScopedUrlKeyProvider
)
	: AbstractFileListItemMenuBuilder<FileListItemMenuBuilder.ViewHolder>(R.layout.layout_file_item_menu) {

	override fun newViewHolder(fileItemMenu: FileListItemContainer): ViewHolder {
		return ViewHolder(fileItemMenu)
	}

	inner class ViewHolder(private val fileListItemContainer: FileListItemContainer) : RecyclerView.ViewHolder(fileListItemContainer.viewAnimator) {
		private val handler by lazy { Handler(itemView.context.mainLooper) }
		private val viewFileDetailsButton by LazyViewFinder<ImageButton>(itemView, R.id.btnViewFileDetails)
		private val playButton by LazyViewFinder<ImageButton>(itemView, R.id.btnPlaySong)
		private val addButton by LazyViewFinder<ImageButton>(itemView, R.id.btnAddToPlaylist)
		private val fileNameTextViewSetter = FileNameTextViewSetter(fileListItemContainer.textView)

		private var promisedTextViewUpdate: Promise<*>? = null

		private var positionedFile: PositionedFile? = null
		private var urlKey: UrlKeyHolder<ServiceFile>? = null

		init {
			registerForApplicationMessages.registerReceiver(handler) { message: PlaybackMessage.TrackChanged ->
				val position = positionedFile?.playlistPosition
				fileListItemContainer.textView.setTypeface(
					null,
					ViewUtils.getActiveListItemTextViewStyle(position == message.positionedFile.playlistPosition)
				)
			}

			registerForApplicationMessages.registerReceiver { message: FilePropertiesUpdatedMessage ->
				if (urlKey == message.urlServiceKey) positionedFile?.serviceFile?.also {
					fileNameTextViewSetter.promiseTextViewUpdate(it)
				}
			}
		}

		fun update(positionedFile: PositionedFile) {
			this.positionedFile = positionedFile
			val serviceFile = positionedFile.serviceFile

			promisedTextViewUpdate = fileNameTextViewSetter.promiseTextViewUpdate(serviceFile)
			val textView = fileListItemContainer.textView
			textView.setTypeface(null, Typeface.NORMAL)
			nowPlayingFileProvider.nowPlayingFile
				.eventually(LoopedInPromise.response({ f ->
					textView.setTypeface(null, ViewUtils.getActiveListItemTextViewStyle(serviceFile == f))
				}, handler))

			scopedUrlKeyProvider
				.promiseUrlKey(serviceFile)
				.then { urlKey = it	}

			val viewAnimator = fileListItemContainer.viewAnimator
			viewAnimator.tryFlipToPreviousView()
			playButton.setOnClickListener(FilePlayClickListener(viewAnimator, positionedFile.playlistPosition, serviceFiles))
			viewFileDetailsButton.setOnClickListener(ViewFileDetailsClickListener(viewAnimator, positionedFile, serviceFiles))
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
