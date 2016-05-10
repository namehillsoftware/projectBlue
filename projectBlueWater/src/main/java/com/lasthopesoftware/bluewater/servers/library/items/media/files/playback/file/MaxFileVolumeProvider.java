package com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file;

import android.content.Context;
import android.preference.PreferenceManager;

import com.lasthopesoftware.bluewater.ApplicationConstants;
import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FilePropertiesProvider;
import com.vedsoft.fluent.FluentTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by david on 5/8/16.
 */
public class MaxFileVolumeProvider extends FluentTask<Void, Void, Float> {

	private static final Logger logger = LoggerFactory.getLogger(MaxFileVolumeProvider.class);

	private static final float MaxRelativeVolumeInDecibels = 23;

	private static final float MaxAbsoluteVolumeInDecibels = 89;

	private static final float MaxComputedVolumeInDecibels = MaxAbsoluteVolumeInDecibels + MaxRelativeVolumeInDecibels;

	private static final float UnityVolume = 1.0f;

	private final Context context;

	private final ConnectionProvider connectionProvider;

	private final IFile file;

	public MaxFileVolumeProvider(Context context, ConnectionProvider connectionProvider, IFile file) {
		this.context = context;
		this.connectionProvider = connectionProvider;
		this.file = file;
	}

	@Override
	protected Float executeInBackground(Void[] params) {
		final boolean isVolumeLevelingEnabled =
				PreferenceManager
						.getDefaultSharedPreferences(context)
						.getBoolean(ApplicationConstants.PreferenceConstants.isVolumeLevelingEnabled, false);

		if (!isVolumeLevelingEnabled)
			return UnityVolume;

		try {
			final CachedFilePropertiesProvider filePropertiesProvider = new CachedFilePropertiesProvider(connectionProvider, file.getKey());

			final Map<String, String> fileProperties = filePropertiesProvider.get();
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
		} catch (ExecutionException | InterruptedException e) {
			logger.info("There was an error getting the max file volume", e);
		}

		return UnityVolume;
	}
}
