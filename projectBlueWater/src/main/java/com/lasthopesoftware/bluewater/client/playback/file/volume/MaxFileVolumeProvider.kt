package com.lasthopesoftware.bluewater.client.playback.file.volume

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.settings.volumeleveling.IVolumeLevelSettings
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import java.util.concurrent.CancellationException
import kotlin.math.pow

class MaxFileVolumeProvider(
	private val volumeLevelSettings: IVolumeLevelSettings,
	private val libraryFileProperties: ProvideLibraryFileProperties,
) : ProvideMaxFileVolume {

	companion object {
		private val logger by lazyLogger<MaxFileVolumeProvider>()
		private const val UnityVolume = 1.0f
		private val promisedUnityVolume = UnityVolume.toPromise()
	}

	override fun promiseMaxFileVolume(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Float> = Promise.Proxy { cp ->
		volumeLevelSettings.isVolumeLevellingEnabled.eventually { isEnabled ->
			if (cp.isCancelled) throw CancellationException("Cancelled while computing volume leveling")
			if (!isEnabled) promisedUnityVolume
			else libraryFileProperties
				.promiseFileProperties(libraryId, serviceFile)
				.also(cp::doCancel)
				.then { fileProperties ->
					fileProperties[KnownFileProperties.VolumeLevelReplayGain]
						?.let { peakGainString ->
							// Formula based on Vanilla Player formula - https://github.com/vanilla-music/vanilla/blob/5eb97409ec4db866d5008ee92d9765bf7cf4ec8c/app/src/main/java/ch/blinkenlights/android/vanilla/PlaybackService.java#L758
							try {
								val peakGainVolumeLevel = peakGainString.toDouble()
								10.0.pow(peakGainVolumeLevel / 20.0).toFloat().coerceIn(0f, UnityVolume)
							} catch (e: NumberFormatException) {
								logger.info(
									"There was an error attempting to parse the given '${KnownFileProperties.VolumeLevelReplayGain}' level of $peakGainString.",
									e
								)
								UnityVolume
							}
						}
						?: UnityVolume
				}
		}
	}
}
