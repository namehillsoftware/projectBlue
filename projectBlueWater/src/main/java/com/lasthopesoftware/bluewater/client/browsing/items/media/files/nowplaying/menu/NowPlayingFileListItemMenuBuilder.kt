package com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.menu

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.BaseMenuViewHolder
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.details.ViewFileDetailsClickListener
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.AbstractFileListItemNowPlayingHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileListItemContainer
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileNameTextViewSetter
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.menu.listeners.FileSeekToClickListener
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.menu.listeners.RemovePlaylistFileClickListener
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.storage.INowPlayingRepository
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.browsing.items.menu.AbstractListItemMenuBuilder
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.namehillsoftware.handoff.promises.Promise
import com.vedsoft.futures.runnables.OneParameterAction

class NowPlayingFileListItemMenuBuilder(private val nowPlayingRepository: INowPlayingRepository) : AbstractListItemMenuBuilder<ServiceFile>() {
	private var onPlaylistFileRemovedListener: OneParameterAction<Int>? = null

	private class ViewHolder internal constructor(val fileListItemContainer: FileListItemContainer, val fileNameTextViewSetter: FileNameTextViewSetter, viewFileDetailsButtonFinder: LazyViewFinder<ImageButton>, playButtonFinder: LazyViewFinder<ImageButton>, private val removeButtonFinder: LazyViewFinder<ImageButton>) : BaseMenuViewHolder(viewFileDetailsButtonFinder, playButtonFinder) {
		var fileListItemNowPlayingHandler: AbstractFileListItemNowPlayingHandler? = null
		var filePropertiesProvider: Promise<*>? = null

		val removeButton: ImageButton
			get() = removeButtonFinder.findView()
	}

	override fun getView(position: Int, serviceFile: ServiceFile, previousView: View?, parent: ViewGroup): View {
		var convertView = previousView
		if (convertView == null) {
			val fileItemMenu = FileListItemContainer(parent.context)
			val viewFlipper = fileItemMenu.viewAnimator

			convertView = viewFlipper

			val inflater = parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
			val fileMenu = inflater.inflate(R.layout.layout_now_playing_file_item_menu, parent, false) as LinearLayout

			viewFlipper.addView(fileMenu)
			viewFlipper.tag = ViewHolder(
				fileItemMenu,
				FileNameTextViewSetter(fileItemMenu.findTextView()),
				LazyViewFinder(fileMenu, R.id.btnViewFileDetails),
				LazyViewFinder(fileMenu, R.id.btnPlaySong),
				LazyViewFinder(fileMenu, R.id.btnRemoveFromPlaylist))

			viewFlipper.setViewChangedListener(onViewChangedListener)
		}

		val viewHolder = convertView?.tag as ViewHolder

		val fileListItem = viewHolder.fileListItemContainer
		val textView = fileListItem.findTextView()

		viewHolder.filePropertiesProvider = viewHolder.fileNameTextViewSetter.promiseTextViewUpdate(serviceFile)
		nowPlayingRepository
			.nowPlaying
			.eventually(LoopedInPromise.response<NowPlaying, Any?>({ np: NowPlaying ->
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
		LongClickViewAnimatorListener.tryFlipToPreviousView(viewFlipper)
		viewHolder.playButton.setOnClickListener(FileSeekToClickListener(viewFlipper, position))
		viewHolder.viewFileDetailsButton.setOnClickListener(ViewFileDetailsClickListener(viewFlipper, serviceFile))
		viewHolder.removeButton.setOnClickListener(RemovePlaylistFileClickListener(viewFlipper, position, onPlaylistFileRemovedListener))

		return viewFlipper
	}

	fun setOnPlaylistFileRemovedListener(onPlaylistFileRemovedListener: OneParameterAction<Int>?) {
		this.onPlaylistFileRemovedListener = onPlaylistFileRemovedListener
	}
}
