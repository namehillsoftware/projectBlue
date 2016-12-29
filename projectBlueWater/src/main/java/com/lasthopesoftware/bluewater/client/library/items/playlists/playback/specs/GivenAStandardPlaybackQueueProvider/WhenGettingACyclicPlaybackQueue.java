package com.lasthopesoftware.bluewater.client.library.items.playlists.playback.specs.GivenAStandardPlaybackQueueProvider;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.BufferingPlaybackQueuesProvider;
import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.IPlaylistPlayerManager;
import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.PlaylistPlayerManager;
import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback.ResolveablePlaybackHandler;
import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by david on 12/17/16.
 */

public class WhenGettingACyclicPlaybackQueue {

	private static Collection<PositionedPlaybackFile> playedFiles;

	@BeforeClass
	public static void setup() throws IOException {
		final Map<IFile, ResolveablePlaybackHandler> resolveablePlaybackHandlers = new HashMap<>();

		final PlaylistPlayerManager playlistPlayerProducer =
			new PlaylistPlayerManager(new BufferingPlaybackQueuesProvider((file, preparedAt) -> {
				if (!resolveablePlaybackHandlers.containsKey(file)) {
					resolveablePlaybackHandlers.put(file, new ResolveablePlaybackHandler());
				}

				return new MockResolveAction(resolveablePlaybackHandlers.get(file));
			}));

		try {
			final IPlaylistPlayerManager playlistPlayerManager = playlistPlayerProducer.startAsCyclical(Arrays.asList(new File(1), new File(2), new File(3)), 0, 0);

			for (int j = 0; j < 2; j++) {
				for (int i = 1; i <= 3; i++) {
					if (j > 0) break;

					resolveablePlaybackHandlers.get(new File(i)).resolve();
				}
			}

			playlistPlayerManager
				.toList()
				.subscribe(positionedPlaybackFiles -> playedFiles = positionedPlaybackFiles);
		} finally {
			playlistPlayerProducer.close();
		}
	}

	@Test
	public void thenTheNewPlaybackQueuePlaysUntilCompletion() {
		assertThat(Stream.of(playedFiles).map(File::getKey).collect(Collectors.toList())).containsExactly(1, 2, 3, 1);
	}

	private static class MockResolveAction implements ThreeParameterAction<IResolvedPromise<IBufferingPlaybackHandler>, IRejectedPromise, OneParameterAction<Runnable>> {

		final ResolveablePlaybackHandler resolveablePlaybackHandler;

		MockResolveAction(ResolveablePlaybackHandler resolveablePlaybackHandler) {
			this.resolveablePlaybackHandler = resolveablePlaybackHandler;
		}

		@Override
		public void runWith(IResolvedPromise<IBufferingPlaybackHandler> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
			resolve.withResult(resolveablePlaybackHandler);
		}
	}
}
