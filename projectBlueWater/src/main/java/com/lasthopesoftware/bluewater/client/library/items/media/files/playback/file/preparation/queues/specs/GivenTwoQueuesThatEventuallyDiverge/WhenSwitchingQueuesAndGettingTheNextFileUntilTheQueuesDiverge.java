package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.specs.GivenTwoQueuesThatEventuallyDiverge;

import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.PositionedFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPositionedFileQueue;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.PreparedPlaybackQueue;
import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.lasthopesoftware.promises.PassThroughPromise;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by david on 1/3/17.
 */

public class WhenSwitchingQueuesAndGettingTheNextFileUntilTheQueuesDiverge {

	private static PositionedPlaybackFile positionedPlaybackFile;
	private static PositionedPlaybackFile expectedPositionedPlaybackFile;

	@BeforeClass
	public static void before() {
		expectedPositionedPlaybackFile = new PositionedPlaybackFile(6, mock(IPlaybackHandler.class), new File(6));

		final IPositionedFileQueue positionedFileQueue = mock(IPositionedFileQueue.class);
		when(positionedFileQueue.poll())
			.thenReturn(
				new PositionedFile(1, new File(1)),
				new PositionedFile(2, new File(2)),
				new PositionedFile(3, new File(3)),
				new PositionedFile(4, new File(4)),
				new PositionedFile(5, new File(5)),
				null);

		final PreparedPlaybackQueue queue =
			new PreparedPlaybackQueue(
				(file, preparedAt) -> new PassThroughPromise<>(new FakeBufferingStatefulPlaybackHandler()),
				positionedFileQueue);

		queue.promiseNextPreparedPlaybackFile(0);
		queue.promiseNextPreparedPlaybackFile(0);

		final IPositionedFileQueue newPositionedFileQueue = mock(IPositionedFileQueue.class);
		when(newPositionedFileQueue.poll())
			.thenReturn(
				new PositionedFile(3, new File(3)),
				new PositionedFile(6, new File(6)),
				null);

		queue.updateQueue(newPositionedFileQueue);

		queue.promiseNextPreparedPlaybackFile(0);
		queue.promiseNextPreparedPlaybackFile(0).then(file -> positionedPlaybackFile = file);
	}

	@Test
	public void thenTheQueueContinues() {
		assertThat(positionedPlaybackFile).isEqualTo(expectedPositionedPlaybackFile);
	}

	private static class MockResolveAction implements ThreeParameterAction<IResolvedPromise<IBufferingPlaybackHandler>, IRejectedPromise, OneParameterAction<Runnable>> {

		@Override
		public void runWith(IResolvedPromise<IBufferingPlaybackHandler> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
			resolve.withResult(new FakeBufferingStatefulPlaybackHandler());
		}
	}

}
