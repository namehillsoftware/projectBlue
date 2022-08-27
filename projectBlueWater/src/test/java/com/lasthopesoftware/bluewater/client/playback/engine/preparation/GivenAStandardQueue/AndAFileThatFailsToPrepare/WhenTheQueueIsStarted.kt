package com.lasthopesoftware.bluewater.client.playback.engine.preparation.GivenAStandardQueue.AndAFileThatFailsToPrepare;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparationException;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue;
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;
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

	private static final Exception expectedException = new Exception();
	private static PreparationException caughtException;
	private static PlayableFile returnedPlaybackHandler;

	@BeforeClass
	public static void before() {

		final List<ServiceFile> serviceFiles =
			Stream
				.range(0, 2)
				.map(ServiceFile::new)
				.collect(Collectors.toList());

		final PlayableFilePreparationSource playbackPreparer = mock(PlayableFilePreparationSource.class);
		when(playbackPreparer.promisePreparedPlaybackFile(new ServiceFile(0), Duration.ZERO))
			.thenReturn(new Promise<>(expectedException));

		when(playbackPreparer.promisePreparedPlaybackFile(new ServiceFile(1), Duration.ZERO))
			.thenReturn(new Promise<>(new FakePreparedPlayableFile<>(new FakeBufferingPlaybackHandler())));

		final CompletingFileQueueProvider bufferingPlaybackQueuesProvider
			= new CompletingFileQueueProvider();

		final int startPosition = 0;

		final PreparedPlayableFileQueue queue = new PreparedPlayableFileQueue(
			() -> 2,
			playbackPreparer,
			bufferingPlaybackQueuesProvider.provideQueue(serviceFiles, startPosition));

		queue.promiseNextPreparedPlaybackFile(Duration.ZERO)
			.eventually(p -> queue.promiseNextPreparedPlaybackFile(Duration.ZERO))
			.then(pf -> returnedPlaybackHandler = pf.getPlayableFile())
			.excuse(err -> {
				if (err instanceof PreparationException)
					caughtException = (PreparationException)err;

				return null;
			});
	}

	@Test
	public void thenThePositionedFileExceptionIsCaught() {
		assertThat(caughtException).hasCause(expectedException);
	}

	@Test
	public void thenThePositionedFileExceptionContainsThePositionedFile() {
		assertThat(caughtException.getPositionedFile()).isEqualTo(new PositionedFile(0, new ServiceFile(0)));
	}

	@Test
	public void thenTheExpectedPlaybackHandlerIsNotReturned() {
		assertThat(returnedPlaybackHandler).isNull();
	}
}
