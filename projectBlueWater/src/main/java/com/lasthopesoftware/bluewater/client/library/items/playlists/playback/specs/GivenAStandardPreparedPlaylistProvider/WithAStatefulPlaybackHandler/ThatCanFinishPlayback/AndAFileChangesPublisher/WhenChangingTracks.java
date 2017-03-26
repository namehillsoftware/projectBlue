package com.lasthopesoftware.bluewater.client.library.items.playlists.playback.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback.AndAFileChangesPublisher;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackServiceFile;
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

	private PositionedPlaybackServiceFile positionedPlaybackFile;
	private PositionedPlaybackServiceFile expectedPositionedPlaybackFile;

	@Before
	public void context() {
		final ResolveablePlaybackHandler playbackHandler = new ResolveablePlaybackHandler();
		final FakeBufferingPlaybackHandler playbackHandlerUnderTest = new FakeBufferingPlaybackHandler();

		final IPromise<PositionedPlaybackServiceFile> positionedPlaybackHandlerContainer =
			new Promise<>(new PositionedPlaybackServiceFile(0, playbackHandler, new ServiceFile(1)));

		final IPromise<PositionedPlaybackServiceFile> secondPositionedPlaybackHandlerContainer =
			new Promise<>((this.expectedPositionedPlaybackFile = new PositionedPlaybackServiceFile(0, playbackHandlerUnderTest, new ServiceFile(1))));

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
