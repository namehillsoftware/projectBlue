package com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.menu

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.RecyclerMenuViewHolder
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.details.ViewFileDetailsClickListener
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.AbstractFileListItemNowPlayingHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileListItemContainer
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileNameTextViewSetter
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.menu.listeners.FileSeekToClickListener
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.menu.listeners.RemovePlaylistFileClickListener
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.storage.INowPlayingRepository
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener
import com.lasthopesoftware.bluewater.client.browsing.items.menu.OnViewChangedListener
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.vedsoft.futures.runnables.OneParameterAction


class NowPlayingFileListItemMenuBuilder(
	private val nowPlayingRepository: INowPlayingRepository) {
	private var onPlaylistFileRemovedListener: OneParameterAction<Int>? = null
	private var onViewChangedListener: OnViewChangedListener? = null

	class ViewHolder internal constructor(
		val fileListItemContainer: FileListItemContainer,
		val fileNameTextViewSetter: FileNameTextViewSetter,
		viewFileDetailsButtonFinder: LazyViewFinder<ImageButton>,
		playButtonFinder: LazyViewFinder<ImageButton>,
		private val removeButtonFinder: LazyViewFinder<ImageButton>)
		: RecyclerMenuViewHolder(fileListItemContainer.viewAnimator, viewFileDetailsButtonFinder, playButtonFinder) {
		var fileListItemNowPlayingHandler: AbstractFileListItemNowPlayingHandler? = null

		val removeButton: ImageButton
			get() = removeButtonFinder.findView()
	}

	fun setOnViewChangedListener(onViewChangedListener: OnViewChangedListener) {
		this.onViewChangedListener = onViewChangedListener
	}

	fun newViewHolder(parent: ViewGroup): ViewHolder {
		val fileItemMenu = FileListItemContainer(parent.context)
		val viewFlipper = fileItemMenu.viewAnimator

		val inflater = parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
		val fileMenu = inflater.inflate(R.layout.layout_now_playing_file_item_menu, parent, false) as LinearLayout

		viewFlipper.addView(fileMenu)
		viewFlipper.setViewChangedListener(onViewChangedListener)

		return ViewHolder(
			fileItemMenu,
			FileNameTextViewSetter(fileItemMenu.findTextView()),
			LazyViewFinder(fileMenu, R.id.btnViewFileDetails),
			LazyViewFinder(fileMenu, R.id.btnPlaySong),
			LazyViewFinder(fileMenu, R.id.btnRemoveFromPlaylist))
	}

	fun setupView(viewHolder: ViewHolder, positionedFile: PositionedFile) {
		val fileListItem = viewHolder.fileListItemContainer
		val textView = fileListItem.findTextView()

		val serviceFile = positionedFile.serviceFile

		viewHolder.fileNameTextViewSetter.promiseTextViewUpdate(serviceFile)

		val position = positionedFile.playlistPosition

		nowPlayingRepository
			.nowPlaying
			.eventually<Unit>(LoopedInPromise.response({ np ->
				textView.setTypeface(null, ViewUtils.getActiveListItemTextViewStyle(position == np.playlistPosition))
			}, textView.context))

		viewHolder.fileListItemNowPlayingHandler?.release()
		viewHolder.fileListItemNowPlayingHandler = object : AbstractFileListItemNowPlayingHandler(fileListItem) {
			override fun onReceive(context: Context, intent: Intent) {
				val playlistPosition = intent.getIntExtra(PlaylistEvents.PlaylistParameters.playlistPosition, -1)
				textView.setTypeface(null, ViewUtils.getActiveListItemTextViewStyle(position == playlistPosition))
			}
		}


		val viewFlipper = fileListItem.viewAnimator
		viewFlipper.setOnLongClickListener(LongClickViewAnimatorListener())

		LongClickViewAnimatorListener.tryFlipToPreviousView(viewFlipper)
		viewHolder.playButton.setOnClickListener(FileSeekToClickListener(viewFlipper, position))
		viewHolder.viewFileDetailsButton.setOnClickListener(ViewFileDetailsClickListener(viewFlipper, serviceFile))
		viewHolder.removeButton.setOnClickListener(RemovePlaylistFileClickListener(viewFlipper, position, onPlaylistFileRemovedListener))
	}
}
