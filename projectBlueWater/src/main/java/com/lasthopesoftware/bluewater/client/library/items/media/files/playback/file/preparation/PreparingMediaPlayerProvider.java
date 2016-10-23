package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import android.content.Context;
import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.initialization.IPlaybackInitialization;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.callables.OneParameterFunction;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Created by david on 9/26/16.
 */
public class PreparingMediaPlayerProvider implements
	IPreparedPlaybackFileProvider,
	OneParameterFunction<IBufferingPlaybackHandler, IPlaybackHandler>,
	OneParameterAction<IBufferingPlaybackHandler>
{
	private final ConnectionProvider connectionProvider;
	private final Queue<IFile> playlist;
	private final IPlaybackInitialization<MediaPlayer> playbackInitialization;
	private final Context context;
	private final Library library;

	private IPromise<IBufferingPlaybackHandler> nextPreparingMediaPlayerPromise;

	public PreparingMediaPlayerProvider(Context context, Library library, ConnectionProvider connectionProvider, List<IFile> playlist, IPlaybackInitialization<MediaPlayer> playbackInitialization) {
		this.library = library;
		this.connectionProvider = connectionProvider;
		this.playlist = new LinkedList<>(playlist);
		this.playbackInitialization = playbackInitialization;
		this.context = context;
	}

	@Override
	public IPromise<IPlaybackHandler> promiseNextPreparedPlaybackFile() {
		return
			(nextPreparingMediaPlayerPromise != null ?
				nextPreparingMediaPlayerPromise :
				getNextPreparingMediaPlayerPromise())
			.then((OneParameterFunction<IBufferingPlaybackHandler, IPlaybackHandler>) this);
	}

	private IPromise<IBufferingPlaybackHandler> getNextPreparingMediaPlayerPromise() {
		return
			new Promise<>(
				new MediaPlayerPreparerTask(
					new BestMatchUriProvider(context, connectionProvider, library, playlist.poll()),
					playbackInitialization));
	}

	@Override
	public IPlaybackHandler expectedUsing(IBufferingPlaybackHandler bufferingPlaybackHandler) {
		bufferingPlaybackHandler.bufferPlaybackFile().then((OneParameterAction<IBufferingPlaybackHandler>) this);

		return bufferingPlaybackHandler;
	}

	@Override
	public void runWith(IBufferingPlaybackHandler bufferingPlaybackHandler) {
		nextPreparingMediaPlayerPromise = getNextPreparingMediaPlayerPromise();
	}
}
