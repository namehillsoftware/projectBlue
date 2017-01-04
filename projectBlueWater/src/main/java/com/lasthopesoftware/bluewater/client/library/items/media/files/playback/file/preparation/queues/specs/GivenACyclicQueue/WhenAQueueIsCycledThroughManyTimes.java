package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.specs.GivenACyclicQueue;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPreparedPlaybackFileQueue;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.PositionedFileQueueProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.PreparedPlaybackQueue;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by david on 11/13/16.
 */

public class WhenAQueueIsCycledThroughManyTimes {

	private static Map<IFile, ThreeParameterAction<IResolvedPromise<IBufferingPlaybackHandler>, IRejectedPromise, OneParameterAction<Runnable>>> fileActionMap;
	private static int expectedNumberAbsolutePromises;
	private static int expectedCycles;
	private static int returnedPromiseCount;

	@BeforeClass
	public static void before() {

		final Random random = new Random(System.currentTimeMillis());
		final int numberOfFiles = random.nextInt(500);

		final List<IFile> files =
			Stream
				.range(0, numberOfFiles)
				.map(i -> new File(random.nextInt()))
				.collect(Collectors.toList());

		fileActionMap =
			Stream
				.of(files)
				.collect(Collectors.toMap(file -> file, file -> spy(new MockResolveAction())));

		final PositionedFileQueueProvider bufferingPlaybackQueuesProvider
			= new PositionedFileQueueProvider();

		final IPreparedPlaybackFileQueue queue =
			new PreparedPlaybackQueue(
				(file, preparedAt) -> fileActionMap.get(file),
				bufferingPlaybackQueuesProvider.getCyclicalQueue(files, 0));

		expectedCycles = random.nextInt(100);

		expectedNumberAbsolutePromises = expectedCycles * numberOfFiles;

		for (int i = 0; i < expectedNumberAbsolutePromises; i++) {
			final IPromise<PositionedPlaybackFile> positionedPlaybackFilePromise =
				queue.promiseNextPreparedPlaybackFile(0);

			if (positionedPlaybackFilePromise != null)
				++returnedPromiseCount;
		}
	}

	@Test
	public void thenEachFileIsPreparedTheAppropriateAmountOfTimes() {
		Stream.of(fileActionMap).forEach(entry -> verify(entry.getValue(), times(expectedCycles)).runWith(any(), any(), any()));
	}

	@Test
	public void thenTheCorrectNumberOfPromisesIsReturned() {
		Assert.assertEquals(expectedNumberAbsolutePromises, returnedPromiseCount);
	}

	private static class MockResolveAction implements ThreeParameterAction<IResolvedPromise<IBufferingPlaybackHandler>, IRejectedPromise, OneParameterAction<Runnable>> {
		@Override
		public void runWith(IResolvedPromise<IBufferingPlaybackHandler> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
			resolve.withResult(mock(IBufferingPlaybackHandler.class));
		}
	}
}
