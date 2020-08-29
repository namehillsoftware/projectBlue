package com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.FilePlayClickListener
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.details.ViewFileDetailsClickListener
import com.lasthopesoftware.bluewater.client.browsing.items.menu.AbstractListItemViewChangedListener
import com.lasthopesoftware.bluewater.client.browsing.items.menu.BuildListItemMenuViewContainers
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.AbstractMenuClickHandler
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.INowPlayingFileProvider
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.namehillsoftware.handoff.promises.Promise

class FileListItemMenuBuilder(private val serviceFiles: Collection<ServiceFile>, private val nowPlayingFileProvider: INowPlayingFileProvider)
	: AbstractListItemViewChangedListener(), BuildListItemMenuViewContainers<FileListItemMenuBuilder.ViewHolder> {

	override fun buildView(parent: ViewGroup): ViewHolder {
		val fileItemMenu = FileListItemContainer(parent.context)
		val notifyOnFlipViewAnimator = fileItemMenu.viewAnimator

		val inflater = parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
		val fileMenu = inflater.inflate(R.layout.layout_file_item_menu, parent, false) as LinearLayout

		notifyOnFlipViewAnimator.addView(fileMenu)
		notifyOnFlipViewAnimator.setViewChangedListener(getOnViewChangedListener())
		notifyOnFlipViewAnimator.setOnLongClickListener(LongClickViewAnimatorListener())

		return ViewHolder(fileItemMenu)
	}

	inner class ViewHolder(private val fileListItemContainer: FileListItemContainer) : RecyclerView.ViewHolder(fileListItemContainer.viewAnimator) {
		private val viewFileDetailsButtonFinder = LazyViewFinder<ImageButton>(itemView, R.id.btnViewFileDetails)
		private val playButtonFinder = LazyViewFinder<ImageButton>(itemView, R.id.btnPlaySong)
		private val addButtonFinder = LazyViewFinder<ImageButton>(itemView, R.id.btnAddToPlaylist)
		private val fileNameTextViewSetter = FileNameTextViewSetter(fileListItemContainer.findTextView())

		private var fileListItemNowPlayingHandler: AbstractFileListItemNowPlayingHandler? = null
		private var promisedTextViewUpdate: Promise<*>? = null

		fun update(positionedFile: PositionedFile) {
			val serviceFile = positionedFile.serviceFile

			promisedTextViewUpdate = fileNameTextViewSetter.promiseTextViewUpdate(serviceFile)
			val textView = fileListItemContainer.findTextView()
			textView.setTypeface(null, Typeface.NORMAL)
			nowPlayingFileProvider.nowPlayingFile
				.eventually(LoopedInPromise.response<ServiceFile, Any?>({ f: ServiceFile ->
					textView.setTypeface(null, ViewUtils.getActiveListItemTextViewStyle(serviceFile == f))
					null
				}, textView.context))

			fileListItemNowPlayingHandler?.release()
			fileListItemNowPlayingHandler = object : AbstractFileListItemNowPlayingHandler(fileListItemContainer) {
				override fun onReceive(context: Context, intent: Intent) {
					val fileKey = intent.getIntExtra(PlaylistEvents.PlaybackFileParameters.fileKey, -1)
					textView.setTypeface(null, ViewUtils.getActiveListItemTextViewStyle(serviceFile.key == fileKey))
				}
			}

			val viewAnimator = fileListItemContainer.viewAnimator
			LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator)
			playButtonFinder.findView().setOnClickListener(FilePlayClickListener(viewAnimator, positionedFile.playlistPosition, serviceFiles))
			viewFileDetailsButtonFinder.findView().setOnClickListener(ViewFileDetailsClickListener(viewAnimator, serviceFile))
			addButtonFinder.findView().setOnClickListener(AddClickListener(viewAnimator, serviceFile))
		}
	}

	private class AddClickListener(viewFlipper: NotifyOnFlipViewAnimator, private val mServiceFile: ServiceFile) : AbstractMenuClickHandler(viewFlipper) {
		override fun onClick(v: View) {
			PlaybackService.addFileToPlaylist(v.context, mServiceFile.key)
			super.onClick(v)
		}
	}
}
