package com.lasthopesoftware.bluewater.client.playback.file.preparation.mediaplayer;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.bluewater.client.playback.file.initialization.IPlaybackInitialization;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.IPlaybackPreparer;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlaybackFile;
import com.namehillsoftware.handoff.promises.Promise;

final class MediaPlayerPlaybackPreparer implements IPlaybackPreparer {

	private final IFileUriProvider fileUriProvider;
	private final IPlaybackInitialization<MediaPlayer> playbackInitialization;

	MediaPlayerPlaybackPreparer(IFileUriProvider fileUriProvider, IPlaybackInitialization<MediaPlayer> playbackInitialization) {
		this.fileUriProvider = fileUriProvider;
		this.playbackInitialization = playbackInitialization;
	}

	@Override
	public Promise<PreparedPlaybackFile> promisePreparedPlaybackHandler(ServiceFile serviceFile, long preparedAt) {
		return
			fileUriProvider
				.getFileUri(serviceFile)
				.eventually(new MediaPlayerPreparerTask(preparedAt, playbackInitialization));
	}
}
