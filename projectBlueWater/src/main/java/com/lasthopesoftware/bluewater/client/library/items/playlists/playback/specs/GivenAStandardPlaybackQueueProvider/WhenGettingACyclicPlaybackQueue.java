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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by david on 12/17/16.
 */

public class WhenGettingACyclicPlaybackQueue {

	private static Collection<PositionedPlaybackFile> playedFiles;
	private static List<Integer> expectedGeneratedFileStream;

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
			final Random random = new Random();

			int numFiles;
			while ((numFiles = random.nextInt(10000)) <= 0);

			final IPlaylistPlayerManager playlistPlayerManager = playlistPlayerProducer.startAsCyclical(Stream.range(1, numFiles).map(File::new).collect(Collectors.toList()), 0, 0);

			int iterations;
			while ((iterations = random.nextInt(100)) <= 0);

			final int stopFile = random.nextInt(numFiles);

			expectedGeneratedFileStream = new ArrayList<>(iterations * numFiles);

			for (int j = 0; j <= iterations; j++) {
				for (int i = 1; i < numFiles; i++) {
					if (j >= iterations && i >= stopFile) {
						expectedGeneratedFileStream.add(i);
						break;
					}

					resolveablePlaybackHandlers.get(new File(i)).resolve();

					expectedGeneratedFileStream.add(i);
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
		assertThat(Stream.of(playedFiles).map(File::getKey).collect(Collectors.toList())).containsExactlyElementsOf(expectedGeneratedFileStream);
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
