package com.lasthopesoftware.bluewater.client.library.items.playlists.playback.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatIsPlaying.AndThenPaused;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPreparedPlaybackFileQueue;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.specs.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.IPlaylistPlayer;
import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.PlaylistPlayer;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.Promise;

import org.junit.Before;
import org.junit.Test;

import io.reactivex.Observable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by david on 11/12/16.
 */

public class WhenResumingPlayback {

	private IPlaybackHandler playbackHandler;

	@Before
	public void before() {
		playbackHandler = new FakeBufferingPlaybackHandler();

		final IPromise<PositionedPlaybackServiceFile> positionedPlaybackHandlerContainer =
			new Promise<>(new PositionedPlaybackServiceFile(0, playbackHandler, new ServiceFile(1)));

		final IPreparedPlaybackFileQueue preparedPlaybackFileQueue = mock(IPreparedPlaybackFileQueue.class);
		when(preparedPlaybackFileQueue.promiseNextPreparedPlaybackFile(0))
			.thenReturn(positionedPlaybackHandlerContainer);

		final IPlaylistPlayer playlistPlayback = new PlaylistPlayer(preparedPlaybackFileQueue, 0);

		Observable.create(playlistPlayback).subscribe();

		playlistPlayback.pause();

		playlistPlayback.resume();
	}

	@Test
	public void thenPlaybackIsResumed() {
		assertThat(playbackHandler.isPlaying()).isTrue();
	}
}
