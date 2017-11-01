package com.lasthopesoftware.bluewater.client.playback.queues.specs.GivenAStandardQueue.AndAFileThatFailsToPrepareForNoGoodReason;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.IPlaybackPreparer;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakePreparedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.queues.CompletingFileQueueProvider;
import com.lasthopesoftware.bluewater.client.playback.queues.PreparedPlaybackQueue;
import com.lasthopesoftware.messenger.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenTheQueueIsStarted {

	private static final Exception expectedException = new Exception();
	private static Throwable caughtException;
	private static IPlaybackHandler returnedPlaybackHandler;

	@BeforeClass
	public static void before() {

		final List<ServiceFile> serviceFiles =
			Stream
				.range(0, 2)
				.map(ServiceFile::new)
				.collect(Collectors.toList());

		final IPlaybackPreparer playbackPreparer = mock(IPlaybackPreparer.class);
		when(playbackPreparer.promisePreparedPlaybackHandler(new ServiceFile(0), 0))
			.thenReturn(new Promise<>(expectedException));

		when(playbackPreparer.promisePreparedPlaybackHandler(new ServiceFile(1), 0))
			.thenReturn(new Promise<>(new FakePreparedPlaybackFile<>(new FakeBufferingPlaybackHandler())));

		final CompletingFileQueueProvider bufferingPlaybackQueuesProvider
			= new CompletingFileQueueProvider();

		final int startPosition = 0;

		final PreparedPlaybackQueue queue = new PreparedPlaybackQueue(
			() -> 2,
			playbackPreparer,
			bufferingPlaybackQueuesProvider.provideQueue(serviceFiles, startPosition));

		queue.promiseNextPreparedPlaybackFile(0)
			.eventually(p -> queue.promiseNextPreparedPlaybackFile(0))
			.then(pf -> returnedPlaybackHandler = pf.getPlaybackHandler())
			.excuse(err -> {
				caughtException = err;

				return null;
			});
	}

	@Test
	public void thenTheExceptionIsCaught() {
		assertThat(caughtException).isEqualTo(expectedException);
	}

	@Test
	public void thenTheExpectedPlaybackHandlerIsNotReturned() {
		assertThat(returnedPlaybackHandler).isNull();
	}
}
