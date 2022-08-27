package com.lasthopesoftware.bluewater.client.playback.engine.preparation.GivenTwoQueuesThatEventuallyDiverge;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue;
import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager;
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile;
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

public class WhenSwitchingQueuesAndGettingTheNextFileUntilTheQueuesDiverge {

	private static PositionedPlayableFile positionedPlayableFile;
	private static PositionedPlayableFile expectedPositionedPlayableFile;

	@BeforeClass
	public static void before() {
		expectedPositionedPlayableFile = new PositionedPlayableFile(
			6,
			mock(PlayableFile.class),
			new NoTransformVolumeManager(),
			new ServiceFile(6));

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
		when(newPositionedFileQueue.poll())
			.thenReturn(
				new PositionedFile(3, new ServiceFile(3)),
				new PositionedFile(6, new ServiceFile(6)),
				null);

		queue.updateQueue(newPositionedFileQueue);

		queue.promiseNextPreparedPlaybackFile(Duration.ZERO);
		queue.promiseNextPreparedPlaybackFile(Duration.ZERO).then(file -> positionedPlayableFile = file);
	}

	@Test
	public void thenTheQueueContinues() {
		assertThat(positionedPlayableFile).isEqualTo(expectedPositionedPlayableFile);
	}
}
