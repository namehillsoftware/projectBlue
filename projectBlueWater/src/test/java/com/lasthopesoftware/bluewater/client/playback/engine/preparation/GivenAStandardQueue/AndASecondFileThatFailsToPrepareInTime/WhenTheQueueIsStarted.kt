package com.lasthopesoftware.bluewater.client.playback.engine.preparation.GivenAStandardQueue.AndASecondFileThatFailsToPrepareInTime;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue;
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakePreparedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider;
import com.namehillsoftware.handoff.promises.Promise;

import org.joda.time.Duration;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenTheQueueIsStarted {

	private static final FakeBufferingPlaybackHandler expectedPlaybackHandler = new FakeBufferingPlaybackHandler();
	private static boolean firstPromiseCancelled;
	private static PlayableFile returnedPlaybackHandler;

	@BeforeClass
	public static void before() {

		final List<ServiceFile> serviceFiles =
			Stream
				.range(0, 2)
				.map(ServiceFile::new)
				.toList();

		final PlayableFilePreparationSource playbackPreparer = mock(PlayableFilePreparationSource.class);
		when(playbackPreparer.promisePreparedPlaybackFile(new ServiceFile(0), Duration.ZERO))
			.thenReturn(new Promise<>(new FakePreparedPlayableFile<>(new FakeBufferingPlaybackHandler())));

		when(playbackPreparer.promisePreparedPlaybackFile(new ServiceFile(1), Duration.ZERO))
			.thenReturn(new Promise<>(messenger -> messenger.cancellationRequested(() -> firstPromiseCancelled = true)))
			.thenReturn(new Promise<>(new FakePreparedPlayableFile<>(expectedPlaybackHandler)));

		final CompletingFileQueueProvider bufferingPlaybackQueuesProvider
			= new CompletingFileQueueProvider();

		final int startPosition = 0;

		final PreparedPlayableFileQueue queue = new PreparedPlayableFileQueue(
			() -> 2,
			playbackPreparer,
			bufferingPlaybackQueuesProvider.provideQueue(serviceFiles, startPosition));

		queue.promiseNextPreparedPlaybackFile(Duration.ZERO)
			.eventually(p -> queue.promiseNextPreparedPlaybackFile(Duration.ZERO))
			.then(pf -> returnedPlaybackHandler = pf.getPlayableFile());
	}

	@Test
	public void thenTheExpectedPlaybackHandlerIsReturned() {
		assertThat(returnedPlaybackHandler).isEqualTo(expectedPlaybackHandler);
	}

	@Test
	public void thenTheFirstPromiseIsCancelled() {
		assertThat(firstPromiseCancelled).isTrue();
	}
}
