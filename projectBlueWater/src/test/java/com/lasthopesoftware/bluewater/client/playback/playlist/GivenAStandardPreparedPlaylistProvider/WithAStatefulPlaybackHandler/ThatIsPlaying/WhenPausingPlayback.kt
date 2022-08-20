package com.lasthopesoftware.bluewater.client.playback.playlist.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatIsPlaying;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.SupplyQueuedPreparedFiles;
import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.playlist.IPlaylistPlayer;
import com.lasthopesoftware.bluewater.client.playback.playlist.PlaylistPlayer;
import com.namehillsoftware.handoff.promises.Promise;

import org.joda.time.Duration;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenPausingPlayback {

	private static FakeBufferingPlaybackHandler playbackHandler;

	@BeforeClass
	public static void before() {
		playbackHandler = new FakeBufferingPlaybackHandler();

		final Promise<PositionedPlayableFile> positionedPlaybackHandlerContainer =
			new Promise<>(new PositionedPlayableFile(
				0,
				playbackHandler,
				new NoTransformVolumeManager(),
				new ServiceFile(1)));

		final SupplyQueuedPreparedFiles preparedPlaybackFileQueue = mock(SupplyQueuedPreparedFiles.class);
		when(preparedPlaybackFileQueue.promiseNextPreparedPlaybackFile(Duration.ZERO))
			.thenReturn(positionedPlaybackHandlerContainer);

		final IPlaylistPlayer playlistPlayback = new PlaylistPlayer(preparedPlaybackFileQueue, Duration.ZERO);

		playlistPlayback.pause();
	}

	@Test
	public void thenPlaybackIsPaused() {
		assertThat(playbackHandler.isPlaying()).isFalse();
	}
}
