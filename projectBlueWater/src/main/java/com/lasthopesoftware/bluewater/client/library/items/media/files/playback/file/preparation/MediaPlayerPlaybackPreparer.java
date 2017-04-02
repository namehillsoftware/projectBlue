package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.initialization.IPlaybackInitialization;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.QueuedPromise;
import com.lasthopesoftware.promises.Promise;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by david on 11/6/16.
 */
final class MediaPlayerPlaybackPreparer implements IPlaybackPreparer {

	private final static ExecutorService mediaPlayerPreparerExecutor = Executors.newSingleThreadExecutor();

	private final IFileUriProvider fileUriProvider;
	private final IPlaybackInitialization<MediaPlayer> playbackInitialization;

	MediaPlayerPlaybackPreparer(IFileUriProvider fileUriProvider, IPlaybackInitialization<MediaPlayer> playbackInitialization) {
		this.fileUriProvider = fileUriProvider;
		this.playbackInitialization = playbackInitialization;
	}

	@Override
	public Promise<IBufferingPlaybackHandler> promisePreparedPlaybackHandler(ServiceFile serviceFile, int preparedAt) {
		return
			new QueuedPromise<>(
				new MediaPlayerPreparerTask(serviceFile, preparedAt, fileUriProvider, playbackInitialization),
				mediaPlayerPreparerExecutor);
	}
}
