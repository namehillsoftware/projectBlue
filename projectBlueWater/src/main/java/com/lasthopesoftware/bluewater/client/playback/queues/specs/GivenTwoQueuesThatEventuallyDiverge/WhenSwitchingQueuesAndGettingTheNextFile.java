package com.lasthopesoftware.bluewater.client.playback.queues.specs.GivenTwoQueuesThatEventuallyDiverge;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.queues.IPositionedFileQueue;
import com.lasthopesoftware.bluewater.client.playback.queues.PreparedPlaybackQueue;
import com.lasthopesoftware.messenger.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenSwitchingQueuesAndGettingTheNextFile {

	private static final List<PositionedPlaybackFile> playedFiles = new ArrayList<>();
	private static final List<PositionedPlaybackFile> expectedPositionedPlaybackFiles = Arrays.asList(
		new PositionedPlaybackFile(3, mock(IPlaybackHandler.class), new ServiceFile(3)),
		new PositionedPlaybackFile(4, mock(IPlaybackHandler.class), new ServiceFile(4)),
		new PositionedPlaybackFile(5, mock(IPlaybackHandler.class), new ServiceFile(6)),
		new PositionedPlaybackFile(6, mock(IPlaybackHandler.class), new ServiceFile(7)));

	@BeforeClass
	public static void before() throws InterruptedException {
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
				() -> 3,
				(file, preparedAt) -> new Promise<>(new FakeBufferingPlaybackHandler()),
				positionedFileQueue);

		queue.promiseNextPreparedPlaybackFile(0);
		queue.promiseNextPreparedPlaybackFile(0);

		final IPositionedFileQueue newPositionedFileQueue = mock(IPositionedFileQueue.class);
		when(newPositionedFileQueue.poll())
			.thenReturn(
				new PositionedFile(3, new ServiceFile(3)),
				new PositionedFile(4, new ServiceFile(4)),
				new PositionedFile(5, new ServiceFile(6)),
				new PositionedFile(6, new ServiceFile(7)),
				null);

		queue.updateQueue(newPositionedFileQueue);

		queue
			.promiseNextPreparedPlaybackFile(0)
			.eventually(file -> {
				playedFiles.add(file);
				return queue.promiseNextPreparedPlaybackFile(0);
			})
			.eventually(file -> {
				playedFiles.add(file);
				return queue.promiseNextPreparedPlaybackFile(0);
			})
			.eventually(file -> {
				playedFiles.add(file);
				return queue.promiseNextPreparedPlaybackFile(0);
			})
			.eventually(file -> {
				playedFiles.add(file);
				return Promise.empty();
			});
	}

	@Test
	public void thenTheQueueContinuesToCompletion() {
		assertThat(playedFiles).asList().containsExactlyElementsOf(expectedPositionedPlaybackFiles);
	}
}
