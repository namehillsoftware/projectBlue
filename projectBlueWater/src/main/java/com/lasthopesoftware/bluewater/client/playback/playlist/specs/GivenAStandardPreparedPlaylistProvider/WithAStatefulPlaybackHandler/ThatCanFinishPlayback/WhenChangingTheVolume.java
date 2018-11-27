package com.lasthopesoftware.bluewater.client.playback.playlist.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue;
import com.lasthopesoftware.bluewater.client.playback.file.EmptyFileVolumeManager;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.volume.specs.fakes.FakeVolumeControllerFactory;
import com.lasthopesoftware.bluewater.client.playback.playlist.IPlaylistPlayer;
import com.lasthopesoftware.bluewater.client.playback.playlist.PlaylistPlayer;
import com.namehillsoftware.handoff.promises.Promise;
import io.reactivex.Observable;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenChangingTheVolume {

	private static final EmptyFileVolumeManager volumeManagerUnderTest = new EmptyFileVolumeManager();

	@BeforeClass
	public static void before() {
		final ResolveablePlaybackHandler playbackHandler = new ResolveablePlaybackHandler();
		final ResolveablePlaybackHandler secondPlaybackHandler = new ResolveablePlaybackHandler();

		final Promise<PositionedPlayableFile> positionedPlaybackHandlerContainer =
			new Promise<>(new PositionedPlayableFile(
				0,
				playbackHandler,
				new EmptyFileVolumeManager(),
				new ServiceFile(1)));

		final Promise<PositionedPlayableFile> secondPositionedPlaybackHandlerContainer =
			new Promise<>(new PositionedPlayableFile(
				0,
				secondPlaybackHandler,
				volumeManagerUnderTest,
				new ServiceFile(1)));

		final PreparedPlayableFileQueue preparedPlaybackFileQueue = mock(PreparedPlayableFileQueue.class);
		when(preparedPlaybackFileQueue.promiseNextPreparedPlaybackFile(0))
			.thenReturn(positionedPlaybackHandlerContainer)
			.thenReturn(secondPositionedPlaybackHandlerContainer);

		final IPlaylistPlayer playlistPlayback = new PlaylistPlayer(preparedPlaybackFileQueue, new FakeVolumeControllerFactory(), 0);

		Observable.create(playlistPlayback).subscribe();

		playlistPlayback.setVolume(0.8f);

		playbackHandler.resolve();
	}

	@Test
	public void thenTheVolumeIsChanged() {
		assertThat(volumeManagerUnderTest.getVolume()).isEqualTo(0.8f);
	}
}
