package com.lasthopesoftware.bluewater.client.playback.exoplayer

import android.os.Handler
import android.os.Looper
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ShuffleOrder
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter

class HandlerDispatchingExoPlayer(private val innerPlayer: ExoPlayer, private val handler: Handler) : PromisingExoPlayer {

	override fun getAudioComponent(): Promise<Player.AudioComponent?> =
		LoopedInPromise(
			MessageWriter { innerPlayer.audioComponent },
			handler)

	override fun getVideoComponent(): Promise<Player.VideoComponent?> =
		LoopedInPromise(
			MessageWriter { innerPlayer.videoComponent },
			handler)

	override fun getTextComponent(): Promise<Player.TextComponent?> =
		LoopedInPromise(
			MessageWriter { innerPlayer.textComponent },
			handler)

	override fun getMetadataComponent(): Promise<Player.MetadataComponent?> =
		LoopedInPromise(
			MessageWriter { innerPlayer.metadataComponent },
			handler)

	override fun getDeviceComponent(): Promise<Player.DeviceComponent?> =
		LoopedInPromise(
			MessageWriter { innerPlayer.deviceComponent },
			handler)

	override fun getApplicationLooper(): Promise<Looper> =
		LoopedInPromise(
			MessageWriter { innerPlayer.applicationLooper },
			handler)

	override fun addListener(listener: Player.EventListener): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.addListener(listener)
				this
			},
			handler)

	override fun removeListener(listener: Player.EventListener): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.removeListener(listener)
				this
			},
			handler)

	override fun setMediaItems(mediaItems: MutableList<MediaItem>): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.setMediaItems(mediaItems)
				this
			},
			handler)

	override fun setMediaItems(mediaItems: MutableList<MediaItem>, resetPosition: Boolean): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.setMediaItems(mediaItems, resetPosition)
				this
			},
			handler)

	override fun setMediaItems(mediaItems: MutableList<MediaItem>, startWindowIndex: Int, startPositionMs: Long): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.setMediaItems(mediaItems, startWindowIndex, startPositionMs)
				this
			},
			handler)

	override fun setMediaItem(mediaItem: MediaItem): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.setMediaItem(mediaItem)
				this
			},
			handler)

	override fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.setMediaItem(mediaItem, startPositionMs)
				this
			},
			handler)

	override fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.setMediaItem(mediaItem, resetPosition)
				this
			},
			handler)

	override fun addMediaItem(mediaItem: MediaItem): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.addMediaItem(mediaItem)
				this
			},
			handler)

	override fun addMediaItem(index: Int, mediaItem: MediaItem): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.addMediaItem(index, mediaItem)
				this
			},
			handler)

	override fun addMediaItems(mediaItems: MutableList<MediaItem>): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.addMediaItems(mediaItems)
				this
			},
			handler)

	override fun addMediaItems(index: Int, mediaItems: MutableList<MediaItem>): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.addMediaItems(index, mediaItems)
				this
			},
			handler)

	override fun moveMediaItem(currentIndex: Int, newIndex: Int): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.moveMediaItem(currentIndex, newIndex)
				this
			},
			handler)

	override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.moveMediaItems(fromIndex, toIndex, newIndex)
				this
			},
			handler)

	override fun removeMediaItem(index: Int): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.removeMediaItem(index)
				this
			},
			handler)

	override fun removeMediaItems(fromIndex: Int, toIndex: Int): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.removeMediaItems(fromIndex, toIndex)
				this
			},
			handler)

	override fun clearMediaItems(): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.clearMediaItems()
				this
			},
			handler)

	override fun prepare(mediaSource: MediaSource): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.prepare()
				this
			},
			handler)

	override fun prepare(mediaSource: MediaSource, resetPosition: Boolean, resetState: Boolean): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.prepare(mediaSource, resetPosition, resetState)
				this
			},
			handler)

	override fun prepare(): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.prepare()
				this
			},
			handler)

	override fun getPlaybackState(): Promise<Int> =
		LoopedInPromise(
			MessageWriter { innerPlayer.playbackState },
			handler)

	override fun getPlaybackSuppressionReason(): Promise<Int> =
		LoopedInPromise(
			MessageWriter { innerPlayer.playbackSuppressionReason },
			handler)

	override fun isPlaying(): Promise<Boolean> =
		LoopedInPromise(
			MessageWriter { innerPlayer.isPlaying },
			handler)

	override fun getPlayerError(): Promise<ExoPlaybackException?> =
		LoopedInPromise(
			MessageWriter { innerPlayer.playerError },
			handler)

	override fun getPlaybackError(): Promise<ExoPlaybackException?> =
		LoopedInPromise(
			MessageWriter { innerPlayer.playbackError },
			handler)

	override fun play(): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.play()
				this
			},
			handler)

	override fun pause(): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.pause()
				this
			},
			handler)

	override fun setPlayWhenReady(playWhenReady: Boolean): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.playWhenReady = playWhenReady
				this
			},
			handler)

	override fun getPlayWhenReady(): Promise<Boolean> =
		LoopedInPromise(
			MessageWriter { innerPlayer.playWhenReady },
			handler)

	override fun setRepeatMode(repeatMode: Int): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.repeatMode = repeatMode
				this
			},
			handler)

	override fun getRepeatMode(): Promise<Int> =
		LoopedInPromise(
			MessageWriter { innerPlayer.repeatMode },
			handler)

	override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.shuffleModeEnabled = shuffleModeEnabled
				this
			},
			handler)

	override fun getShuffleModeEnabled(): Promise<Boolean> =
		LoopedInPromise(
			MessageWriter { innerPlayer.shuffleModeEnabled },
			handler)

	override fun isLoading(): Promise<Boolean> =
		LoopedInPromise(
			MessageWriter { innerPlayer.isLoading },
			handler)

	override fun seekToDefaultPosition(): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.seekToDefaultPosition()
				this
			},
			handler)

	override fun seekToDefaultPosition(windowIndex: Int): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.seekToDefaultPosition(windowIndex)
				this
			},
			handler)

	override fun seekTo(positionMs: Long): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.seekTo(positionMs)
				this
			},
			handler)

	override fun seekTo(windowIndex: Int, positionMs: Long): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.seekTo(windowIndex, positionMs)
				this
			},
			handler)

	override fun hasPrevious(): Promise<Boolean> =
		LoopedInPromise(
			MessageWriter { innerPlayer.hasPrevious() },
			handler)

	override fun previous(): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.previous()
				this
			},
			handler)

	override fun hasNext(): Promise<Boolean> =
		LoopedInPromise(
			MessageWriter { innerPlayer.hasNext() },
			handler)

	override fun next(): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.next()
				this
			},
			handler)

	override fun setPlaybackParameters(playbackParameters: PlaybackParameters?): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.setPlaybackParameters(playbackParameters)
				this
			},
			handler)

	override fun getPlaybackParameters(): Promise<PlaybackParameters> =
		LoopedInPromise(
			MessageWriter { innerPlayer.playbackParameters },
			handler)

	override fun stop(): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.stop()
				this
			},
			handler)

	override fun stop(reset: Boolean): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.stop(reset)
				this
			},
			handler)

	override fun release(): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.release()
				this
			},
			handler)

	override fun getRendererCount(): Promise<Int> =
		LoopedInPromise(
			MessageWriter { innerPlayer.rendererCount },
			handler)

	override fun getRendererType(index: Int): Promise<Int> =
		LoopedInPromise(
			MessageWriter { innerPlayer.getRendererType(index) },
			handler)

	override fun getTrackSelector(): Promise<TrackSelector?> =
		LoopedInPromise(
			MessageWriter { innerPlayer.trackSelector },
			handler)

	override fun getCurrentTrackGroups(): Promise<TrackGroupArray> =
		LoopedInPromise(
			MessageWriter { innerPlayer.currentTrackGroups },
			handler)

	override fun getCurrentTrackSelections(): Promise<TrackSelectionArray> =
		LoopedInPromise(
			MessageWriter { innerPlayer.currentTrackSelections },
			handler)

	override fun getCurrentManifest(): Promise<Any?> =
		LoopedInPromise(
			MessageWriter { innerPlayer.currentManifest },
			handler)

	override fun getCurrentTimeline(): Promise<Timeline> =
		LoopedInPromise(
			MessageWriter { innerPlayer.currentTimeline },
			handler)

	override fun getCurrentPeriodIndex(): Promise<Int> =
		LoopedInPromise(
			MessageWriter { innerPlayer.currentPeriodIndex },
			handler)

	override fun getCurrentWindowIndex(): Promise<Int> =
		LoopedInPromise(
			MessageWriter { innerPlayer.currentWindowIndex },
			handler)

	override fun getNextWindowIndex(): Promise<Int> =
		LoopedInPromise(
			MessageWriter { innerPlayer.nextWindowIndex },
			handler)

	override fun getPreviousWindowIndex(): Promise<Int> =
		LoopedInPromise(
			MessageWriter { innerPlayer.previousWindowIndex },
			handler)

	override fun getCurrentTag(): Promise<Any?> =
		LoopedInPromise(
			MessageWriter { innerPlayer.currentTag },
			handler)

	override fun getCurrentMediaItem(): Promise<MediaItem?> =
		LoopedInPromise(
			MessageWriter { innerPlayer.currentMediaItem },
			handler)

	override fun getMediaItemCount(): Promise<Int> =
		LoopedInPromise(
			MessageWriter { innerPlayer.mediaItemCount },
			handler)

	override fun getMediaItemAt(index: Int): Promise<MediaItem> =
		LoopedInPromise(
			MessageWriter { innerPlayer.getMediaItemAt(index) },
			handler)

	override fun getDuration(): Promise<Long> =
		LoopedInPromise(
			MessageWriter { innerPlayer.duration },
			handler)

	override fun getCurrentPosition(): Promise<Long> =
		LoopedInPromise(
			MessageWriter { innerPlayer.currentPosition },
			handler)

	override fun getBufferedPosition(): Promise<Long> =
		LoopedInPromise(
			MessageWriter { innerPlayer.bufferedPosition },
			handler)

	override fun getBufferedPercentage(): Promise<Int> =
		LoopedInPromise(
			MessageWriter { innerPlayer.bufferedPercentage },
			handler)

	override fun getTotalBufferedDuration(): Promise<Long> =
		LoopedInPromise(
			MessageWriter { innerPlayer.totalBufferedDuration },
			handler)

	override fun isCurrentWindowDynamic(): Promise<Boolean> =
		LoopedInPromise(
			MessageWriter { innerPlayer.isCurrentWindowDynamic },
			handler)

	override fun isCurrentWindowLive(): Promise<Boolean> =
		LoopedInPromise(
			MessageWriter { innerPlayer.isCurrentWindowLive },
			handler)

	override fun getCurrentLiveOffset(): Promise<Long> =
		LoopedInPromise(
			MessageWriter { innerPlayer.currentLiveOffset },
			handler)

	override fun isCurrentWindowSeekable(): Promise<Boolean> =
		LoopedInPromise(
			MessageWriter { innerPlayer.isCurrentWindowSeekable },
			handler)

	override fun isPlayingAd(): Promise<Boolean> =
		LoopedInPromise(
			MessageWriter { innerPlayer.isPlayingAd },
			handler)

	override fun getCurrentAdGroupIndex(): Promise<Int> =
		LoopedInPromise(
			MessageWriter { innerPlayer.currentAdGroupIndex },
			handler)

	override fun getCurrentAdIndexInAdGroup(): Promise<Int> =
		LoopedInPromise(
			MessageWriter { innerPlayer.currentAdIndexInAdGroup },
			handler)

	override fun getContentDuration(): Promise<Long> =
		LoopedInPromise(
			MessageWriter { innerPlayer.contentDuration },
			handler)

	override fun getContentPosition(): Promise<Long> =
		LoopedInPromise(
			MessageWriter { innerPlayer.contentPosition },
			handler)

	override fun getContentBufferedPosition(): Promise<Long> =
		LoopedInPromise(
			MessageWriter { innerPlayer.contentBufferedPosition },
			handler)

	override fun getPlaybackLooper(): Promise<Looper> =
		LoopedInPromise(
			MessageWriter { innerPlayer.playbackLooper },
			handler)

	override fun retry(): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.retry()
				this
			},
			handler)

	override fun setMediaSources(mediaSources: MutableList<MediaSource>): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.setMediaSources(mediaSources)
				this
			},
			handler)

	override fun setMediaSources(mediaSources: MutableList<MediaSource>, resetPosition: Boolean): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.setMediaSources(mediaSources, resetPosition)
				this
			},
			handler)

	override fun setMediaSources(mediaSources: MutableList<MediaSource>, startWindowIndex: Int, startPositionMs: Long): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.setMediaSources(mediaSources, startWindowIndex, startPositionMs)
				this
			},
			handler)

	override fun setMediaSource(mediaSource: MediaSource): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.setMediaSource(mediaSource)
				this
			},
			handler)

	override fun setMediaSource(mediaSource: MediaSource, startPositionMs: Long): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.setMediaSource(mediaSource, startPositionMs)
				this
			},
			handler)

	override fun setMediaSource(mediaSource: MediaSource, resetPosition: Boolean): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.setMediaSource(mediaSource, resetPosition)
				this
			},
			handler)

	override fun addMediaSource(mediaSource: MediaSource): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.addMediaSource(mediaSource)
				this
			},
			handler)

	override fun addMediaSource(index: Int, mediaSource: MediaSource): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.addMediaSource(index, mediaSource)
				this
			},
			handler)

	override fun addMediaSources(mediaSources: MutableList<MediaSource>): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.addMediaSources(mediaSources)
				this
			},
			handler)

	override fun addMediaSources(index: Int, mediaSources: MutableList<MediaSource>): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.addMediaSources(index, mediaSources)
				this
			},
			handler)

	override fun setShuffleOrder(shuffleOrder: ShuffleOrder): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.setShuffleOrder(shuffleOrder)
				this
			},
			handler)

	override fun createMessage(target: PlayerMessage.Target): Promise<PlayerMessage> =
		LoopedInPromise(
			MessageWriter { innerPlayer.createMessage(target) },
			handler)

	override fun setSeekParameters(seekParameters: SeekParameters?): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.setSeekParameters(seekParameters)
				this
			},
			handler)

	override fun getSeekParameters(): Promise<SeekParameters> =
		LoopedInPromise(
			MessageWriter { innerPlayer.seekParameters },
			handler)

	override fun setForegroundMode(foregroundMode: Boolean): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.setForegroundMode(foregroundMode)
				this
			},
			handler)

	override fun setPauseAtEndOfMediaItems(pauseAtEndOfMediaItems: Boolean): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.pauseAtEndOfMediaItems = pauseAtEndOfMediaItems
				this
			},
			handler)

	override fun getPauseAtEndOfMediaItems(): Promise<Boolean> =
		LoopedInPromise(
			MessageWriter { innerPlayer.pauseAtEndOfMediaItems },
			handler)

	override fun experimentalSetOffloadSchedulingEnabled(offloadSchedulingEnabled: Boolean): Promise<PromisingExoPlayer> =
		LoopedInPromise(
			MessageWriter {
				innerPlayer.experimentalSetOffloadSchedulingEnabled(offloadSchedulingEnabled)
				this
			},
			handler)
}
