package com.lasthopesoftware.bluewater.client.playback.queues.specs.GivenAStandardQueue.AndASecondFileThatFailsToPrepare;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.IPlaybackPreparer;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.queues.CompletingFileQueueProvider;
import com.lasthopesoftware.bluewater.client.playback.queues.PreparedPlaybackQueue;
import com.lasthopesoftware.messenger.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenTheQueueIsStarted {

	private static TimeoutException timeoutException;
	private static boolean firstPromiseCancelled;

	@BeforeClass
	public static void before() {
		final Random random = new Random(System.currentTimeMillis());

		final List<ServiceFile> serviceFiles =
			Stream
				.range(0, 2)
				.map(i -> new ServiceFile(random.nextInt()))
				.collect(Collectors.toList());

		final IPlaybackPreparer playbackPreparer = mock(IPlaybackPreparer.class);
		when(playbackPreparer.promisePreparedPlaybackHandler(any(), anyInt()))
			.thenReturn(new Promise<>(new FakeBufferingPlaybackHandler()))
			.thenReturn(new Promise<>(messenger -> messenger.cancellationRequested(() -> firstPromiseCancelled = true)));

		final CompletingFileQueueProvider bufferingPlaybackQueuesProvider
			= new CompletingFileQueueProvider();

		final int startPosition = 0;

		final PreparedPlaybackQueue queue = new PreparedPlaybackQueue(
			() -> 2,
			playbackPreparer,
			bufferingPlaybackQueuesProvider.provideQueue(serviceFiles, startPosition));

		queue.promiseNextPreparedPlaybackFile(0)
			.eventually(p -> queue.promiseNextPreparedPlaybackFile(0))
			.excuse(e -> {
				if (e instanceof TimeoutException)
					timeoutException = (TimeoutException)e;

				return null;
			});
	}

	@Test
	public void thenATimeoutExceptionIsThrown() {
		assertThat(timeoutException).isNotNull();
	}

	@Test
	public void thenTheFirstPromiseIsCancelled() {
		assertThat(firstPromiseCancelled).isTrue();
	}
}
