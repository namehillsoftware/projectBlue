package com.lasthopesoftware.bluewater.client.playback.queues.specs.GivenAStandardQueue;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.queues.CompletingFileQueueProvider;
import com.lasthopesoftware.bluewater.client.playback.queues.PreparedPlaybackQueue;
import com.lasthopesoftware.messenger.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by david on 3/2/17.
 */

public class WhenTheQueueIsClosed {

	private static boolean isCancelled;

	@BeforeClass
	public static void before() throws IOException {
		final CompletingFileQueueProvider bufferingPlaybackQueuesProvider = new CompletingFileQueueProvider();

		final Promise<PreparedPlaybackFile> cancelRecordingPromise = new Promise<>((messenger) -> {
			messenger.cancellationRequested(() -> isCancelled = true);
		});

		final PreparedPlaybackQueue queue =
			new PreparedPlaybackQueue(
				() -> 1,
				(file, preparedAt) -> cancelRecordingPromise,
				bufferingPlaybackQueuesProvider.provideQueue(Collections.singletonList(new ServiceFile(1)), 0));

		queue.promiseNextPreparedPlaybackFile(0);

		queue.close();
	}

	@Test
	public void thenThePreparedFilesAreCancelled() {
		assertThat(isCancelled).isTrue();
	}
}
