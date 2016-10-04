package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import android.content.Context;
import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.initialization.IPlaybackInitialization;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;

import java.io.IOException;
import java.util.List;

/**
 * Created by david on 9/26/16.
 */
public class PreparingMediaPlayerProvider implements IPreparingPlaybackFileProvider {
	private ConnectionProvider connectionProvider;
	private final List<IFile> playlist;
	private final IPlaybackInitialization<MediaPlayer> playbackInitialization;
	private Context context;
	private Library library;

	public PreparingMediaPlayerProvider(Context context, Library library, ConnectionProvider connectionProvider, List<IFile> playlist, IPlaybackInitialization<MediaPlayer> playbackInitialization) {
		this.library = library;
		this.connectionProvider = connectionProvider;
		this.playlist = playlist;
		this.playbackInitialization = playbackInitialization;
		this.context = context;
	}

	@Override
	public IPlaybackFilePreparer getPreparingPlaybackFile(int pos) throws IOException {
		final IFile file = playlist.get(pos);
		final BestMatchUriProvider bestMatchUriProvider = new BestMatchUriProvider(context, connectionProvider, library, file);
		final MediaPlayer mediaPlayer = playbackInitialization.initializeMediaPlayer(bestMatchUriProvider.getFileUri());
		return new MediaPlayerPreparer(mediaPlayer);
	}
}
