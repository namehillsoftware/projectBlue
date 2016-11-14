package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.specs.GivenAStandardPlaylist;

import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.PositionedFileContainer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.CyclicalQueuedPlaybackProvider;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.mockito.Mockito.mock;

/**
 * Created by david on 11/13/16.
 */

public class WhenAQueueIsCycledThroughCompletely {

	@Before
	public void before() {

		final Random random = new Random(System.currentTimeMillis());
		final int numberOfFiles = random.nextInt(500);

		final List<PositionedFileContainer> files = new ArrayList<>();
		for (int i = 0; i < numberOfFiles; i++)
			files.add(new PositionedFileContainer(i, new File(random.nextInt())));

		final CyclicalQueuedPlaybackProvider cyclicalQueuedPlaybackProvider
			= new CyclicalQueuedPlaybackProvider(files, (file, preparedAt) ->
				(resolve, reject, onCancelled) -> mock(IBufferingPlaybackHandler.class));
	}

	@Test
	public void thenEachFileIsPlayedTheAppropriateAmountOfTimes() {

	}
}
