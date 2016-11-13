package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.specs.GivenAStandardPlaybackPreparerTaskFactory;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.IPlaybackPreparerTaskFactory;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPreparedPlaybackFileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.PlaybackQueuesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.QueuedPlaybackHandlerProvider;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.mockito.Mockito.mock;

/**
 * Created by david on 11/13/16.
 */

public class WhenRequestingANonCyclicalQueue {
	private IPreparedPlaybackFileProvider queue;

	@Before
	public void before() {
		final PlaybackQueuesProvider playbackQueuesProvider
			= new PlaybackQueuesProvider(mock(IPlaybackPreparerTaskFactory.class));

		queue = playbackQueuesProvider.getQueue(new ArrayList<>(), 0, false);
	}

	@Test
	public void thenANonCyclicalQueueIsProvided() {
		Assert.assertEquals(QueuedPlaybackHandlerProvider.class, queue.getClass());
	}
}
