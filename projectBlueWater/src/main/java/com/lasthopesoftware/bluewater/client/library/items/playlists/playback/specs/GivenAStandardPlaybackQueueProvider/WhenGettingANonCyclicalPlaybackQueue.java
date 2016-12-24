package com.lasthopesoftware.bluewater.client.library.items.playlists.playback.specs.GivenAStandardPlaybackQueueProvider;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.BufferingPlaybackQueuesProvider;
import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.IPlaylistPlayer;
import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.PlaylistPlayerProducer;
import com.lasthopesoftware.promises.ExpectedPromise;
import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by david on 12/17/16.
 */

public class WhenGettingANonCyclicalPlaybackQueue {

	private static Collection<PositionedPlaybackFile> playedFiles;

	@BeforeClass
	public static void setup() {
		final PlaylistPlayerProducer playlistPlayerProducer =
			new PlaylistPlayerProducer(new BufferingPlaybackQueuesProvider((file, preparedAt) -> new MockResolveAction()));

		final IPlaylistPlayer playlistPlayer = playlistPlayerProducer.getPlaylistPlayer(Arrays.asList(new File(1), new File(2), new File(3)), 0, 0, false);
		playlistPlayer.toList().subscribe(positionedPlaybackFiles -> playedFiles = positionedPlaybackFiles);
	}

	@Test
	public void thenTheNewPlaybackQueuePlaysUntilCompletion() {
		assertThat(Stream.of(playedFiles).map(File::getKey).collect(Collectors.toList())).containsExactly(1, 2, 3);
	}

	private static class MockResolveAction implements ThreeParameterAction<IResolvedPromise<IBufferingPlaybackHandler>, IRejectedPromise, OneParameterAction<Runnable>> {
		@Override
		public void runWith(IResolvedPromise<IBufferingPlaybackHandler> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
			final IBufferingPlaybackHandler mock = mock(IBufferingPlaybackHandler.class);
			when(mock.bufferPlaybackFile()).thenReturn(new ExpectedPromise<>(() -> mock));
			when(mock.promisePlayback()).thenReturn(new ExpectedPromise<>(() -> mock));
			resolve.withResult(mock);
		}
	}
}
