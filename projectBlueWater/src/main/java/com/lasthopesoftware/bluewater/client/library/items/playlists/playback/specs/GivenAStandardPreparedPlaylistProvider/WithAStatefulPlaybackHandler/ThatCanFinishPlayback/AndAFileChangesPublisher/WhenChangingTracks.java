package com.lasthopesoftware.bluewater.client.library.items.playlists.playback.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback.AndAFileChangesPublisher;

import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPreparedPlaybackFileQueue;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.specs.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.PlaylistPlayer;
import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback.ResolveablePlaybackHandler;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.Promise;

import org.junit.Before;
import org.junit.Test;

import io.reactivex.Observable;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by david on 12/17/16.
 */

public class WhenChangingTracks {

	private PositionedPlaybackFile positionedPlaybackFile;
	private PositionedPlaybackFile expectedPositionedPlaybackFile;

	@Before
	public void context() {
		final ResolveablePlaybackHandler playbackHandler = new ResolveablePlaybackHandler();
		final FakeBufferingPlaybackHandler playbackHandlerUnderTest = new FakeBufferingPlaybackHandler();

		final IPromise<PositionedPlaybackFile> positionedPlaybackHandlerContainer =
			new Promise<>(new PositionedPlaybackFile(0, playbackHandler, new File(1)));

		final IPromise<PositionedPlaybackFile> secondPositionedPlaybackHandlerContainer =
			new Promise<>((this.expectedPositionedPlaybackFile = new PositionedPlaybackFile(0, playbackHandlerUnderTest, new File(1))));

		final IPreparedPlaybackFileQueue preparedPlaybackFileQueue = mock(IPreparedPlaybackFileQueue.class);
		when(preparedPlaybackFileQueue.promiseNextPreparedPlaybackFile(0))
			.thenReturn(positionedPlaybackHandlerContainer)
			.thenReturn(secondPositionedPlaybackHandlerContainer);

		Observable.create(new PlaylistPlayer(preparedPlaybackFileQueue, 0)).subscribe(positionedPlaybackFile -> this.positionedPlaybackFile = positionedPlaybackFile);

		playbackHandler.resolve();
	}

	@Test
	public void thenTheChangeCanBeObserved() {
		assertThat(this.positionedPlaybackFile).isEqualTo(this.expectedPositionedPlaybackFile);
	}
}
