package com.lasthopesoftware.bluewater.client.playback.playlist.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback.AndAFileChangesPublisher;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.volume.specs.fakes.FakeVolumeControllerFactory;
import com.lasthopesoftware.bluewater.client.playback.playlist.PlaylistPlayer;
import com.lasthopesoftware.bluewater.client.playback.playlist.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback.ResolveablePlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.queues.IPreparedPlaybackFileQueue;
import com.namehillsoftware.handoff.promises.Promise;

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

		final Promise<PositionedPlaybackFile> positionedPlaybackHandlerContainer =
			new Promise<>(new PositionedPlaybackFile(0, playbackHandler, new ServiceFile(1)));

		final Promise<PositionedPlaybackFile> secondPositionedPlaybackHandlerContainer =
			new Promise<>((this.expectedPositionedPlaybackFile = new PositionedPlaybackFile(0, playbackHandlerUnderTest, new ServiceFile(1))));

		final IPreparedPlaybackFileQueue preparedPlaybackFileQueue = mock(IPreparedPlaybackFileQueue.class);
		when(preparedPlaybackFileQueue.promiseNextPreparedPlaybackFile(0))
			.thenReturn(positionedPlaybackHandlerContainer)
			.thenReturn(secondPositionedPlaybackHandlerContainer);

		Observable.create(new PlaylistPlayer(preparedPlaybackFileQueue, new FakeVolumeControllerFactory(), 0)).subscribe(positionedPlaybackFile -> this.positionedPlaybackFile = positionedPlaybackFile);

		playbackHandler.resolve();
	}

	@Test
	public void thenTheChangeCanBeObserved() {
		assertThat(this.positionedPlaybackFile).isEqualTo(this.expectedPositionedPlaybackFile);
	}
}
