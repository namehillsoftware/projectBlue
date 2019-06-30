package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.volume.ProvideMaxFileVolume;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.VoidResponse;

public class MaxFileVolumePreparer implements PlayableFilePreparationSource {

	private final PlayableFilePreparationSource playableFilePreparationSource;
	private final ProvideMaxFileVolume provideMaxFileVolume;

	public MaxFileVolumePreparer(PlayableFilePreparationSource playableFilePreparationSource, ProvideMaxFileVolume provideMaxFileVolume) {
		this.playableFilePreparationSource = playableFilePreparationSource;
		this.provideMaxFileVolume = provideMaxFileVolume;
	}

	@Override
	public Promise<PreparedPlayableFile> promisePreparedPlaybackFile(ServiceFile serviceFile, long preparedAt) {
		final Promise<Float> promisedMaxFileVolume = provideMaxFileVolume.promiseMaxFileVolume(serviceFile);

		return playableFilePreparationSource
			.promisePreparedPlaybackFile(serviceFile, preparedAt)
			.then(ppf -> {
				final MaxFileVolumeManager maxFileVolumeManager = new MaxFileVolumeManager(ppf.getPlayableFileVolumeManager());

				promisedMaxFileVolume
					.then(new VoidResponse<>(maxFileVolumeManager::setMaxFileVolume));

				return new PreparedPlayableFile(
					ppf.getPlaybackHandler(),
					maxFileVolumeManager,
					ppf.getBufferingPlaybackFile());
			});
	}
}
