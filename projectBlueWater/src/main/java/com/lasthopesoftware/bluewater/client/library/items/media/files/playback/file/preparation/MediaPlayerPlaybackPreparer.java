package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.initialization.IPlaybackInitialization;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.QueuedPromise;
import com.lasthopesoftware.promises.IPromise;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by david on 11/6/16.
 */
public class MediaPlayerPlaybackPreparer implements IPlaybackPreparer {

	private final static ExecutorService mediaPlayerPreparerExecutor = Executors.newSingleThreadExecutor();

	private final IFileUriProvider fileUriProvider;
	private final IPlaybackInitialization<MediaPlayer> playbackInitialization;

	public MediaPlayerPlaybackPreparer(IFileUriProvider fileUriProvider, IPlaybackInitialization<MediaPlayer> playbackInitialization) {
		this.fileUriProvider = fileUriProvider;
		this.playbackInitialization = playbackInitialization;
	}

	@Override
	public IPromise<IBufferingPlaybackHandler> promisePreparedPlaybackHandler(IFile file, int preparedAt) {
		return
			new QueuedPromise<>(
				new MediaPlayerPreparerTask(file, preparedAt, fileUriProvider, playbackInitialization),
				mediaPlayerPreparerExecutor);
	}
}
