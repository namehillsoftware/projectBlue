package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import android.content.Context;
import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.initialization.IPlaybackInitialization;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.Promise;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Created by david on 9/26/16.
 */
public class PreparingMediaPlayerProvider implements IPreparedPlaybackFileProvider {
	private ConnectionProvider connectionProvider;
	private final Queue<IFile> playlist;
	private final IPlaybackInitialization<MediaPlayer> playbackInitialization;
	private Context context;
	private Library library;

	public PreparingMediaPlayerProvider(Context context, Library library, ConnectionProvider connectionProvider, List<IFile> playlist, IPlaybackInitialization<MediaPlayer> playbackInitialization) {
		this.library = library;
		this.connectionProvider = connectionProvider;
		this.playlist = new LinkedList<>(playlist);
		this.playbackInitialization = playbackInitialization;
		this.context = context;
	}

	@Override
	public IPromise<IPlaybackHandler> promiseNextPreparedPlaybackFile() throws IOException {
		final IFile file = playlist.poll();
		final BestMatchUriProvider bestMatchUriProvider = new BestMatchUriProvider(context, connectionProvider, library, file);
		final MediaPlayer mediaPlayer = playbackInitialization.initializeMediaPlayer(bestMatchUriProvider.getFileUri());
		return new Promise<>(new MediaPlayerPreparerTask(mediaPlayer));
	}
}
