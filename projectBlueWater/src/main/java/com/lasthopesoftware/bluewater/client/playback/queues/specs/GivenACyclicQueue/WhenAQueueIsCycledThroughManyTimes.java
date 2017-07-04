package com.lasthopesoftware.bluewater.client.playback.queues.specs.GivenACyclicQueue;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.queues.CyclicalFileQueueProvider;
import com.lasthopesoftware.bluewater.client.playback.queues.IPreparedPlaybackFileQueue;
import com.lasthopesoftware.bluewater.client.playback.queues.PreparedPlaybackQueue;
import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.Promise;
import com.vedsoft.futures.runnables.OneParameterAction;

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

	private static Map<ServiceFile, OneParameterAction<Messenger<IBufferingPlaybackHandler>>> fileActionMap;
	private static int expectedNumberAbsolutePromises;
	private static int expectedCycles;
	private static int returnedPromiseCount;

	@BeforeClass
	public static void before() {

		final Random random = new Random(System.currentTimeMillis());
		final int numberOfFiles = random.nextInt(500);

		final List<ServiceFile> serviceFiles =
			Stream
				.range(0, numberOfFiles)
				.map(i -> new ServiceFile(random.nextInt()))
				.collect(Collectors.toList());

		fileActionMap =
			Stream
				.of(serviceFiles)
				.collect(Collectors.toMap(file -> file, file -> spy(new MockResolveAction())));

		final CyclicalFileQueueProvider bufferingPlaybackQueuesProvider
			= new CyclicalFileQueueProvider();

		final IPreparedPlaybackFileQueue queue =
			new PreparedPlaybackQueue(
				(file, preparedAt) -> new Promise<>(fileActionMap.get(file)),
				bufferingPlaybackQueuesProvider.provideQueue(serviceFiles, 0));

		expectedCycles = random.nextInt(100);

		expectedNumberAbsolutePromises = expectedCycles * numberOfFiles;

		for (int i = 0; i < expectedNumberAbsolutePromises; i++) {
			final Promise<PositionedPlaybackFile> positionedPlaybackFilePromise =
				queue.promiseNextPreparedPlaybackFile(0);

			if (positionedPlaybackFilePromise != null)
				++returnedPromiseCount;
		}
	}

	@Test
	public void thenEachFileIsPreparedTheAppropriateAmountOfTimes() {
		Stream.of(fileActionMap).forEach(entry -> verify(entry.getValue(), times(expectedCycles)).runWith(any()));
	}

	@Test
	public void thenTheCorrectNumberOfPromisesIsReturned() {
		Assert.assertEquals(expectedNumberAbsolutePromises, returnedPromiseCount);
	}

	private static class MockResolveAction implements OneParameterAction<Messenger<IBufferingPlaybackHandler>> {
		@Override
		public void runWith(Messenger<IBufferingPlaybackHandler> messenger) {
			messenger.sendResolution(mock(IBufferingPlaybackHandler.class));
		}
	}
}
