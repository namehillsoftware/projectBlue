package com.lasthopesoftware.bluewater.client.playback.engine.preparation.specs.GivenACyclicQueue;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPreparedPlaybackQueue;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueue;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CyclicalFileQueueProvider;
import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.MessengerOperator;
import com.namehillsoftware.handoff.promises.Promise;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.namehillsoftware.handoff.promises.response.ImmediateAction.perform;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class WhenTheQueueIsStarted {
	private static IPreparedPlaybackQueue queue;
	private static int startPosition;

	@BeforeClass
	public static void before() {
		final Random random = new Random(System.currentTimeMillis());
		final int numberOfFiles = random.nextInt(500);

		final List<ServiceFile> serviceFiles =
			Stream
				.range(0, numberOfFiles)
				.map(i -> new ServiceFile(random.nextInt()))
				.collect(Collectors.toList());

		Map<ServiceFile, MockResolveAction> fileActionMap =
			Stream
				.of(serviceFiles)
				.collect(Collectors.toMap(file -> file, file -> spy(new MockResolveAction())));

		final CyclicalFileQueueProvider bufferingPlaybackQueuesProvider
			= new CyclicalFileQueueProvider();

		startPosition = random.nextInt(numberOfFiles);

		queue =
			new PreparedPlaybackQueue(
				() -> 1,
				(file, preparedAt) -> new Promise<>(fileActionMap.get(file)),
				bufferingPlaybackQueuesProvider.provideQueue(serviceFiles, startPosition));
	}

	@Test
	public void thenTheQueueStartsAtTheCorrectPosition() {
		queue
			.promiseNextPreparedPlaybackFile(0)
			.then(perform(positionedPlaybackFile -> Assert.assertEquals(startPosition, positionedPlaybackFile.getPlaylistPosition())));
	}

	private static class MockResolveAction implements MessengerOperator<PreparedPlaybackFile> {
		@Override
		public void send(Messenger<PreparedPlaybackFile> resolve) {
			resolve.sendResolution(mock(PreparedPlaybackFile.class));
		}
	}
}
