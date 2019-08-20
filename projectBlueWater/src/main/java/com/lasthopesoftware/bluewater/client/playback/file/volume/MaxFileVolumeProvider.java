package com.lasthopesoftware.bluewater.client.playback.file.volume;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.settings.volumeleveling.IVolumeLevelSettings;
import com.namehillsoftware.handoff.promises.Promise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaxFileVolumeProvider implements ProvideMaxFileVolume {

	private static final Logger logger = LoggerFactory.getLogger(MaxFileVolumeProvider.class);

	private static final float MaxRelativeVolumeInDecibels = 23;

	private static final float MaxAbsoluteVolumeInDecibels = 89;

	private static final float MaxComputedVolumeInDecibels = MaxAbsoluteVolumeInDecibels + MaxRelativeVolumeInDecibels;

	private static final float UnityVolume = 1.0f;

	private static final Promise<Float> promisedUnityVolume = new Promise<>(UnityVolume);

	private final CachedFilePropertiesProvider cachedFilePropertiesProvider;
	private final IVolumeLevelSettings volumeLevelSettings;

	public MaxFileVolumeProvider(IVolumeLevelSettings volumeLevelSettings, CachedFilePropertiesProvider cachedFilePropertiesProvider) {
		this.volumeLevelSettings = volumeLevelSettings;
		this.cachedFilePropertiesProvider = cachedFilePropertiesProvider;
	}

	@Override
	public Promise<Float> promiseMaxFileVolume(ServiceFile serviceFile) {
		if (!volumeLevelSettings.isVolumeLevellingEnabled())
			return promisedUnityVolume;

		return
			cachedFilePropertiesProvider
				.promiseFileProperties(serviceFile)
				.then(fileProperties -> {
					if (!fileProperties.containsKey(FilePropertiesProvider.VolumeLevelR128))
						return UnityVolume;

					final String r128VolumeLevelString = fileProperties.get(FilePropertiesProvider.VolumeLevelR128);
					try {
						final float r128VolumeLevel = Float.parseFloat(r128VolumeLevelString);

						final float normalizedVolumeLevel = MaxRelativeVolumeInDecibels - r128VolumeLevel;

						return Math.min(1 - (normalizedVolumeLevel / MaxComputedVolumeInDecibels), UnityVolume);
					} catch (NumberFormatException e) {
						logger.info("There was an error attempting to parse the given R128 level of " + r128VolumeLevelString + ".", e);
					}

					return UnityVolume;
				});
	}
}
