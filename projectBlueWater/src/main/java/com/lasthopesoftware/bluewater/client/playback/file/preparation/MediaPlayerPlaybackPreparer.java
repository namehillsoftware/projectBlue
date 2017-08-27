package com.lasthopesoftware.bluewater.client.playback.file.preparation;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.initialization.IPlaybackInitialization;
import com.lasthopesoftware.messenger.promises.Promise;

final class MediaPlayerPlaybackPreparer implements IPlaybackPreparer {

	private final IFileUriProvider fileUriProvider;
	private final IPlaybackInitialization<MediaPlayer> playbackInitialization;

	MediaPlayerPlaybackPreparer(IFileUriProvider fileUriProvider, IPlaybackInitialization<MediaPlayer> playbackInitialization) {
		this.fileUriProvider = fileUriProvider;
		this.playbackInitialization = playbackInitialization;
	}

	@Override
	public Promise<IBufferingPlaybackHandler> promisePreparedPlaybackHandler(ServiceFile serviceFile, int preparedAt) {
		return
			fileUriProvider
				.getFileUri(serviceFile)
				.eventually(new MediaPlayerPreparerTask(preparedAt, playbackInitialization));
	}
}
