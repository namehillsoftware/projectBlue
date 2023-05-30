/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lasthopesoftware.bluewater.client.playback.exoplayer

import android.net.Uri
import android.os.Handler
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.FormatHolder
import com.google.android.exoplayer2.ParserException
import com.google.android.exoplayer2.SeekParameters
import com.google.android.exoplayer2.decoder.DecoderInputBuffer
import com.google.android.exoplayer2.drm.DrmSessionEventListener
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.extractor.Extractor
import com.google.android.exoplayer2.extractor.ExtractorOutput
import com.google.android.exoplayer2.extractor.PositionHolder
import com.google.android.exoplayer2.extractor.SeekMap
import com.google.android.exoplayer2.extractor.SeekMap.Unseekable
import com.google.android.exoplayer2.extractor.TrackOutput
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.metadata.icy.IcyHeaders
import com.google.android.exoplayer2.source.LoadEventInfo
import com.google.android.exoplayer2.source.MediaLoadData
import com.google.android.exoplayer2.source.MediaPeriod
import com.google.android.exoplayer2.source.MediaSourceEventListener
import com.google.android.exoplayer2.source.ProgressiveMediaExtractor
import com.google.android.exoplayer2.source.SampleQueue
import com.google.android.exoplayer2.source.SampleQueue.UpstreamFormatChangedListener
import com.google.android.exoplayer2.source.SampleStream
import com.google.android.exoplayer2.source.SampleStream.ReadFlags
import com.google.android.exoplayer2.source.TrackGroup
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.ExoTrackSelection
import com.google.android.exoplayer2.upstream.Allocator
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSourceUtil
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.LoadErrorHandlingPolicy
import com.google.android.exoplayer2.upstream.LoadErrorHandlingPolicy.LoadErrorInfo
import com.google.android.exoplayer2.upstream.Loader
import com.google.android.exoplayer2.upstream.Loader.LoadErrorAction
import com.google.android.exoplayer2.upstream.StatsDataSource
import com.google.android.exoplayer2.util.Assertions
import com.google.android.exoplayer2.util.ConditionVariable
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.exoplayer.DavidsMediaPeriod.ExtractingLoadable
import org.checkerframework.checker.nullness.qual.EnsuresNonNull
import org.checkerframework.checker.nullness.qual.MonotonicNonNull
import java.io.IOException
import java.io.InterruptedIOException
import kotlin.math.max
import kotlin.math.min

/** A [MediaPeriod] that extracts data using an [Extractor].
 * @param uri The [Uri] of the media stream.
 * @param dataSource The data source to read the media.
 * @param progressiveMediaExtractor The [ProgressiveMediaExtractor] to use to read the data
 * source.
 * @param drmSessionManager A [DrmSessionManager] to allow DRM interactions.
 * @param drmEventDispatcher A dispatcher to notify of [DrmSessionEventListener] events.
 * @param loadErrorHandlingPolicy The [LoadErrorHandlingPolicy].
 * @param mediaSourceEventDispatcher A dispatcher to notify of [MediaSourceEventListener]
 * events.
 * @param listener A listener to notify when information about the period changes.
 * @param allocator An [Allocator] from which to obtain media buffer allocations.
 * @param continueLoadingCheckIntervalBytes The number of bytes that should be loaded between each
 * invocation of [Callback.onContinueLoadingRequested].
 */
internal class DavidsMediaPeriod(
	private val libraryId: LibraryId,
	private val uri: Uri,
	private val dataSource: DataSource,
	progressiveMediaExtractor: ProgressiveMediaExtractor,
	private val drmSessionManager: DrmSessionManager,
	private val drmEventDispatcher: DrmSessionEventListener.EventDispatcher,
	private val loadErrorHandlingPolicy: LoadErrorHandlingPolicy,
	private val mediaSourceEventDispatcher: MediaSourceEventListener.EventDispatcher,
	private val listener: Listener,
	private val allocator: Allocator,
	continueLoadingCheckIntervalBytes: Int
) : MediaPeriod, ExtractorOutput,
	Loader.Callback<ExtractingLoadable>, Loader.ReleaseCallback,
	UpstreamFormatChangedListener {
	/** Listener for information about the period.  */
	internal interface Listener {
		/**
		 * Called when the duration, the ability to seek within the period, or the categorization as
		 * live stream changes.
		 *
		 * @param durationUs The duration of the period, or [C.TIME_UNSET].
		 * @param isSeekable Whether the period is seekable.
		 * @param isLive Whether the period is live.
		 */
		fun onSourceInfoRefreshed(durationUs: Long, isSeekable: Boolean, isLive: Boolean)
	}

	private val continueLoadingCheckIntervalBytes: Long
	private val loader: Loader
	private val progressiveMediaExtractor: ProgressiveMediaExtractor
	private val loadCondition: ConditionVariable
	private val maybeFinishPrepareRunnable: Runnable
	private val onContinueLoadingRequestedRunnable: Runnable
	private val handler: Handler
	private var callback: MediaPeriod.Callback? = null
	private var icyHeaders: IcyHeaders? = null
	private var sampleQueues: Array<SampleQueue>
	private var sampleQueueTrackIds: Array<TrackId>
	private var sampleQueuesBuilt = false
	private var prepared = false
	private var haveAudioVideoTracks = false
	private var trackState: @MonotonicNonNull TrackState? = null
	private var seekMap: @MonotonicNonNull SeekMap? = null
	private var durationUs: Long
	private var isLive = false
	private var dataType: @C.DataType Int
	private var seenFirstTrackSelection = false
	private var notifyDiscontinuity = false
	private var enabledTrackCount = 0
	private var isLengthKnown = false
	private var lastSeekPositionUs: Long = 0
	private var pendingResetPositionUs: Long
	private var pendingDeferredRetry = false
	private var extractedSamplesCountAtStartOfLoad = 0
	private var loadingFinished = false
	private var released = false

	// maybeFinishPrepare is not posted to the handler until initialization completes.
	init {
		this.continueLoadingCheckIntervalBytes = continueLoadingCheckIntervalBytes.toLong()
		loader = Loader("ProgressiveMediaPeriod")
		this.progressiveMediaExtractor = progressiveMediaExtractor
		loadCondition = ConditionVariable()
		maybeFinishPrepareRunnable = Runnable { maybeFinishPrepare() }
		onContinueLoadingRequestedRunnable = Runnable {
			if (!released) {
				Assertions.checkNotNull(callback)
					.onContinueLoadingRequested(this@DavidsMediaPeriod)
			}
		}
		handler = Util.createHandlerForCurrentLooper()
		sampleQueueTrackIds = emptyArray()
		sampleQueues = emptyArray()
		pendingResetPositionUs = C.TIME_UNSET
		durationUs = C.TIME_UNSET
		dataType = C.DATA_TYPE_MEDIA
	}

	fun release() {
		if (prepared) {
			// Discard as much as we can synchronously. We only do this if we're prepared, since otherwise
			// sampleQueues may still be being modified by the loading thread.
			for (sampleQueue in sampleQueues) {
				sampleQueue.preRelease()
			}
		}
		loader.release(this)
		handler.removeCallbacksAndMessages(null)
		callback = null
		released = true
	}

	override fun onLoaderReleased() {
		for (sampleQueue in sampleQueues) {
			sampleQueue.release()
		}
		progressiveMediaExtractor.release()
	}

	override fun prepare(callback: MediaPeriod.Callback, positionUs: Long) {
		this.callback = callback
		loadCondition.open()
		startLoading()
	}

	override fun maybeThrowPrepareError() {
		maybeThrowError()
		if (loadingFinished && !prepared) {
			throw ParserException.createForMalformedContainer(
				"Loading finished before preparation is complete.", null
			)
		}
	}

	override fun getTrackGroups(): TrackGroupArray {
		assertPrepared()
		return trackState!!.tracks
	}

	override fun selectTracks(
		selections: Array<ExoTrackSelection>,
		mayRetainStreamFlags: BooleanArray,
		streams: Array<SampleStream>,
		streamResetFlags: BooleanArray,
		positionUs: Long
	): Long {
		var mutablePositionUs = positionUs
		assertPrepared()
		val tracks = trackState!!.tracks
		val trackEnabledStates = trackState!!.trackEnabledStates
		val oldEnabledTrackCount = enabledTrackCount
		val nullableStreams = streams.map { it as SampleStream? }.toTypedArray()
		// Deselect old tracks.
		for (i in selections.indices) {
			if (nullableStreams[i] != null && (selections[i] == null || !mayRetainStreamFlags[i])) {
				val track = (nullableStreams[i] as SampleStreamImpl).track
				Assertions.checkState(trackEnabledStates[track])
				enabledTrackCount--
				trackEnabledStates[track] = false
				nullableStreams[i] = null
			}
		}
		// We'll always need to seek if this is a first selection to a non-zero position, or if we're
		// making a selection having previously disabled all tracks.
		var seekRequired = if (seenFirstTrackSelection) oldEnabledTrackCount == 0 else mutablePositionUs != 0L
		// Select new tracks.
		for (i in selections.indices) {
			if (nullableStreams[i] == null && selections[i] != null) {
				val selection = selections[i]
				Assertions.checkState(selection.length() == 1)
				Assertions.checkState(selection.getIndexInTrackGroup(0) == 0)
				val track = tracks.indexOf(selection.trackGroup)
				Assertions.checkState(!trackEnabledStates[track])
				enabledTrackCount++
				trackEnabledStates[track] = true
				nullableStreams[i] = SampleStreamImpl(track)
				streamResetFlags[i] = true
				// If there's still a chance of avoiding a seek, try and seek within the sample queue.
				if (!seekRequired) {
					val sampleQueue = sampleQueues[track]
					// A seek can be avoided if we're able to seek to the current playback position in the
					// sample queue, or if we haven't read anything from the queue since the previous seek
					// (this case is common for sparse tracks such as metadata tracks). In all other cases a
					// seek is required.
					seekRequired = (!sampleQueue.seekTo(mutablePositionUs, true)
						&& sampleQueue.readIndex != 0)
				}
			}
		}
		if (enabledTrackCount == 0) {
			pendingDeferredRetry = false
			notifyDiscontinuity = false
			if (loader.isLoading) {
				// Discard as much as we can synchronously.
				for (sampleQueue in sampleQueues) {
					sampleQueue.discardToEnd()
				}
				loader.cancelLoading()
			} else {
				for (sampleQueue in sampleQueues) {
					sampleQueue.reset()
				}
			}
		} else if (seekRequired) {
			mutablePositionUs = seekToUs(mutablePositionUs)
			// We'll need to reset renderers consuming from all streams due to the seek.
			for (i in streams.indices) {
				if (nullableStreams[i] != null) {
					streamResetFlags[i] = true
				}
			}
		}
		seenFirstTrackSelection = true
		return mutablePositionUs
	}

	override fun discardBuffer(positionUs: Long, toKeyframe: Boolean) {
		assertPrepared()
		if (isPendingReset) {
			return
		}
		val trackEnabledStates = trackState!!.trackEnabledStates
		val trackCount = sampleQueues.size
		for (i in 0 until trackCount) {
			sampleQueues[i].discardTo(positionUs, toKeyframe, trackEnabledStates[i])
		}
	}

	override fun reevaluateBuffer(positionUs: Long) {
		// Do nothing.
	}

	override fun continueLoading(playbackPositionUs: Long): Boolean {
		if ((loadingFinished
				|| loader.hasFatalError()
				|| pendingDeferredRetry) || prepared && enabledTrackCount == 0
		) {
			return false
		}
		var continuedLoading = loadCondition.open()
		if (!loader.isLoading) {
			startLoading()
			continuedLoading = true
		}
		return continuedLoading
	}

	override fun isLoading(): Boolean {
		return loader.isLoading && loadCondition.isOpen
	}

	override fun getNextLoadPositionUs(): Long {
		return bufferedPositionUs
	}

	override fun readDiscontinuity(): Long {
		if (notifyDiscontinuity
			&& (loadingFinished || extractedSamplesCount > extractedSamplesCountAtStartOfLoad)
		) {
			notifyDiscontinuity = false
			return lastSeekPositionUs
		}
		return C.TIME_UNSET
	}

	override fun getBufferedPositionUs(): Long {
		assertPrepared()
		if (loadingFinished || enabledTrackCount == 0) {
			return C.TIME_END_OF_SOURCE
		} else if (isPendingReset) {
			return pendingResetPositionUs
		}
		var largestQueuedTimestampUs = Long.MAX_VALUE
		if (haveAudioVideoTracks) {
			// Ignore non-AV tracks, which may be sparse or poorly interleaved.
			val trackCount = sampleQueues.size
			for (i in 0 until trackCount) {
				if (trackState!!.trackIsAudioVideoFlags[i]
					&& trackState!!.trackEnabledStates[i]
					&& !sampleQueues[i].isLastSampleQueued
				) {
					largestQueuedTimestampUs =
						min(largestQueuedTimestampUs, sampleQueues[i].largestQueuedTimestampUs)
				}
			}
		}
		if (largestQueuedTimestampUs == Long.MAX_VALUE) {
			largestQueuedTimestampUs = getLargestQueuedTimestampUs( /* includeDisabledTracks= */false)
		}
		return if (largestQueuedTimestampUs == Long.MIN_VALUE) lastSeekPositionUs else largestQueuedTimestampUs
	}

	override fun seekToUs(positionUs: Long): Long {
		var mutablePositionUs = positionUs
		assertPrepared()
		val trackIsAudioVideoFlags = trackState!!.trackIsAudioVideoFlags
		// Treat all seeks into non-seekable media as being to t=0.
		mutablePositionUs = if (seekMap!!.isSeekable) mutablePositionUs else 0
		notifyDiscontinuity = false
		lastSeekPositionUs = mutablePositionUs
		if (isPendingReset) {
			// A reset is already pending. We only need to update its position.
			pendingResetPositionUs = mutablePositionUs
			return mutablePositionUs
		}

		// If we're not playing a live stream, try and seek within the buffer.
		if (dataType != C.DATA_TYPE_MEDIA_PROGRESSIVE_LIVE
			&& seekInsideBufferUs(trackIsAudioVideoFlags, mutablePositionUs)
		) {
			return mutablePositionUs
		}

		// We can't seek inside the buffer, and so need to reset.
		pendingDeferredRetry = false
		pendingResetPositionUs = mutablePositionUs
		loadingFinished = false
		if (loader.isLoading) {
			// Discard as much as we can synchronously.
			for (sampleQueue in sampleQueues) {
				sampleQueue.discardToEnd()
			}
			loader.cancelLoading()
		} else {
			loader.clearFatalError()
			for (sampleQueue in sampleQueues) {
				sampleQueue.reset()
			}
		}
		return mutablePositionUs
	}

	override fun getAdjustedSeekPositionUs(positionUs: Long, seekParameters: SeekParameters): Long {
		assertPrepared()
		if (!seekMap!!.isSeekable) {
			// Treat all seeks into non-seekable media as being to t=0.
			return 0
		}
		val seekPoints = seekMap!!.getSeekPoints(positionUs)
		return seekParameters.resolveSeekPositionUs(
			positionUs, seekPoints.first.timeUs, seekPoints.second.timeUs
		)
	}

	// SampleStream methods.
	/* package */
	fun isReady(track: Int): Boolean {
		return !suppressRead() && sampleQueues[track].isReady(loadingFinished)
	}

	/* package */
	@Throws(IOException::class)
	fun maybeThrowError(sampleQueueIndex: Int) {
		sampleQueues[sampleQueueIndex].maybeThrowError()
		maybeThrowError()
	}

	/* package */
	@Throws(IOException::class)
	fun maybeThrowError() {
		loader.maybeThrowError(loadErrorHandlingPolicy.getMinimumLoadableRetryCount(dataType))
	}

	/* package */
	fun readData(
		sampleQueueIndex: Int,
		formatHolder: FormatHolder?,
		buffer: DecoderInputBuffer?,
		readFlags: @ReadFlags Int
	): Int {
		if (suppressRead()) {
			return C.RESULT_NOTHING_READ
		}
		maybeNotifyDownstreamFormat(sampleQueueIndex)
		val result = sampleQueues[sampleQueueIndex].read(
			formatHolder!!,
			buffer!!, readFlags, loadingFinished
		)
		if (result == C.RESULT_NOTHING_READ) {
			maybeStartDeferredRetry(sampleQueueIndex)
		}
		return result
	}

	/* package */
	fun skipData(track: Int, positionUs: Long): Int {
		if (suppressRead()) {
			return 0
		}
		maybeNotifyDownstreamFormat(track)
		val sampleQueue = sampleQueues[track]
		val skipCount = sampleQueue.getSkipCount(positionUs, loadingFinished)
		sampleQueue.skip(skipCount)
		if (skipCount == 0) {
			maybeStartDeferredRetry(track)
		}
		return skipCount
	}

	private fun maybeNotifyDownstreamFormat(track: Int) {
		assertPrepared()
		val trackNotifiedDownstreamFormats = trackState!!.trackNotifiedDownstreamFormats
		if (!trackNotifiedDownstreamFormats[track]) {
			val trackFormat = trackState!!.tracks[track].getFormat(0)
			mediaSourceEventDispatcher.downstreamFormatChanged(
				MimeTypes.getTrackType(trackFormat.sampleMimeType),
				trackFormat,
				C.SELECTION_REASON_UNKNOWN,
				null,
				lastSeekPositionUs
			)
			trackNotifiedDownstreamFormats[track] = true
		}
	}

	private fun maybeStartDeferredRetry(track: Int) {
		assertPrepared()
		val trackIsAudioVideoFlags = trackState!!.trackIsAudioVideoFlags
		if (!pendingDeferredRetry
			|| !trackIsAudioVideoFlags[track]
			|| sampleQueues[track].isReady(false)
		) {
			return
		}
		pendingResetPositionUs = 0
		pendingDeferredRetry = false
		notifyDiscontinuity = true
		lastSeekPositionUs = 0
		extractedSamplesCountAtStartOfLoad = 0
		for (sampleQueue in sampleQueues) {
			sampleQueue.reset()
		}
		Assertions.checkNotNull(callback).onContinueLoadingRequested(this)
	}

	private fun suppressRead(): Boolean {
		return notifyDiscontinuity || isPendingReset
	}

	// Loader.Callback implementation.
	override fun onLoadCompleted(
		loadable: ExtractingLoadable, elapsedRealtimeMs: Long, loadDurationMs: Long
	) {
		if (durationUs == C.TIME_UNSET && seekMap != null) {
			val isSeekable = seekMap!!.isSeekable
			val largestQueuedTimestampUs = getLargestQueuedTimestampUs( /* includeDisabledTracks= */true)
			durationUs =
				if (largestQueuedTimestampUs == Long.MIN_VALUE) 0 else largestQueuedTimestampUs + DEFAULT_LAST_SAMPLE_DURATION_US
			listener.onSourceInfoRefreshed(durationUs, isSeekable, isLive)
		}
		val dataSource = loadable.dataSource
		val loadEventInfo = LoadEventInfo(
			loadable.loadTaskId,
			loadable.dataSpec,
			dataSource.lastOpenedUri,
			dataSource.lastResponseHeaders,
			elapsedRealtimeMs,
			loadDurationMs,
			dataSource.bytesRead
		)
		loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId)
		mediaSourceEventDispatcher.loadCompleted(
			loadEventInfo,
			C.DATA_TYPE_MEDIA,
			C.TRACK_TYPE_UNKNOWN,
			null,
			C.SELECTION_REASON_UNKNOWN,
			null,
			loadable.seekTimeUs,
			durationUs
		)
		loadingFinished = true
		Assertions.checkNotNull(callback).onContinueLoadingRequested(this)
	}

	override fun onLoadCanceled(
		loadable: ExtractingLoadable, elapsedRealtimeMs: Long, loadDurationMs: Long, released: Boolean
	) {
		val dataSource = loadable.dataSource
		val loadEventInfo = LoadEventInfo(
			loadable.loadTaskId,
			loadable.dataSpec,
			dataSource.lastOpenedUri,
			dataSource.lastResponseHeaders,
			elapsedRealtimeMs,
			loadDurationMs,
			dataSource.bytesRead
		)
		loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId)
		mediaSourceEventDispatcher.loadCanceled(
			loadEventInfo,
			C.DATA_TYPE_MEDIA,
			C.TRACK_TYPE_UNKNOWN,
			null,
			C.SELECTION_REASON_UNKNOWN,
			null,
			loadable.seekTimeUs,
			durationUs
		)
		if (!released) {
			for (sampleQueue in sampleQueues) {
				sampleQueue.reset()
			}
			if (enabledTrackCount > 0) {
				Assertions.checkNotNull(callback).onContinueLoadingRequested(this)
			}
		}
	}

	override fun onLoadError(
		loadable: ExtractingLoadable,
		elapsedRealtimeMs: Long,
		loadDurationMs: Long,
		error: IOException,
		errorCount: Int
	): LoadErrorAction {
		val dataSource = loadable.dataSource
		val loadEventInfo = LoadEventInfo(
			loadable.loadTaskId,
			loadable.dataSpec,
			dataSource.lastOpenedUri,
			dataSource.lastResponseHeaders,
			elapsedRealtimeMs,
			loadDurationMs,
			dataSource.bytesRead
		)
		val mediaLoadData = MediaLoadData(
			C.DATA_TYPE_MEDIA,
			C.TRACK_TYPE_UNKNOWN,
			null,
			C.SELECTION_REASON_UNKNOWN,
			null,
			Util.usToMs(loadable.seekTimeUs),
			Util.usToMs(durationUs)
		)
		val loadErrorAction: LoadErrorAction
		val retryDelayMs = loadErrorHandlingPolicy.getRetryDelayMsFor(
			LoadErrorInfo(loadEventInfo, mediaLoadData, error, errorCount)
		)
		loadErrorAction = if (retryDelayMs == C.TIME_UNSET) {
			Loader.DONT_RETRY_FATAL
		} else  /* the load should be retried */ {
			val extractedSamplesCount = extractedSamplesCount
			val madeProgress = extractedSamplesCount > extractedSamplesCountAtStartOfLoad
			if (configureRetry(loadable, extractedSamplesCount)) Loader.createRetryAction(
				madeProgress,
				retryDelayMs
			) else Loader.DONT_RETRY
		}
		val wasCanceled = !loadErrorAction.isRetry
		mediaSourceEventDispatcher.loadError(
			loadEventInfo,
			C.DATA_TYPE_MEDIA,
			C.TRACK_TYPE_UNKNOWN,
			null,
			C.SELECTION_REASON_UNKNOWN,
			null,
			loadable.seekTimeUs,
			durationUs,
			error,
			wasCanceled
		)
		if (wasCanceled) {
			loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId)
		}
		return loadErrorAction
	}

	// ExtractorOutput implementation. Called by the loading thread.
	override fun track(id: Int, type: Int): TrackOutput {
		return prepareTrackOutput(TrackId(id,  /* isIcyTrack= */false))
	}

	override fun endTracks() {
		sampleQueuesBuilt = true
		handler.post(maybeFinishPrepareRunnable)
	}

	override fun seekMap(seekMap: SeekMap) {
		handler.post { setSeekMap(seekMap) }
	}

	// UpstreamFormatChangedListener implementation. Called by the loading thread.
	override fun onUpstreamFormatChanged(format: Format) {
		handler.post(maybeFinishPrepareRunnable)
	}

	// Internal methods.
	private fun onLengthKnown() {
		handler.post { isLengthKnown = true }
	}

	private fun prepareTrackOutput(id: TrackId): TrackOutput {
		val trackCount = sampleQueues.size
		for (i in 0 until trackCount) {
			if (id == sampleQueueTrackIds[i]) {
				return sampleQueues[i]
			}
		}
		val trackOutput = SampleQueue.createWithDrm(allocator, drmSessionManager, drmEventDispatcher)
		trackOutput.setUpstreamFormatChangeListener(this)
		val sampleQueueTrackIds = sampleQueueTrackIds.copyOf(trackCount + 1)
		sampleQueueTrackIds[trackCount] = id
		this.sampleQueueTrackIds = sampleQueueTrackIds.filterNotNull().toTypedArray()
		val sampleQueues = sampleQueues.copyOf(trackCount + 1)
		sampleQueues[trackCount] = trackOutput
		this.sampleQueues = sampleQueues.filterNotNull().toTypedArray()
		return trackOutput
	}

	private fun setSeekMap(seekMap: SeekMap) {
		this.seekMap = if (icyHeaders == null) seekMap else Unseekable(C.TIME_UNSET)
		durationUs = seekMap.durationUs
		isLive = !isLengthKnown && seekMap.durationUs == C.TIME_UNSET
		dataType = if (isLive) C.DATA_TYPE_MEDIA_PROGRESSIVE_LIVE else C.DATA_TYPE_MEDIA
		listener.onSourceInfoRefreshed(durationUs, seekMap.isSeekable, isLive)
		if (!prepared) {
			maybeFinishPrepare()
		}
	}

	private fun maybeFinishPrepare() {
		if (released || prepared || !sampleQueuesBuilt || seekMap == null) {
			return
		}
		for (sampleQueue in sampleQueues) {
			if (sampleQueue.upstreamFormat == null) {
				return
			}
		}
		loadCondition.close()
		val trackCount = sampleQueues.size
		val trackArray = Array(trackCount) { TrackGroup() }
		val trackIsAudioVideoFlags = BooleanArray(trackCount)
		for (i in 0 until trackCount) {
			var trackFormat = Assertions.checkNotNull(
				sampleQueues[i].upstreamFormat
			)
			val mimeType = trackFormat.sampleMimeType
			val isAudio = MimeTypes.isAudio(mimeType)
			val isAudioVideo = isAudio || MimeTypes.isVideo(mimeType)
			trackIsAudioVideoFlags[i] = isAudioVideo
			haveAudioVideoTracks = haveAudioVideoTracks or isAudioVideo
			val icyHeaders = icyHeaders
			if (icyHeaders != null) {
				if (isAudio || sampleQueueTrackIds[i].isIcyTrack) {
					var metadata = trackFormat.metadata
					metadata = metadata?.copyWithAppendedEntries(icyHeaders) ?: Metadata(icyHeaders)
					trackFormat = trackFormat.buildUpon().setMetadata(metadata).build()
				}
				// Update the track format with the bitrate from the ICY header only if it declares neither
				// an average or peak bitrate of its own.
				if (isAudio && trackFormat.averageBitrate == Format.NO_VALUE && trackFormat.peakBitrate == Format.NO_VALUE && icyHeaders.bitrate != Format.NO_VALUE) {
					trackFormat = trackFormat.buildUpon().setAverageBitrate(icyHeaders.bitrate).build()
				}
			}
			trackFormat = trackFormat.copyWithCryptoType(drmSessionManager.getCryptoType(trackFormat))
			trackArray[i] = TrackGroup(i.toString(), trackFormat)
		}
		trackState = TrackState(TrackGroupArray(*trackArray), trackIsAudioVideoFlags)
		prepared = true
		Assertions.checkNotNull(callback).onPrepared(this)
	}

	private fun startLoading() {
		val loadable = ExtractingLoadable(
			libraryId,
			uri,
			dataSource,
			progressiveMediaExtractor,
			this,
			loadCondition
		)

		if (prepared) {
			Assertions.checkState(isPendingReset)
			if (durationUs != C.TIME_UNSET && pendingResetPositionUs > durationUs) {
				loadingFinished = true
				pendingResetPositionUs = C.TIME_UNSET
				return
			}
			loadable.setLoadPosition(
				Assertions.checkNotNull<@MonotonicNonNull SeekMap?>(seekMap)
					.getSeekPoints(pendingResetPositionUs).first.position,
				pendingResetPositionUs
			)
			for (sampleQueue in sampleQueues) {
				sampleQueue.setStartTimeUs(pendingResetPositionUs)
			}
			pendingResetPositionUs = C.TIME_UNSET
		}
		extractedSamplesCountAtStartOfLoad = extractedSamplesCount
		val elapsedRealtimeMs = loader.startLoading(
			loadable, this, loadErrorHandlingPolicy.getMinimumLoadableRetryCount(dataType)
		)
		val dataSpec = loadable.dataSpec
		mediaSourceEventDispatcher.loadStarted(
			LoadEventInfo(loadable.loadTaskId, dataSpec, elapsedRealtimeMs),
			C.DATA_TYPE_MEDIA,
			C.TRACK_TYPE_UNKNOWN,
			null,
			C.SELECTION_REASON_UNKNOWN,
			null,
			loadable.seekTimeUs,
			durationUs
		)
	}

	/**
	 * Called to configure a retry when a load error occurs.
	 *
	 * @param loadable The current loadable for which the error was encountered.
	 * @param currentExtractedSampleCount The current number of samples that have been extracted into
	 * the sample queues.
	 * @return Whether the loader should retry with the current loadable. False indicates a deferred
	 * retry.
	 */
	private fun configureRetry(loadable: ExtractingLoadable, currentExtractedSampleCount: Int): Boolean {
		return if (isLengthKnown || seekMap != null && seekMap!!.durationUs != C.TIME_UNSET) {
			// We're playing an on-demand stream. Resume the current loadable, which will
			// request data starting from the point it left off.
			extractedSamplesCountAtStartOfLoad = currentExtractedSampleCount
			true
		} else if (prepared && !suppressRead()) {
			// We're playing a stream of unknown length and duration. Assume it's live, and therefore that
			// the data at the uri is a continuously shifting window of the latest available media. For
			// this case there's no way to continue loading from where a previous load finished, so it's
			// necessary to load from the start whenever commencing a new load. Deferring the retry until
			// we run out of buffered data makes for a much better user experience. See:
			// https://github.com/google/ExoPlayer/issues/1606.
			// Note that the suppressRead() check means only a single deferred retry can occur without
			// progress being made. Any subsequent failures without progress will go through the else
			// block below.
			pendingDeferredRetry = true
			false
		} else {
			// This is the same case as above, except in this case there's no value in deferring the retry
			// because there's no buffered data to be read. This case also covers an on-demand stream with
			// unknown length that has yet to be prepared. This case cannot be disambiguated from the live
			// stream case, so we have no option but to load from the start.
			notifyDiscontinuity = prepared
			lastSeekPositionUs = 0
			extractedSamplesCountAtStartOfLoad = 0
			for (sampleQueue in sampleQueues) {
				sampleQueue.reset()
			}
			loadable.setLoadPosition(0, 0)
			true
		}
	}

	/**
	 * Attempts to seek to the specified position within the sample queues.
	 *
	 * @param trackIsAudioVideoFlags Whether each track is audio/video.
	 * @param positionUs The seek position in microseconds.
	 * @return Whether the in-buffer seek was successful.
	 */
	private fun seekInsideBufferUs(trackIsAudioVideoFlags: BooleanArray, positionUs: Long): Boolean {
		val trackCount = sampleQueues.size
		for (i in 0 until trackCount) {
			val sampleQueue = sampleQueues[i]
			val seekInsideQueue = sampleQueue.seekTo(positionUs, false)
			// If we have AV tracks then an in-buffer seek is successful if the seek into every AV queue
			// is successful. We ignore whether seeks within non-AV queues are successful in this case, as
			// they may be sparse or poorly interleaved. If we only have non-AV tracks then a seek is
			// successful only if the seek into every queue succeeds.
			if (!seekInsideQueue && (trackIsAudioVideoFlags[i] || !haveAudioVideoTracks)) {
				return false
			}
		}
		return true
	}

	private val extractedSamplesCount: Int
		get() {
			var extractedSamplesCount = 0
			for (sampleQueue in sampleQueues) {
				extractedSamplesCount += sampleQueue.writeIndex
			}
			return extractedSamplesCount
		}

	private fun getLargestQueuedTimestampUs(includeDisabledTracks: Boolean): Long {
		var largestQueuedTimestampUs = Long.MIN_VALUE
		for (i in sampleQueues.indices) {
			if (includeDisabledTracks || Assertions.checkNotNull(trackState).trackEnabledStates[i]) {
				largestQueuedTimestampUs = max(largestQueuedTimestampUs, sampleQueues[i].largestQueuedTimestampUs)
			}
		}
		return largestQueuedTimestampUs
	}

	private val isPendingReset: Boolean
		get() = pendingResetPositionUs != C.TIME_UNSET

	@EnsuresNonNull("trackState", "seekMap")
	private fun assertPrepared() {
		Assertions.checkState(prepared)
		Assertions.checkNotNull(trackState)
		Assertions.checkNotNull<@MonotonicNonNull SeekMap?>(seekMap)
	}

	private inner class SampleStreamImpl(val track: Int) : SampleStream {
		override fun isReady(): Boolean {
			return this@DavidsMediaPeriod.isReady(track)
		}

		override fun maybeThrowError() {
			this@DavidsMediaPeriod.maybeThrowError(track)
		}

		override fun readData(
			formatHolder: FormatHolder, buffer: DecoderInputBuffer, readFlags: @ReadFlags Int
		): Int {
			return this@DavidsMediaPeriod.readData(track, formatHolder, buffer, readFlags)
		}

		override fun skipData(positionUs: Long): Int {
			return this@DavidsMediaPeriod.skipData(track, positionUs)
		}
	}

	/** Loads the media stream and extracts sample data from it.  */ /* package */
	inner class ExtractingLoadable(
		private val libraryId: LibraryId,
		private val uri: Uri,
		dataSource: DataSource,
		progressiveMediaExtractor: ProgressiveMediaExtractor,
		extractorOutput: ExtractorOutput,
		loadCondition: ConditionVariable
	) : Loader.Loadable {
		val loadTaskId: Long
		val dataSource: StatsDataSource
		private val progressiveMediaExtractor: ProgressiveMediaExtractor
		private val extractorOutput: ExtractorOutput
		private val loadCondition: ConditionVariable
		private val positionHolder: PositionHolder

		@Volatile
		var loadCanceled = false
		private var pendingExtractorSeek: Boolean
		var seekTimeUs: Long = 0
		var dataSpec: DataSpec
		private var seenIcyMetadata = false

		init {
			this.dataSource = StatsDataSource(dataSource)
			this.progressiveMediaExtractor = progressiveMediaExtractor
			this.extractorOutput = extractorOutput
			this.loadCondition = loadCondition
			positionHolder = PositionHolder()
			pendingExtractorSeek = true
			loadTaskId = LoadEventInfo.getNewId()
			dataSpec = buildDataSpec( /* position= */0)
		}

		// Loadable implementation.
		override fun cancelLoad() {
			loadCanceled = true
		}

		override fun load() {
			var result = Extractor.RESULT_CONTINUE
			while (result == Extractor.RESULT_CONTINUE && !loadCanceled) {
				try {
					var position = positionHolder.position
					dataSpec = buildDataSpec(position)
					var length = dataSource.open(dataSpec)
					if (length != C.LENGTH_UNSET.toLong()) {
						length += position
						onLengthKnown()
					}
					icyHeaders = IcyHeaders.parse(dataSource.responseHeaders)
					val extractorDataSource = dataSource
					progressiveMediaExtractor.init(
						extractorDataSource,
						uri,
						dataSource.responseHeaders,
						position,
						length,
						extractorOutput
					)
					if (icyHeaders != null) {
						progressiveMediaExtractor.disableSeekingOnMp3Streams()
					}
					if (pendingExtractorSeek) {
						progressiveMediaExtractor.seek(position, seekTimeUs)
						pendingExtractorSeek = false
					}
					while (result == Extractor.RESULT_CONTINUE && !loadCanceled) {
						try {
							loadCondition.block()
						} catch (e: InterruptedException) {
							throw InterruptedIOException()
						}
						result = progressiveMediaExtractor.read(positionHolder)
						val currentInputPosition = progressiveMediaExtractor.currentInputPosition
						if (currentInputPosition > position + continueLoadingCheckIntervalBytes) {
							position = currentInputPosition
							loadCondition.close()
							handler.post(onContinueLoadingRequestedRunnable)
						}
					}
				} finally {
					if (result == Extractor.RESULT_SEEK) {
						result = Extractor.RESULT_CONTINUE
					} else if (progressiveMediaExtractor.currentInputPosition != C.POSITION_UNSET.toLong()) {
						positionHolder.position = progressiveMediaExtractor.currentInputPosition
					}
					DataSourceUtil.closeQuietly(dataSource)
				}
			}
		}

		// Internal methods.
		private fun buildDataSpec(position: Long): DataSpec {
			// Disable caching if the content length cannot be resolved, since this is indicative of a
			// progressive live stream.
			return DataSpec.Builder()
				.setUri(uri)
				.setPosition(position)
				.setFlags(
					DataSpec.FLAG_DONT_CACHE_IF_LENGTH_UNKNOWN or DataSpec.FLAG_ALLOW_CACHE_FRAGMENTATION
				)
				.setCustomData(libraryId)
				.build()
		}

		fun setLoadPosition(position: Long, timeUs: Long) {
			positionHolder.position = position
			seekTimeUs = timeUs
			pendingExtractorSeek = true
			seenIcyMetadata = false
		}
	}

	/** Stores track state.  */
	private class TrackState(val tracks: TrackGroupArray, val trackIsAudioVideoFlags: BooleanArray) {
		val trackEnabledStates: BooleanArray = BooleanArray(tracks.length)
		val trackNotifiedDownstreamFormats: BooleanArray = BooleanArray(tracks.length)
	}

	/** Identifies a track.  */
	private data class TrackId(val id: Int, val isIcyTrack: Boolean)

	companion object {
		/**
		 * When the source's duration is unknown, it is calculated by adding this value to the largest
		 * sample timestamp seen when buffering completes.
		 */
		private const val DEFAULT_LAST_SAMPLE_DURATION_US: Long = 10000
	}
}
