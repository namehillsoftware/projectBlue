package com.lasthopesoftware.bluewater.client.playback.queues.specs.GivenTwoQueuesThatEventuallyDiverge;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.specs.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.queues.IPositionedFileQueue;
import com.lasthopesoftware.bluewater.client.playback.queues.PreparedPlaybackQueue;
import com.lasthopesoftware.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by david on 1/4/17.
 */

public class WhenSwitchingQueuesAndTheNextQueueIsEmpty {
	private static Promise<PositionedPlaybackFile> nextPreparedPlaybackFilePromise;

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

		final PreparedPlaybackQueue queue =
			new PreparedPlaybackQueue(
				(file, preparedAt) -> new Promise<>(new FakeBufferingPlaybackHandler()),
				positionedFileQueue);

		queue.promiseNextPreparedPlaybackFile(0);
		queue.promiseNextPreparedPlaybackFile(0);

		final IPositionedFileQueue newPositionedFileQueue = mock(IPositionedFileQueue.class);

		queue.updateQueue(newPositionedFileQueue);

		nextPreparedPlaybackFilePromise = queue.promiseNextPreparedPlaybackFile(0);
	}

	@Test
	public void thenTheQueueContinues() {
		assertThat(nextPreparedPlaybackFilePromise).isNull();
	}

}
