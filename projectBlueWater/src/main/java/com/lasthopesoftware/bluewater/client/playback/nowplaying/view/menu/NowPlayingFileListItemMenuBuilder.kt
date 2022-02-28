package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.menu

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.details.ViewFileDetailsClickListener
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileListItemContainer
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileListItemNowPlayingRegistrar
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileNameTextViewSetter
import com.lasthopesoftware.bluewater.client.browsing.items.menu.BuildListItemMenuViewContainers
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator
import com.lasthopesoftware.bluewater.client.browsing.items.menu.OnViewChangedListener
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.MaintainNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments.EditPlaylist
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments.NowPlayingPlaylistMessage
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.menu.listeners.FileSeekToClickListener
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.menu.listeners.RemovePlaylistFileClickListener
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils
import com.lasthopesoftware.bluewater.shared.android.view.getValue
import com.lasthopesoftware.bluewater.shared.messages.TypedMessageFeed
import com.lasthopesoftware.bluewater.shared.messages.receiveMessages
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import kotlinx.coroutines.flow.onEach


class NowPlayingFileListItemMenuBuilder(
	private val nowPlayingRepository: MaintainNowPlayingState,
	private val fileListItemNowPlayingRegistrar: FileListItemNowPlayingRegistrar,
	private val typedMessageFeed: TypedMessageFeed<NowPlayingPlaylistMessage>
) : BuildListItemMenuViewContainers<NowPlayingFileListItemMenuBuilder.ViewHolder>
{
	private var onViewChangedListener: OnViewChangedListener? = null

	fun setOnViewChangedListener(onViewChangedListener: OnViewChangedListener?) {
		this.onViewChangedListener = onViewChangedListener
	}

	override fun newViewHolder(parent: ViewGroup): NowPlayingFileListItemMenuBuilder.ViewHolder {
		val notifyOnFlipViewAnimator = NotifyOnFlipViewAnimator(parent.context)
		notifyOnFlipViewAnimator.layoutParams = AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
		val inflater = parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
		val textViewContainer = inflater.inflate(R.layout, viewAnimator, false) as RelativeLayout
		textViewFinder = LazyViewFinder(textViewContainer, R.id.tvStandard)
		notifyOnFlipViewAnimator.addView(textViewContainer)

		val fileMenu = inflater.inflate(R.layout.layout_now_playing_file_item_menu, parent, false) as LinearLayout

		notifyOnFlipViewAnimator.addView(fileMenu)
		notifyOnFlipViewAnimator.setViewChangedListener(onViewChangedListener)
		notifyOnFlipViewAnimator.setOnLongClickListener(LongClickViewAnimatorListener(notifyOnFlipViewAnimator))

		return ViewHolder(fileItemMenu)
	}

	inner class ViewHolder internal constructor(private val fileListItemContainer: FileListItemContainer)
		: RecyclerView.ViewHolder(fileListItemContainer.viewAnimator) {

		private val viewFileDetailsButton by LazyViewFinder<ImageButton>(itemView, R.id.btnViewFileDetails)
		private val playButton by LazyViewFinder<ImageButton>(itemView, R.id.btnPlaySong)
		private val removeButton by LazyViewFinder<ImageButton>(itemView, R.id.btnRemoveFromPlaylist)
		private val fileNameTextViewSetter = FileNameTextViewSetter(fileListItemContainer.textView)

		var fileListItemNowPlayingHandler: AutoCloseable? = null

		fun update(positionedFile: PositionedFile) {
			val fileListItem = fileListItemContainer
			val textView = fileListItem.textView

			val serviceFile = positionedFile.serviceFile

			fileNameTextViewSetter.promiseTextViewUpdate(serviceFile)

			val position = positionedFile.playlistPosition

			val viewFlipper = fileListItem.viewAnimator

			nowPlayingRepository
				.promiseNowPlaying()
				.eventually(LoopedInPromise.response({ np ->
					textView.setTypeface(null, ViewUtils.getActiveListItemTextViewStyle(position == np?.playlistPosition))
					viewFlipper.isSelected = position == np?.playlistPosition
				}, textView.context))

			fileListItemNowPlayingHandler?.close()
			fileListItemNowPlayingHandler = fileListItemNowPlayingRegistrar.registerNewHandler { intent ->
				val playlistPosition = intent.getIntExtra(PlaylistEvents.PlaylistParameters.playlistPosition, -1)
				textView.setTypeface(null, ViewUtils.getActiveListItemTextViewStyle(position == playlistPosition))
				viewFlipper.isSelected = position == playlistPosition
			}

			typedMessageFeed.receiveMessages<EditPlaylist>().onEach {

			}

			LongClickViewAnimatorListener.tryFlipToPreviousView(viewFlipper)
			playButton.setOnClickListener(FileSeekToClickListener(viewFlipper, position))
			viewFileDetailsButton.setOnClickListener(ViewFileDetailsClickListener(viewFlipper, serviceFile))
			removeButton.setOnClickListener(RemovePlaylistFileClickListener(viewFlipper, position))
		}
	}
}
