package com.lasthopesoftware.bluewater.client.playback.exoplayer

import android.os.Looper
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ShuffleOrder
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import java.util.concurrent.Executors

class SingleThreadedExoPlayer(private val innerPlayer: ExoPlayer) : PromisingExoPlayer {

	companion object {
		private val executor = Executors.newSingleThreadExecutor()
	}

	override fun getAudioComponent(): Promise<Player.AudioComponent?> =
		QueuedPromise(
			MessageWriter{ innerPlayer.audioComponent },
			executor)

	override fun getVideoComponent(): Promise<Player.VideoComponent?> =
		QueuedPromise(
			MessageWriter{ innerPlayer.videoComponent },
			executor)

	override fun getTextComponent(): Promise<Player.TextComponent?> =
		QueuedPromise(
			MessageWriter{ innerPlayer.textComponent },
			executor)

	override fun getMetadataComponent(): Promise<Player.MetadataComponent?>  =
		QueuedPromise(
			MessageWriter{ innerPlayer.metadataComponent },
			executor)

	override fun getDeviceComponent(): Promise<Player.DeviceComponent?> =
		QueuedPromise(
			MessageWriter{ innerPlayer.deviceComponent },
			executor)

	override fun getApplicationLooper(): Promise<Looper> =
		QueuedPromise(
			MessageWriter{ innerPlayer.applicationLooper },
			executor)

	override fun addListener(listener: Player.EventListener): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.addListener(listener) },
			executor)

	override fun removeListener(listener: Player.EventListener): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.removeListener(listener) },
			executor)

	override fun setMediaItems(mediaItems: MutableList<MediaItem>): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.setMediaItems(mediaItems) },
			executor)

	override fun setMediaItems(mediaItems: MutableList<MediaItem>, resetPosition: Boolean): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.setMediaItems(mediaItems, resetPosition) },
			executor)

	override fun setMediaItems(mediaItems: MutableList<MediaItem>, startWindowIndex: Int, startPositionMs: Long): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.setMediaItems(mediaItems, startWindowIndex, startPositionMs) },
			executor)

	override fun setMediaItem(mediaItem: MediaItem): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.setMediaItem(mediaItem) },
			executor)

	override fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.setMediaItem(mediaItem, startPositionMs) },
			executor)

	override fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.setMediaItem(mediaItem, resetPosition) },
			executor)

	override fun addMediaItem(mediaItem: MediaItem): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.addMediaItem(mediaItem) },
			executor)

	override fun addMediaItem(index: Int, mediaItem: MediaItem): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.addMediaItem(index, mediaItem) },
			executor)

	override fun addMediaItems(mediaItems: MutableList<MediaItem>): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.addMediaItems(mediaItems) },
			executor)

	override fun addMediaItems(index: Int, mediaItems: MutableList<MediaItem>): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.addMediaItems(index, mediaItems) },
			executor)

	override fun moveMediaItem(currentIndex: Int, newIndex: Int): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.moveMediaItem(currentIndex, newIndex) },
			executor)

	override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.moveMediaItems(fromIndex, toIndex, newIndex) },
			executor)

	override fun removeMediaItem(index: Int): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.removeMediaItem(index) },
			executor)

	override fun removeMediaItems(fromIndex: Int, toIndex: Int): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.removeMediaItems(fromIndex, toIndex) },
			executor)

	override fun clearMediaItems(): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.clearMediaItems() },
			executor)

	override fun prepare(mediaSource: MediaSource): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.prepare() },
			executor)

	override fun prepare(mediaSource: MediaSource, resetPosition: Boolean, resetState: Boolean): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.prepare(mediaSource, resetPosition, resetState) },
			executor)

	override fun prepare(): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.prepare() },
			executor)

	override fun getPlaybackState(): Promise<Int> =
		QueuedPromise(
			MessageWriter{ innerPlayer.playbackState },
			executor)

	override fun getPlaybackSuppressionReason(): Promise<Int> =
		QueuedPromise(
			MessageWriter{ innerPlayer.playbackSuppressionReason },
			executor)

	override fun isPlaying(): Promise<Boolean> =
		QueuedPromise(
			MessageWriter{ innerPlayer.isPlaying },
			executor)

	override fun getPlayerError(): Promise<ExoPlaybackException?> =
		QueuedPromise(
			MessageWriter{ innerPlayer.playerError },
			executor)

	override fun getPlaybackError(): Promise<ExoPlaybackException?> =
		QueuedPromise(
			MessageWriter{ innerPlayer.playbackError },
			executor)

	override fun play(): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.play() },
			executor)

	override fun pause(): Promise<Unit>  =
		QueuedPromise(
			MessageWriter{ innerPlayer.pause() },
			executor)

	override fun setPlayWhenReady(playWhenReady: Boolean): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.playWhenReady = playWhenReady },
			executor)

	override fun getPlayWhenReady(): Promise<Boolean> =
		QueuedPromise(
			MessageWriter{ innerPlayer.playWhenReady },
			executor)

	override fun setRepeatMode(repeatMode: Int): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.repeatMode = repeatMode },
			executor)

	override fun getRepeatMode(): Promise<Int> =
		QueuedPromise(
			MessageWriter{ innerPlayer.repeatMode },
			executor)

	override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.shuffleModeEnabled = shuffleModeEnabled },
			executor)

	override fun getShuffleModeEnabled(): Promise<Boolean> =
		QueuedPromise(
			MessageWriter{ innerPlayer.shuffleModeEnabled },
			executor)

	override fun isLoading(): Promise<Boolean> =
		QueuedPromise(
			MessageWriter{ innerPlayer.isLoading },
			executor)

	override fun seekToDefaultPosition(): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.seekToDefaultPosition() },
			executor)

	override fun seekToDefaultPosition(windowIndex: Int): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.seekToDefaultPosition(windowIndex) },
			executor)

	override fun seekTo(positionMs: Long): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.seekTo(positionMs) },
			executor)

	override fun seekTo(windowIndex: Int, positionMs: Long): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.seekTo(windowIndex, positionMs) },
			executor)

	override fun hasPrevious(): Promise<Boolean> =
		QueuedPromise(
			MessageWriter{ innerPlayer.hasPrevious() },
			executor)

	override fun previous(): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.previous() },
			executor)

	override fun hasNext(): Promise<Boolean> =
		QueuedPromise(
			MessageWriter{ innerPlayer.hasNext() },
			executor)

	override fun next(): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.next() },
			executor)

	override fun setPlaybackParameters(playbackParameters: PlaybackParameters?): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.setPlaybackParameters(playbackParameters) },
			executor)

	override fun getPlaybackParameters(): Promise<PlaybackParameters> =
		QueuedPromise(
			MessageWriter{ innerPlayer.playbackParameters },
			executor)

	override fun stop(): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.stop() },
			executor)

	override fun stop(reset: Boolean): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.stop(reset) },
			executor)

	override fun release(): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.release() },
			executor)

	override fun getRendererCount(): Promise<Int> =
		QueuedPromise(
			MessageWriter{ innerPlayer.rendererCount },
			executor)

	override fun getRendererType(index: Int): Promise<Int> =
		QueuedPromise(
			MessageWriter{ innerPlayer.getRendererType(index) },
			executor)

	override fun getTrackSelector(): Promise<TrackSelector?> =
		QueuedPromise(
			MessageWriter{ innerPlayer.trackSelector },
			executor)

	override fun getCurrentTrackGroups(): Promise<TrackGroupArray> =
		QueuedPromise(
			MessageWriter{ innerPlayer.currentTrackGroups },
			executor)

	override fun getCurrentTrackSelections(): Promise<TrackSelectionArray> =
		QueuedPromise(
			MessageWriter{ innerPlayer.currentTrackSelections },
			executor)

	override fun getCurrentManifest(): Promise<Any?> =
		QueuedPromise(
			MessageWriter{ innerPlayer.currentManifest },
			executor)

	override fun getCurrentTimeline(): Promise<Timeline> =
		QueuedPromise(
			MessageWriter{ innerPlayer.currentTimeline },
			executor)

	override fun getCurrentPeriodIndex(): Promise<Int> =
		QueuedPromise(
			MessageWriter{ innerPlayer.currentPeriodIndex },
			executor)

	override fun getCurrentWindowIndex(): Promise<Int> =
		QueuedPromise(
			MessageWriter{ innerPlayer.currentWindowIndex },
			executor)

	override fun getNextWindowIndex(): Promise<Int> =
		QueuedPromise(
			MessageWriter{ innerPlayer.nextWindowIndex },
			executor)

	override fun getPreviousWindowIndex(): Promise<Int> =
		QueuedPromise(
			MessageWriter{ innerPlayer.previousWindowIndex },
			executor)

	override fun getCurrentTag(): Promise<Any?> =
		QueuedPromise(
			MessageWriter{ innerPlayer.currentTag },
			executor)

	override fun getCurrentMediaItem(): Promise<MediaItem?> =
		QueuedPromise(
			MessageWriter{ innerPlayer.currentMediaItem },
			executor)

	override fun getMediaItemCount(): Promise<Int> =
		QueuedPromise(
			MessageWriter{ innerPlayer.mediaItemCount },
			executor)

	override fun getMediaItemAt(index: Int): Promise<MediaItem> =
		QueuedPromise(
			MessageWriter{ innerPlayer.getMediaItemAt(index) },
			executor)

	override fun getDuration(): Promise<Long> =
		QueuedPromise(
			MessageWriter{ innerPlayer.duration },
			executor)

	override fun getCurrentPosition(): Promise<Long> =
		QueuedPromise(
			MessageWriter{ innerPlayer.currentPosition },
			executor)

	override fun getBufferedPosition(): Promise<Long> =
		QueuedPromise(
			MessageWriter{ innerPlayer.bufferedPosition },
			executor)

	override fun getBufferedPercentage(): Promise<Int> =
		QueuedPromise(
			MessageWriter{ innerPlayer.bufferedPercentage },
			executor)

	override fun getTotalBufferedDuration(): Promise<Long> =
		QueuedPromise(
			MessageWriter{ innerPlayer.totalBufferedDuration },
			executor)

	override fun isCurrentWindowDynamic(): Promise<Boolean> =
		QueuedPromise(
			MessageWriter{ innerPlayer.isCurrentWindowDynamic },
			executor)

	override fun isCurrentWindowLive(): Promise<Boolean> =
		QueuedPromise(
			MessageWriter{ innerPlayer.isCurrentWindowLive },
			executor)

	override fun getCurrentLiveOffset(): Promise<Long> =
		QueuedPromise(
			MessageWriter{ innerPlayer.currentLiveOffset },
			executor)

	override fun isCurrentWindowSeekable(): Promise<Boolean> =
		QueuedPromise(
			MessageWriter{ innerPlayer.isCurrentWindowSeekable },
			executor)

	override fun isPlayingAd(): Promise<Boolean> =
		QueuedPromise(
			MessageWriter{ innerPlayer.isPlayingAd },
			executor)

	override fun getCurrentAdGroupIndex(): Promise<Int> =
		QueuedPromise(
			MessageWriter{ innerPlayer.currentAdGroupIndex },
			executor)

	override fun getCurrentAdIndexInAdGroup(): Promise<Int> =
		QueuedPromise(
			MessageWriter{ innerPlayer.currentAdIndexInAdGroup },
			executor)

	override fun getContentDuration(): Promise<Long> =
		QueuedPromise(
			MessageWriter{ innerPlayer.contentDuration },
			executor)

	override fun getContentPosition(): Promise<Long> =
		QueuedPromise(
			MessageWriter{ innerPlayer.contentPosition },
			executor)

	override fun getContentBufferedPosition(): Promise<Long> =
		QueuedPromise(
			MessageWriter{ innerPlayer.contentBufferedPosition },
			executor)

	override fun getPlaybackLooper(): Promise<Looper> =
		QueuedPromise(
			MessageWriter{ innerPlayer.playbackLooper },
			executor)

	override fun retry(): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.retry() },
			executor)

	override fun setMediaSources(mediaSources: MutableList<MediaSource>): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.setMediaSources(mediaSources) },
			executor)

	override fun setMediaSources(mediaSources: MutableList<MediaSource>, resetPosition: Boolean): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.setMediaSources(mediaSources, resetPosition) },
			executor)

	override fun setMediaSources(mediaSources: MutableList<MediaSource>, startWindowIndex: Int, startPositionMs: Long): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.setMediaSources(mediaSources, startWindowIndex, startPositionMs) },
			executor)

	override fun setMediaSource(mediaSource: MediaSource): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.setMediaSource(mediaSource) },
			executor)

	override fun setMediaSource(mediaSource: MediaSource, startPositionMs: Long): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.setMediaSource(mediaSource, startPositionMs) },
			executor)

	override fun setMediaSource(mediaSource: MediaSource, resetPosition: Boolean): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.setMediaSource(mediaSource, resetPosition) },
			executor)

	override fun addMediaSource(mediaSource: MediaSource): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.addMediaSource(mediaSource) },
			executor)

	override fun addMediaSource(index: Int, mediaSource: MediaSource): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.addMediaSource(index, mediaSource) },
			executor)

	override fun addMediaSources(mediaSources: MutableList<MediaSource>): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.addMediaSources(mediaSources) },
			executor)

	override fun addMediaSources(index: Int, mediaSources: MutableList<MediaSource>): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.addMediaSources(index, mediaSources) },
			executor)

	override fun setShuffleOrder(shuffleOrder: ShuffleOrder): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.setShuffleOrder(shuffleOrder) },
			executor)

	override fun createMessage(target: PlayerMessage.Target): Promise<PlayerMessage> =
		QueuedPromise(
			MessageWriter{ innerPlayer.createMessage(target) },
			executor)

	override fun setSeekParameters(seekParameters: SeekParameters?): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.setSeekParameters(seekParameters) },
			executor)

	override fun getSeekParameters(): Promise<SeekParameters> =
		QueuedPromise(
			MessageWriter{ innerPlayer.seekParameters },
			executor)

	override fun setForegroundMode(foregroundMode: Boolean): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.setForegroundMode(foregroundMode) },
			executor)

	override fun setPauseAtEndOfMediaItems(pauseAtEndOfMediaItems: Boolean): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.pauseAtEndOfMediaItems = pauseAtEndOfMediaItems },
			executor)

	override fun getPauseAtEndOfMediaItems(): Promise<Boolean> =
		QueuedPromise(
			MessageWriter{ innerPlayer.pauseAtEndOfMediaItems },
			executor)

	override fun experimentalSetOffloadSchedulingEnabled(offloadSchedulingEnabled: Boolean): Promise<Unit> =
		QueuedPromise(
			MessageWriter{ innerPlayer.experimentalSetOffloadSchedulingEnabled(offloadSchedulingEnabled) },
			executor)
}
