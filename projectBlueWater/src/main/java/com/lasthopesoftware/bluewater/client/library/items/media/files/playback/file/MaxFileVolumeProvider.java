package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import android.content.Context;
import android.preference.PreferenceManager;

import com.lasthopesoftware.bluewater.ApplicationConstants;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.promises.Promise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by david on 5/8/16.
 */
public class MaxFileVolumeProvider {

	private static final Logger logger = LoggerFactory.getLogger(MaxFileVolumeProvider.class);

	private static final float MaxRelativeVolumeInDecibels = 23;

	private static final float MaxAbsoluteVolumeInDecibels = 89;

	private static final float MaxComputedVolumeInDecibels = MaxAbsoluteVolumeInDecibels + MaxRelativeVolumeInDecibels;

	private static final float UnityVolume = 1.0f;

	private static final Promise<Float> promisedUnityVolume = new Promise<>(UnityVolume);

	private final Context context;

	private final CachedFilePropertiesProvider cachedFilePropertiesProvider;

	public MaxFileVolumeProvider(Context context, CachedFilePropertiesProvider cachedFilePropertiesProvider) {
		this.context = context;
		this.cachedFilePropertiesProvider = cachedFilePropertiesProvider;
	}

	protected Promise<Float> getMaxFileVolume(ServiceFile serviceFile) {
		final boolean isVolumeLevelingEnabled =
				PreferenceManager
						.getDefaultSharedPreferences(context)
						.getBoolean(ApplicationConstants.PreferenceConstants.isVolumeLevelingEnabled, false);

		if (!isVolumeLevelingEnabled)
			return promisedUnityVolume;

		return
			cachedFilePropertiesProvider
				.promiseFileProperties(serviceFile.getKey())
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
