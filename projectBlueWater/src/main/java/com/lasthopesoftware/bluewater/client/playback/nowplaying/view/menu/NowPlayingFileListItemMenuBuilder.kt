package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.menu

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.details.ViewFileDetailsClickListener
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileListItemNowPlayingRegistrar
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileNameTextViewSetter
import com.lasthopesoftware.bluewater.client.browsing.items.menu.BuildListItemMenuViewContainers
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator
import com.lasthopesoftware.bluewater.client.browsing.items.menu.OnViewChangedListener
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.MaintainNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments.playlist.*
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.menu.listeners.FileSeekToClickListener
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.menu.listeners.RemovePlaylistFileClickListener
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.shared.android.messages.ReceiveBroadcastEvents
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils
import com.lasthopesoftware.bluewater.shared.android.view.getValue
import com.lasthopesoftware.bluewater.shared.messages.RegisterForTypedMessages
import com.lasthopesoftware.bluewater.shared.messages.SendTypedMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise


class NowPlayingFileListItemMenuBuilder(
	private val nowPlayingRepository: MaintainNowPlayingState,
	private val fileListItemNowPlayingRegistrar: FileListItemNowPlayingRegistrar,
	private val hasEditPlaylistState: HasEditPlaylistState,
	private val typedMessagesRegistration: RegisterForTypedMessages<NowPlayingPlaylistMessage>,
	private val sendTypedMessages: SendTypedMessages<NowPlayingPlaylistMessage>
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

		val textViewContainer = inflater.inflate(R.layout.layout_now_playing_file_item, notifyOnFlipViewAnimator, false) as LinearLayout
		notifyOnFlipViewAnimator.addView(textViewContainer)

		val fileMenu = inflater.inflate(R.layout.layout_now_playing_file_item_menu, notifyOnFlipViewAnimator, false) as LinearLayout
		notifyOnFlipViewAnimator.addView(fileMenu)

		notifyOnFlipViewAnimator.setViewChangedListener(onViewChangedListener)
		notifyOnFlipViewAnimator.setOnLongClickListener(LongClickViewAnimatorListener(notifyOnFlipViewAnimator))

		val dragButton = notifyOnFlipViewAnimator.findViewById<ImageButton>(R.id.dragButton)
		dragButton.visibility = if (hasEditPlaylistState.isEditingPlaylist) View.VISIBLE else View.GONE
		typedMessagesRegistration
			.registerReceiver { _ : EditPlaylist ->
				LongClickViewAnimatorListener.tryFlipToPreviousView(notifyOnFlipViewAnimator)
				notifyOnFlipViewAnimator.isEnabled = false
				dragButton.visibility = View.VISIBLE
			}
		typedMessagesRegistration
			.registerReceiver { _ : FinishEditPlaylist ->
				notifyOnFlipViewAnimator.isEnabled = true
				dragButton.visibility = View.GONE
			}

		return ViewHolder(notifyOnFlipViewAnimator)
			.also(fileListItemNowPlayingRegistrar::registerNewHandler)
			.also(dragButton::setOnLongClickListener)
	}

	inner class ViewHolder internal constructor(private val viewAnimator: NotifyOnFlipViewAnimator)
		: RecyclerView.ViewHolder(viewAnimator), ReceiveBroadcastEvents, View.OnLongClickListener {

		private val handler by lazy { Handler(viewAnimator.context.mainLooper) }
		private val viewFileDetailsButton by LazyViewFinder<ImageButton>(itemView, R.id.btnViewFileDetails)
		private val playButton by LazyViewFinder<ImageButton>(itemView, R.id.btnPlaySong)
		private val removeButton by LazyViewFinder<ImageButton>(itemView, R.id.btnRemoveFromPlaylist)
		private val textView by LazyViewFinder<TextView>(itemView, R.id.fileName)
		private val artistView by LazyViewFinder<TextView>(itemView, R.id.artist)
		private val fileNameTextViewSetter by lazy { FileNameTextViewSetter(textView, artistView) }

		private var positionedFile: PositionedFile? = null

		fun update(positionedFile: PositionedFile) {
			this.positionedFile = positionedFile

			val serviceFile = positionedFile.serviceFile

			fileNameTextViewSetter.promiseTextViewUpdate(serviceFile)

			val position = positionedFile.playlistPosition

			nowPlayingRepository
				.promiseNowPlaying()
				.eventually(LoopedInPromise.response({ np ->
					textView.setTypeface(null, ViewUtils.getActiveListItemTextViewStyle(position == np?.playlistPosition))
					viewAnimator.isSelected = position == np?.playlistPosition
				}, handler))

			LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator)
			playButton.setOnClickListener(FileSeekToClickListener(viewAnimator, position))
			viewFileDetailsButton.setOnClickListener(ViewFileDetailsClickListener(viewAnimator, serviceFile))
			removeButton.setOnClickListener(RemovePlaylistFileClickListener(viewAnimator, position))
		}

		override fun onReceive(intent: Intent) {
			val playlistPosition = intent.getIntExtra(PlaylistEvents.PlaylistParameters.playlistPosition, -1)
			val position = positionedFile?.playlistPosition
			textView.setTypeface(null, ViewUtils.getActiveListItemTextViewStyle(position == playlistPosition))
			viewAnimator.isSelected = position == playlistPosition
		}

		override fun onLongClick(v: View?): Boolean =
			positionedFile
				?.let {
					sendTypedMessages.sendMessage(ItemDragged(it, this))
					true
				}
				?: false
	}
}
