package com.lasthopesoftware.bluewater.client.playback.playlist.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback.AndAFileChangesPublisher;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.SupplyQueuedPreparedFiles;
import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile;
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.fakes.ResolvablePlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.playlist.PlaylistPlayer;
import com.namehillsoftware.handoff.promises.Promise;

import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;

import io.reactivex.Observable;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenChangingTracks {

	private PositionedPlayingFile positionedPlayingFile;
	private PositionedPlayableFile expectedPositionedPlayableFile;

	@Before
	public void context() {
		final ResolvablePlaybackHandler playbackHandler = new ResolvablePlaybackHandler();
		final FakeBufferingPlaybackHandler playbackHandlerUnderTest = new FakeBufferingPlaybackHandler();

		final Promise<PositionedPlayableFile> positionedPlaybackHandlerContainer =
			new Promise<>(new PositionedPlayableFile(
				0,
				playbackHandler,
				new NoTransformVolumeManager(),
				new ServiceFile(1)));

		final Promise<PositionedPlayableFile> secondPositionedPlaybackHandlerContainer =
			new Promise<>((this.expectedPositionedPlayableFile = new PositionedPlayableFile(
				0,
				playbackHandlerUnderTest,
				new NoTransformVolumeManager(),
				new ServiceFile(1))));

		final SupplyQueuedPreparedFiles preparedPlaybackFileQueue = mock(SupplyQueuedPreparedFiles.class);
		when(preparedPlaybackFileQueue.promiseNextPreparedPlaybackFile(Duration.ZERO))
			.thenReturn(positionedPlaybackHandlerContainer)
			.thenReturn(secondPositionedPlaybackHandlerContainer);

		Observable.create(new PlaylistPlayer(preparedPlaybackFileQueue, Duration.ZERO)).subscribe(positionedPlayingFile -> this.positionedPlayingFile = positionedPlayingFile);

		playbackHandler.resolve();
	}

	@Test
	public void thenTheChangeCanBeObserved() {
		assertThat(positionedPlayingFile.asPositionedFile())
			.isEqualTo(expectedPositionedPlayableFile.asPositionedFile());
	}
}
