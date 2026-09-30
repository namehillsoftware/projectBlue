package com.lasthopesoftware.bluewater.client.playback.engine

import android.media.AudioManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC
import androidx.media3.common.C.USAGE_MEDIA
import androidx.media3.common.audio.AudioFocusRequestCompat
import androidx.media3.common.audio.AudioManagerCompat
import androidx.media3.common.util.UnstableApi
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.errors.PlaybackResourceNotAvailableInTimeException
import com.lasthopesoftware.bluewater.client.playback.volume.IVolumeManagement
import com.lasthopesoftware.bluewater.shared.android.audiofocus.ControlAudioFocus
import com.lasthopesoftware.promises.PromiseDelay.Companion.delay
import com.lasthopesoftware.promises.extensions.cancelBackThen
import com.lasthopesoftware.promises.extensions.unitResponse
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

@UnstableApi
class AudioManagingPlaybackStateChanger(
	private val innerPlaybackState: ChangePlaybackState,
	private val systemInducedPlaybackState: ChangePlaybackStateForSystem,
	private val audioFocus: ControlAudioFocus,
	private val volumeManager: IVolumeManagement
) :
	ChangePlaybackState by innerPlaybackState,
	AutoCloseable,
	AudioManager.OnAudioFocusChangeListener
{
	companion object {
		private val audioFocusTimeout by lazy { Duration.standardSeconds(10) }
	}

	private val lazyAudioRequest = lazy {
		AudioFocusRequestCompat
			.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
			.setAudioAttributes(
				AudioAttributes.Builder()
					.setContentType(AUDIO_CONTENT_TYPE_MUSIC)
					.setUsage(USAGE_MEDIA)
					.build())
			.setOnAudioFocusChangeListener(this)
			.setWillPauseWhenDucked(false)
			.build()
	}

	private val audioFocusSync = Any()
	private var audioFocusPromise: Promise<AudioFocusRequestCompat> = Promise.empty()
	private var isPlaying = false

	override fun startPlaylist(libraryId: LibraryId, playlist: List<ServiceFile>, playlistPosition: Int): Promise<Unit> {
		isPlaying = true
		return getNewAudioFocusRequest()
			.eventually { innerPlaybackState.startPlaylist(libraryId, playlist, playlistPosition) }
	}

	override fun resume(): Promise<Unit> {
		isPlaying = true
		return getNewAudioFocusRequest()
			.eventually { innerPlaybackState.resume() }
	}

	override fun pause(): Promise<Unit> {
		isPlaying = false
		return innerPlaybackState
				.pause()
				.eventually { abandonAudioFocus() }
				.unitResponse()
	}

	override fun onAudioFocusChange(focusChange: Int) {
		if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
			// resume playback
			volumeManager.setVolume(1.0f)
			if (!isPlaying) systemInducedPlaybackState.resume()
			isPlaying = true
			return
		}

		if (!isPlaying) return

		when (focusChange) {
			AudioManager.AUDIOFOCUS_LOSS -> {
				// Lost focus but it will not be regained, release resources
				isPlaying = false
				systemInducedPlaybackState.pause()
					.eventually { abandonAudioFocus() }
			}
			AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
				// Lost focus but it will be regained... cannot release resources
				isPlaying = false
				systemInducedPlaybackState.interrupt()
			}
			AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
				// Lost focus for a short time, but it's ok to keep playing at an attenuated level
				volumeManager.setVolume(0.2f)
		}
	}

	override fun close() {
		abandonAudioFocus()
	}

	private fun getNewAudioFocusRequest(): Promise<AudioFocusRequestCompat> =
		synchronized(audioFocusSync) {
			abandonAudioFocus()
				.eventually(
					{ getAudioFocusWithTimeout() },
					{ getAudioFocusWithTimeout() })
				.also { audioFocusPromise = it }
		}

	private fun getAudioFocusWithTimeout(): Promise<AudioFocusRequestCompat> {
		val audioFocusGranted = AtomicBoolean()
		val promisedAudioFocus = audioFocus
			.promiseAudioFocus(lazyAudioRequest.value)
			.cancelBackThen { compat, signal ->
				audioFocusGranted.set(!signal.isCancelled)
				compat
			}
		return Promise.whenAny(
			promisedAudioFocus,
			delay<Any?>(audioFocusTimeout).then { _ ->
				promisedAudioFocus.cancel()
				if (!audioFocusGranted.get())
					throw PlaybackResourceNotAvailableInTimeException("audio focus", audioFocusTimeout)
				null
			})
	}

	private fun abandonAudioFocus() =
		synchronized(audioFocusSync) {
			audioFocusPromise.cancel()
			audioFocusPromise.then {
				it?.also(audioFocus::abandonAudioFocus)
			}
		}
}
