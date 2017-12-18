package com.lasthopesoftware.bluewater.client.playback.playlist.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.volume.specs.fakes.FakeVolumeControllerFactory;
import com.lasthopesoftware.bluewater.client.playback.playlist.IPlaylistPlayer;
import com.lasthopesoftware.bluewater.client.playback.playlist.PlaylistPlayer;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.Before;
import org.junit.Test;

import io.reactivex.Observable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenChangingTheVolume {

	private FakeBufferingPlaybackHandler playbackHandlerUnderTest;

	@Before
	public void before() {
		final ResolveablePlaybackHandler playbackHandler = new ResolveablePlaybackHandler();
		playbackHandlerUnderTest = new FakeBufferingPlaybackHandler();

		final Promise<PositionedPlaybackFile> positionedPlaybackHandlerContainer =
			new Promise<>(new PositionedPlaybackFile(0, playbackHandler, new ServiceFile(1)));

		final Promise<PositionedPlaybackFile> secondPositionedPlaybackHandlerContainer =
			new Promise<>(new PositionedPlaybackFile(0, playbackHandlerUnderTest, new ServiceFile(1)));

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
		assertThat(playbackHandlerUnderTest.getVolume()).isEqualTo(0.8f);
	}
}
