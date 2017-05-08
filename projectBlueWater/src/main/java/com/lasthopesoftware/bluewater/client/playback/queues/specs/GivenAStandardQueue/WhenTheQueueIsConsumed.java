package com.lasthopesoftware.bluewater.client.playback.queues.specs.GivenAStandardQueue;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.queues.CompletingFileQueueProvider;
import com.lasthopesoftware.bluewater.client.playback.queues.IPreparedPlaybackFileQueue;
import com.lasthopesoftware.bluewater.client.playback.queues.PreparedPlaybackQueue;
import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class WhenTheQueueIsConsumed {

	private static Map<ServiceFile, MockResolveAction> fileActionMap;
	private static int returnedPromiseCount;
	private static int expectedNumberOfFiles;

	@BeforeClass
	public static void before() {

		final Random random = new Random(System.currentTimeMillis());
		expectedNumberOfFiles = random.nextInt(500);

		final List<ServiceFile> serviceFiles =
			Stream
				.range(0, expectedNumberOfFiles)
				.map(i -> new ServiceFile(random.nextInt()))
				.collect(Collectors.toList());

		fileActionMap =
			Stream
				.of(serviceFiles)
				.collect(Collectors.toMap(file -> file, file -> new MockResolveAction()));

		final CompletingFileQueueProvider bufferingPlaybackQueuesProvider
			= new CompletingFileQueueProvider();

		final IPreparedPlaybackFileQueue queue =
			new PreparedPlaybackQueue(
				(file, preparedAt) -> new Promise<>(fileActionMap.get(file)),
				bufferingPlaybackQueuesProvider.provideQueue(serviceFiles, 0));

		final int expectedCycles = random.nextInt(100);

		final int expectedNumberAbsolutePromises = expectedCycles * expectedNumberOfFiles;

		for (int i = 0; i < expectedNumberAbsolutePromises; i++) {
			final Promise<PositionedPlaybackFile> positionedPlaybackFilePromise =
				queue.promiseNextPreparedPlaybackFile(0);

			if (positionedPlaybackFilePromise != null)
				++returnedPromiseCount;
		}
	}

	@Test
	public void thenEachFileIsPreparedTheAppropriateAmountOfTimes() {
		assertThat(Stream.of(fileActionMap).map(entry -> entry.getValue().calls).distinct().collect(Collectors.toList())).containsExactly(1);
	}

	@Test
	public void thenTheCorrectNumberOfPromisesIsReturned() {
		assertThat(returnedPromiseCount).isEqualTo(expectedNumberOfFiles);
	}

	private static class MockResolveAction implements ThreeParameterAction<IResolvedPromise<IBufferingPlaybackHandler>, IRejectedPromise, OneParameterAction<Runnable>> {
		private int calls;

		@Override
		public void runWith(IResolvedPromise<IBufferingPlaybackHandler> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
			++calls;
			resolve.sendResolution(mock(IBufferingPlaybackHandler.class));
		}
	}
}
