package com.lasthopesoftware.bluewater.client.library.items.playlists.playback.specs.GivenAStandardPlaybackQueueProvider;

import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.IPlaybackPreparerTaskFactory;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.BufferingPlaybackQueuesProvider;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by david on 12/22/16.
 */

public class WhenSwitchingBetweenACompletableAndACyclicQueueWhileAFileIsPlaying {

	private IPlaybackHandler playbackHandler;
	private IPlaybackHandler expectedPlaybackHandler;

	@BeforeClass
	public static void before() {

		final IPlaybackPreparerTaskFactory playbackPreparerTaskFactory =
			mock(IPlaybackPreparerTaskFactory.class);

		final ResolveablePlaybackHandler firstFilePlaybackHandler = new ResolveablePlaybackHandler();

		when(playbackPreparerTaskFactory.getPlaybackPreparerTask(new File(1), anyInt()))
			.thenReturn((resolve, reject, c) -> resolve.withResult(firstFilePlaybackHandler))
			.thenReturn((resolve, reject, c) -> resolve.withResult(new ResolveablePlaybackHandler()))
			.thenReturn((resolve, reject, c) -> resolve.withResult(new ResolveablePlaybackHandler()));

		final PlaylistPlayerManager playlistPlayerProducer =
			new PlaylistPlayerManager(new BufferingPlaybackQueuesProvider((file, preparedAt) -> new MockResolveAction()));

		playlistPlayerProducer.startAsCompletable(Arrays.asList(new File(1), new File(2), new File(3)), 0, 0);
		firstFilePlaybackHandler.resolve();
		playlistPlayerProducer.continueAsCyclical();
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

		final IBufferingPlaybackHandler resolveablePlaybackHandler = new ResolveablePlaybackHandler();

		@Override
		public void runWith(IResolvedPromise<IBufferingPlaybackHandler> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
			resolve.withResult(resolveablePlaybackHandler);
		}
	}
}
