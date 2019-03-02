package com.lasthopesoftware.bluewater.client.playback.playlist.playablefile.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue;
import com.lasthopesoftware.bluewater.client.playback.file.EmptyFileVolumeManager;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.volume.specs.fakes.FakeVolumeControllerFactory;
import com.lasthopesoftware.bluewater.client.playback.playlist.IPlaylistPlayer;
import com.lasthopesoftware.bluewater.client.playback.playlist.playablefile.PlayableFilePlayer;
import com.namehillsoftware.handoff.promises.Promise;
import io.reactivex.Observable;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenChangingTheVolume {

	private static final EmptyFileVolumeManager volumeManager = new EmptyFileVolumeManager();

	@BeforeClass
	public static void before() {
		final FakeBufferingPlaybackHandler playbackHandler = new FakeBufferingPlaybackHandler();
		playbackHandler.promisePlayback();

		final Promise<PositionedPlayableFile> positionedPlaybackHandlerContainer =
			new Promise<>(new PositionedPlayableFile(
				0,
				playbackHandler,
				volumeManager,
				new ServiceFile(1)));

		final PreparedPlayableFileQueue preparedPlaybackFileQueue = mock(PreparedPlayableFileQueue.class);
		when(preparedPlaybackFileQueue.promiseNextPreparedPlaybackFile(0))
			.thenReturn(positionedPlaybackHandlerContainer);

		final IPlaylistPlayer playlistPlayback = new PlayableFilePlayer(preparedPlaybackFileQueue, new FakeVolumeControllerFactory(), 0);
		Observable.create(playlistPlayback).subscribe();

		playlistPlayback.setVolume(0.8f);
	}

	@Test
	public void thenTheVolumeIsChanged() {
		assertThat(volumeManager.getVolume()).isEqualTo(0.8f);
	}
}
