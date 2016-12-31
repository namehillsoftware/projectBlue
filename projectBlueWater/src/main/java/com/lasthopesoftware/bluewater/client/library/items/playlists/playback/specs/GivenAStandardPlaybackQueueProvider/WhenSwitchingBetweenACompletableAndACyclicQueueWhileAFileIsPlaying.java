package com.lasthopesoftware.bluewater.client.library.items.playlists.playback.specs.GivenAStandardPlaybackQueueProvider;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by david on 12/22/16.
 */

public class WhenSwitchingBetweenACompletableAndACyclicQueueWhileAFileIsPlaying {

	private IPlaybackHandler playbackHandler;
	private IPlaybackHandler expectedPlaybackHandler;

	@BeforeClass
	public static void before() {

		final Map<IFile, ResolveablePlaybackHandler> resolveablePlaybackHandlers = new HashMap<>();

		final PlaylistPlayerManager playlistPlayerProducer =
			new PlaylistPlayerManager(new BufferingPlaybackQueuesProvider((file, preparedAt) -> {
				if (!resolveablePlaybackHandlers.containsKey(file)) {
					resolveablePlaybackHandlers.put(file, new ResolveablePlaybackHandler());
				}

				return new MockResolveAction(resolveablePlaybackHandlers.get(file));
			}));

		final Random random = new Random();

		int numFiles;
		while ((numFiles = random.nextInt(10000)) < 10);

		final IPlaylistPlayerManager playlistPlayerManager = playlistPlayerProducer.startAsCompletable(Stream.range(1, numFiles).map(File::new).collect(Collectors.toList()), 0, 0);

		int switchPoint;
		while ((switchPoint = random.nextInt(numFiles)) <= 0);

		for (int i = 1; i <= switchPoint; i++) {
			resolveablePlaybackHandlers.get(new File(i)).resolve();
		}

		playlistPlayerManager.continueAsCyclical();

		int iterations;
		while ((iterations = random.nextInt(100)) <= 0);

		expectedGeneratedFileStream = new ArrayList<>(iterations * numFiles);

		for (int j = 0; j <= iterations; j++) {
			for (int i = 1; i < numFiles; i++) {
				if (j >= iterations && i >= switchPoint) {
					expectedGeneratedFileStream.add(i);
					break;
				}

				resolveablePlaybackHandlers.get(new File(i)).resolve();

				expectedGeneratedFileStream.add(i);
			}
		}

		playedFiles = new ArrayList<>(expectedGeneratedFileStream.size());

		playlistPlayerManager.subscribe(positionedPlaybackFile -> playedFiles.add(positionedPlaybackFile));
	}

	@Test
	public void thenThePlaybackOfTheCurrentFileIsNeverPaused() {
		verify(this.playbackHandler, times(0)).pause();
	}

	@Test
	public void thenTheCurrentPlaybackHandlerIsNeverClosed() throws IOException {
		verify(this.playbackHandler, times(0)).close();
	}

	@Test
	public void thenTheCurrentPlaybackHandlerIsPlaying() {
		assertThat(this.playbackHandler.isPlaying()).isTrue();
	}
	
	@Test
	public void thenThePlaybackHandlerRemainsTheSame() {
		assertThat(this.playbackHandler).isEqualTo(this.expectedPlaybackHandler);
	}

	private static class MockResolveAction implements ThreeParameterAction<IResolvedPromise<IBufferingPlaybackHandler>, IRejectedPromise, OneParameterAction<Runnable>> {

		final IBufferingPlaybackHandler resolveablePlaybackHandler;

		private MockResolveAction(IBufferingPlaybackHandler resolveablePlaybackHandler) {
			this.resolveablePlaybackHandler = resolveablePlaybackHandler;
		}

		@Override
		public void runWith(IResolvedPromise<IBufferingPlaybackHandler> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
			resolve.withResult(resolveablePlaybackHandler);
		}
	}
}
