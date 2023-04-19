package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.menu

import android.content.Context
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
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.ViewFileDetailsClickListener
import com.lasthopesoftware.bluewater.client.browsing.files.menu.FileNameTextViewSetter
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertiesUpdatedMessage
import com.lasthopesoftware.bluewater.client.browsing.items.menu.BuildListItemMenuViewContainers
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener.Companion.tryFlipToPreviousView
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator
import com.lasthopesoftware.bluewater.client.browsing.items.menu.OnViewChangedListener
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideScopedUrlKey
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.MaintainNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.playlist.EditPlaylist
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.playlist.FinishEditPlaylist
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.playlist.HasEditPlaylistState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.playlist.ItemDragged
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.playlist.NowPlayingPlaylistMessage
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.menu.listeners.FileSeekToClickListener
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.menu.listeners.RemovePlaylistFileClickListener
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils
import com.lasthopesoftware.bluewater.shared.android.view.getValue
import com.lasthopesoftware.bluewater.shared.messages.RegisterForTypedMessages
import com.lasthopesoftware.bluewater.shared.messages.SendTypedMessages
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse


class NowPlayingFileListItemMenuBuilder(
	private val nowPlayingRepository: MaintainNowPlayingState,
	private val registerForApplicationMessages: RegisterForApplicationMessages,
	private val hasEditPlaylistState: HasEditPlaylistState,
	private val typedMessagesRegistration: RegisterForTypedMessages<NowPlayingPlaylistMessage>,
	private val sendTypedMessages: SendTypedMessages<NowPlayingPlaylistMessage>,
	private val scopedUrlKeyProvider: ProvideScopedUrlKey
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

		val handler = Handler(parent.context.mainLooper)
		typedMessagesRegistration
			.registerReceiver(handler) { _ : EditPlaylist ->
				notifyOnFlipViewAnimator.tryFlipToPreviousView()
				notifyOnFlipViewAnimator.isEnabled = false
				dragButton.visibility = View.VISIBLE
			}
		typedMessagesRegistration
			.registerReceiver(handler) { _ : FinishEditPlaylist ->
				notifyOnFlipViewAnimator.isEnabled = true
				dragButton.visibility = View.GONE
			}

		return ViewHolder(notifyOnFlipViewAnimator)
			.also(dragButton::setOnLongClickListener)
	}

	inner class ViewHolder internal constructor(
		private val viewAnimator: NotifyOnFlipViewAnimator
	) :
		RecyclerView.ViewHolder(viewAnimator),
		View.OnLongClickListener,
		ImmediateResponse<NowPlaying?, Unit>
	{
		private val handler by lazy { Handler(viewAnimator.context.mainLooper) }
		private val viewFileDetailsButton by LazyViewFinder<ImageButton>(itemView, R.id.btnViewFileDetails)
		private val playButton by LazyViewFinder<ImageButton>(itemView, R.id.btnPlaySong)
		private val removeButton by LazyViewFinder<ImageButton>(itemView, R.id.btnRemoveFromPlaylist)
		private val textView by LazyViewFinder<TextView>(itemView, R.id.fileName)
		private val artistView by LazyViewFinder<TextView>(itemView, R.id.artist)
		private val fileNameTextViewSetter by lazy { FileNameTextViewSetter(textView, artistView) }

		init {
			registerForApplicationMessages.registerReceiver(handler) { message: PlaybackMessage.TrackChanged ->
				val position = positionedFile?.playlistPosition
				textView.setTypeface(
					null,
					ViewUtils.getActiveListItemTextViewStyle(position == message.positionedFile.playlistPosition)
				)
				viewAnimator.isSelected = position == message.positionedFile.playlistPosition
			}

			registerForApplicationMessages.registerReceiver { message: FilePropertiesUpdatedMessage ->
				if (urlKey == message.urlServiceKey) positionedFile?.serviceFile?.also {
					fileNameTextViewSetter.promiseTextViewUpdate(it)
				}
			}
		}

		private var playlist: List<ServiceFile>? = null
		private var positionedFile: PositionedFile? = null
		private var urlKey: UrlKeyHolder<ServiceFile>? = null

		fun update(positionedFile: PositionedFile, playlist: List<ServiceFile>) {
			this.positionedFile = positionedFile
			this.playlist = playlist

			val serviceFile = positionedFile.serviceFile

			fileNameTextViewSetter.promiseTextViewUpdate(serviceFile)

			val position = positionedFile.playlistPosition

			nowPlayingRepository
				.promiseNowPlaying()
				.eventually(LoopedInPromise.response(this, handler))

			scopedUrlKeyProvider
				.promiseUrlKey(serviceFile)
				.then { urlKey = it }

			viewAnimator.tryFlipToPreviousView()
			playButton.setOnClickListener(FileSeekToClickListener(viewAnimator, position))
			removeButton.setOnClickListener(RemovePlaylistFileClickListener(viewAnimator, position))
		}

		override fun onLongClick(v: View?): Boolean =
			positionedFile
				?.let {
					sendTypedMessages.sendMessage(ItemDragged(it))
					true
				}
				?: false

		override fun respond(np: NowPlaying?) {
			val viewFileDetailsClickListener = np?.libraryId?.let { libraryId ->
				positionedFile?.let { file ->
					playlist?.let {
						ViewFileDetailsClickListener(viewAnimator, libraryId, file, it)
					}
				}
			}
			if (viewFileDetailsClickListener != null) {
				viewAnimator.setOnClickListener(viewFileDetailsClickListener)
				viewFileDetailsButton.setOnClickListener(viewFileDetailsClickListener)
			}

			val position = positionedFile?.playlistPosition
			textView.setTypeface(null, ViewUtils.getActiveListItemTextViewStyle(position == np?.playlistPosition))
			viewAnimator.isSelected = position == np?.playlistPosition
		}
	}
}
