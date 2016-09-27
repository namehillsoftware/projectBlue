package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.initialization.IPlaybackInitialization;

import java.util.List;

/**
 * Created by david on 9/26/16.
 */
public class PreparingMediaPlayerProvider implements IPreparingPlaybackFileProvider {
	private ConnectionProvider connectionProvider;
	private final List<IFile> playlist;
	private final IPlaybackInitialization<MediaPlayer> playbackInitialization;

	public PreparingMediaPlayerProvider(ConnectionProvider connectionProvider, List<IFile> playlist, IPlaybackInitialization<MediaPlayer> playbackInitialization) {
		this.connectionProvider = connectionProvider;
		this.playlist = playlist;
		this.playbackInitialization = playbackInitialization;
	}

	@Override
	public IPlaybackFilePreparer getPreparingPlaybackFile(int pos) {
		final IFile file = playlist.get(pos);
		final MediaPlayer mediaPlayer = playbackInitialization.initializeMediaPlayer(file.getPlaybackUrl(connectionProvider));
		return new MediaPlayerPreparer(mediaPlayer, file);
	}
}
