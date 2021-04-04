package com.lasthopesoftware.bluewater.client.playback.file.volume

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.CachedSessionFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.settings.volumeleveling.IVolumeLevelSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import org.slf4j.LoggerFactory
import kotlin.math.pow

class MaxFileVolumeProvider(private val volumeLevelSettings: IVolumeLevelSettings, private val cachedSessionFilePropertiesProvider: CachedSessionFilePropertiesProvider) : ProvideMaxFileVolume {

	companion object {
		private val logger = LoggerFactory.getLogger(MaxFileVolumeProvider::class.java)
		private const val UnityVolume = 1.0f
		private val promisedUnityVolume = UnityVolume.toPromise()
	}

	override fun promiseMaxFileVolume(serviceFile: ServiceFile): Promise<Float> {
		return if (!volumeLevelSettings.isVolumeLevellingEnabled) promisedUnityVolume else cachedSessionFilePropertiesProvider
			.promiseFileProperties(serviceFile)
			.then { fileProperties ->
				if (!fileProperties.containsKey(KnownFileProperties.VolumeLevelR128)) return@then UnityVolume

				val r128VolumeLevelString = fileProperties[KnownFileProperties.VolumeLevelR128] ?: return@then UnityVolume

				// Base formula on Vanilla Player formula - https://github.com/vanilla-music/vanilla/blob/5eb97409ec4db866d5008ee92d9765bf7cf4ec8c/app/src/main/java/ch/blinkenlights/android/vanilla/PlaybackService.java#L758
				try {
					val r128VolumeLevel = r128VolumeLevelString.toDouble()
					return@then 10.0.pow(r128VolumeLevel / 20.0).toFloat().coerceIn(0f, UnityVolume)
				} catch (e: NumberFormatException) {
					logger.info("There was an error attempting to parse the given R128 level of $r128VolumeLevelString.", e)
					UnityVolume
				}
			}
	}
}
