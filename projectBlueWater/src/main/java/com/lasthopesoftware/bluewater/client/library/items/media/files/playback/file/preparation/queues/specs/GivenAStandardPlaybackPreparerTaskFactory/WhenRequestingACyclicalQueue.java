package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.specs.GivenAStandardPlaybackPreparerTaskFactory;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.IPlaybackPreparerTaskFactory;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.CyclicalQueuedPlaybackProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPreparedPlaybackFileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.PlaybackQueuesProvider;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.mockito.Mockito.mock;

/**
 * Created by david on 11/13/16.
 */

public class WhenRequestingACyclicalQueue {
	private IPreparedPlaybackFileProvider queue;

	@Before
	public void before() {
		final PlaybackQueuesProvider playbackQueuesProvider
			= new PlaybackQueuesProvider(mock(IPlaybackPreparerTaskFactory.class));

		queue = playbackQueuesProvider.getQueue(new ArrayList<>(), 0, true);
	}

	@Test
	public void thenACyclicalQueueIsProvided() {
		Assert.assertEquals(CyclicalQueuedPlaybackProvider.class, queue.getClass());
	}
}
