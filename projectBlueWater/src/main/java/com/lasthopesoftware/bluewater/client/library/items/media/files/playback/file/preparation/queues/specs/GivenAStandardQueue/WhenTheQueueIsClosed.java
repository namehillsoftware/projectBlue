package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.specs.GivenAStandardQueue;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.CompletingFileQueueProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.PreparedPlaybackQueue;
import com.lasthopesoftware.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Created by david on 3/2/17.
 */

public class WhenTheQueueIsClosed {

	private static final Promise<IBufferingPlaybackHandler> mockPromise = spy(new Promise<>(mock(IBufferingPlaybackHandler.class)));

	@BeforeClass
	public static void before() throws IOException {
		final CompletingFileQueueProvider bufferingPlaybackQueuesProvider = new CompletingFileQueueProvider();

		final PreparedPlaybackQueue queue =
			new PreparedPlaybackQueue(
				(file, preparedAt) -> mockPromise,
				bufferingPlaybackQueuesProvider.provideQueue(Collections.singletonList(new ServiceFile(1)), 0));

		queue.promiseNextPreparedPlaybackFile(0);

		queue.close();
	}

	@Test
	public void thenThePreparedFilesAreCancelled() {
		verify(mockPromise).cancel();
	}
}
