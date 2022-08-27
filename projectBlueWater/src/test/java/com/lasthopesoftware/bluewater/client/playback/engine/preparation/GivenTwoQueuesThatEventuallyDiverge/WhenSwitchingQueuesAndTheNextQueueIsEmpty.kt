package com.lasthopesoftware.bluewater.client.playback.engine.preparation.GivenTwoQueuesThatEventuallyDiverge;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakePreparedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.IPositionedFileQueue;
import com.namehillsoftware.handoff.promises.Promise;

import org.joda.time.Duration;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenSwitchingQueuesAndTheNextQueueIsEmpty {
	private static Promise<PositionedPlayableFile> nextPreparedPlaybackFilePromise;

	@BeforeClass
	public static void before() {
		final IPositionedFileQueue positionedFileQueue = mock(IPositionedFileQueue.class);
		when(positionedFileQueue.poll())
			.thenReturn(
				new PositionedFile(1, new ServiceFile(1)),
				new PositionedFile(2, new ServiceFile(2)),
				new PositionedFile(3, new ServiceFile(3)),
				new PositionedFile(4, new ServiceFile(4)),
				new PositionedFile(5, new ServiceFile(5)),
				null);

		final PreparedPlayableFileQueue queue =
			new PreparedPlayableFileQueue(
				() -> 1,
				(file, preparedAt) -> new Promise<>(new FakePreparedPlayableFile<>(new FakeBufferingPlaybackHandler())),
				positionedFileQueue);

		queue.promiseNextPreparedPlaybackFile(Duration.ZERO);
		queue.promiseNextPreparedPlaybackFile(Duration.ZERO);

		final IPositionedFileQueue newPositionedFileQueue = mock(IPositionedFileQueue.class);

		queue.updateQueue(newPositionedFileQueue);

		nextPreparedPlaybackFilePromise = queue.promiseNextPreparedPlaybackFile(Duration.ZERO);
	}

	@Test
	public void thenTheQueueContinues() {
		assertThat(nextPreparedPlaybackFilePromise).isNull();
	}

}
