package com.lasthopesoftware.bluewater.client.playback.view.nowplaying.menu

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.details.ViewFileDetailsClickListener
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.AbstractFileListItemNowPlayingHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileListItemContainer
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileNameTextViewSetter
import com.lasthopesoftware.bluewater.client.browsing.items.menu.AbstractListItemViewChangedListener
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.menu.listeners.FileSeekToClickListener
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.menu.listeners.RemovePlaylistFileClickListener
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.INowPlayingRepository
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise


class NowPlayingFileListItemMenuBuilder(private val nowPlayingRepository: INowPlayingRepository) : AbstractListItemViewChangedListener() {

	fun newViewHolder(parent: ViewGroup): ViewHolder {
		val fileItemMenu = FileListItemContainer(parent.context)
		val viewFlipper = fileItemMenu.viewAnimator

		val inflater = parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
		val fileMenu = inflater.inflate(R.layout.layout_now_playing_file_item_menu, parent, false) as LinearLayout

		viewFlipper.addView(fileMenu)
		viewFlipper.setViewChangedListener(getOnViewChangedListener())
		viewFlipper.setOnLongClickListener(LongClickViewAnimatorListener())

		return ViewHolder(fileItemMenu)
	}

	inner class ViewHolder internal constructor(private val fileListItemContainer: FileListItemContainer)
		: RecyclerView.ViewHolder(fileListItemContainer.viewAnimator) {

		private val viewFileDetailsButtonFinder = LazyViewFinder<ImageButton>(itemView, R.id.btnViewFileDetails)
		private val playButtonFinder = LazyViewFinder<ImageButton>(itemView, R.id.btnPlaySong)
		private val removeButtonFinder = LazyViewFinder<ImageButton>(itemView, R.id.btnRemoveFromPlaylist)
		private val fileNameTextViewSetter = FileNameTextViewSetter(fileListItemContainer.findTextView())

		var fileListItemNowPlayingHandler: AbstractFileListItemNowPlayingHandler? = null

		fun update(positionedFile: PositionedFile) {
			val fileListItem = fileListItemContainer
			val textView = fileListItem.findTextView()

			val serviceFile = positionedFile.serviceFile

			fileNameTextViewSetter.promiseTextViewUpdate(serviceFile)

			val position = positionedFile.playlistPosition

			val viewFlipper = fileListItem.viewAnimator

			nowPlayingRepository
				.nowPlaying
				.eventually<Unit>(LoopedInPromise.response({ np ->
					textView.setTypeface(null, ViewUtils.getActiveListItemTextViewStyle(position == np.playlistPosition))
					viewFlipper.isSelected = position == np.playlistPosition
				}, textView.context))

			fileListItemNowPlayingHandler?.release()
			fileListItemNowPlayingHandler = object : AbstractFileListItemNowPlayingHandler(fileListItem) {
				override fun onReceive(context: Context, intent: Intent) {
					val playlistPosition = intent.getIntExtra(PlaylistEvents.PlaylistParameters.playlistPosition, -1)
					textView.setTypeface(null, ViewUtils.getActiveListItemTextViewStyle(position == playlistPosition))
					viewFlipper.isSelected = position == playlistPosition
				}
			}

			LongClickViewAnimatorListener.tryFlipToPreviousView(viewFlipper)
			playButtonFinder.findView().setOnClickListener(FileSeekToClickListener(viewFlipper, position))
			viewFileDetailsButtonFinder.findView().setOnClickListener(ViewFileDetailsClickListener(viewFlipper, serviceFile))
			removeButtonFinder.findView().setOnClickListener(RemovePlaylistFileClickListener(viewFlipper, position))
		}
	}
}
