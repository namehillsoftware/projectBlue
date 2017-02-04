package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.specs.GivenAStandardQueue;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPreparedPlaybackFileQueue;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.PositionedFileQueueProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.PreparedPlaybackQueue;
import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.callables.VoidFunc;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Created by david on 11/13/16.
 */

public class WhenTheQueueIsStarted {
	private static IPreparedPlaybackFileQueue queue;
	private static int startPosition;

	@BeforeClass
	public static void before() {
		final Random random = new Random(System.currentTimeMillis());
		final int numberOfFiles = random.nextInt(500);

		final List<IFile> files =
			Stream
				.range(0, numberOfFiles)
				.map(i -> new File(random.nextInt()))
				.collect(Collectors.toList());

		Map<IFile, MockResolveAction> fileActionMap =
			Stream
				.of(files)
				.collect(Collectors.toMap(file -> file, file -> spy(new MockResolveAction())));

		final PositionedFileQueueProvider bufferingPlaybackQueuesProvider
			= new PositionedFileQueueProvider();

		startPosition = random.nextInt(numberOfFiles);

		queue =
			new PreparedPlaybackQueue(
				(file, preparedAt) -> new Promise<>(fileActionMap.get(file)),
				bufferingPlaybackQueuesProvider.getCompletableQueue(files, startPosition));
	}

	@Test
	public void thenTheQueueStartsAtTheCorrectPosition() {
		queue
			.promiseNextPreparedPlaybackFile(0)
			.then(VoidFunc.runningCarelessly(positionedPlaybackFile -> Assert.assertEquals(startPosition, positionedPlaybackFile.getPosition())));
	}

	private static class MockResolveAction implements ThreeParameterAction<IResolvedPromise<IBufferingPlaybackHandler>, IRejectedPromise, OneParameterAction<Runnable>> {
		@Override
		public void runWith(IResolvedPromise<IBufferingPlaybackHandler> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
			resolve.withResult(mock(IBufferingPlaybackHandler.class));
		}
	}
}
