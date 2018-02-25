package com.lasthopesoftware.bluewater.client.playback.playlist.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback.AndAFileChangesPublisher;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue;
import com.lasthopesoftware.bluewater.client.playback.file.EmptyFileVolumeManager;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.volume.specs.fakes.FakeVolumeControllerFactory;
import com.lasthopesoftware.bluewater.client.playback.playlist.PlaylistPlayer;
import com.lasthopesoftware.bluewater.client.playback.playlist.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback.ResolveablePlaybackHandler;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.Before;
import org.junit.Test;

import io.reactivex.Observable;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenChangingTracks {

	private PositionedPlayableFile positionedPlayableFile;
	private PositionedPlayableFile expectedPositionedPlayableFile;

	@Before
	public void context() {
		final ResolveablePlaybackHandler playbackHandler = new ResolveablePlaybackHandler();
		final FakeBufferingPlaybackHandler playbackHandlerUnderTest = new FakeBufferingPlaybackHandler();

		final Promise<PositionedPlayableFile> positionedPlaybackHandlerContainer =
			new Promise<>(new PositionedPlayableFile(
				0,
				playbackHandler,
				new EmptyFileVolumeManager(),
				new ServiceFile(1)));

		final Promise<PositionedPlayableFile> secondPositionedPlaybackHandlerContainer =
			new Promise<>((this.expectedPositionedPlayableFile = new PositionedPlayableFile(
				0,
				playbackHandlerUnderTest,
				new EmptyFileVolumeManager(),
				new ServiceFile(1))));

		final PreparedPlayableFileQueue preparedPlaybackFileQueue = mock(PreparedPlayableFileQueue.class);
		when(preparedPlaybackFileQueue.promiseNextPreparedPlaybackFile(0))
			.thenReturn(positionedPlaybackHandlerContainer)
			.thenReturn(secondPositionedPlaybackHandlerContainer);

		Observable.create(new PlaylistPlayer(preparedPlaybackFileQueue, new FakeVolumeControllerFactory(), 0)).subscribe(positionedPlaybackFile -> this.positionedPlayableFile = positionedPlaybackFile);

		playbackHandler.resolve();
	}

	@Test
	public void thenTheChangeCanBeObserved() {
		assertThat(this.positionedPlayableFile).isEqualTo(this.expectedPositionedPlayableFile);
	}
}
