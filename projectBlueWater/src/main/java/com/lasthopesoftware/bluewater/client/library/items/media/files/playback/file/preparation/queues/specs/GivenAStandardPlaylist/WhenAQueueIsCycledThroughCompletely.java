package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.specs.GivenAStandardPlaylist;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.PositionedFileContainer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.CyclicalQueuedPlaybackProvider;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by david on 11/13/16.
 */

public class WhenAQueueIsCycledThroughCompletely {

	private HashMap<IFile, ThreeParameterAction<IResolvedPromise<IBufferingPlaybackHandler>, IRejectedPromise, OneParameterAction<Runnable>>> fileActionMap;
	private int expectedNumberAbsolutePromises;
	private List<IPromise<PositionedPlaybackFile>> returnedPromises;
	private int expectedCycles;

	@Before
	public void before() {

		final Random random = new Random(System.currentTimeMillis());
		final int numberOfFiles = random.nextInt(500);

		fileActionMap = new HashMap<>(numberOfFiles);
		final List<PositionedFileContainer> fileContainers = new ArrayList<>(numberOfFiles);
		for (int i = 0; i < numberOfFiles; i++) {
			final IFile newFile = new File(random.nextInt());
			fileContainers.add(new PositionedFileContainer(i, newFile));
			final ThreeParameterAction<IResolvedPromise<IBufferingPlaybackHandler>, IRejectedPromise, OneParameterAction<Runnable>>
				action = new MockResolveAction();
			fileActionMap.put(newFile, spy(action));
		}

		final CyclicalQueuedPlaybackProvider cyclicalQueuedPlaybackProvider
			= new CyclicalQueuedPlaybackProvider(fileContainers, (file, preparedAt) -> fileActionMap.get(file));

		expectedCycles = random.nextInt(100);

		expectedNumberAbsolutePromises = expectedCycles * numberOfFiles;

		returnedPromises = new ArrayList<>(expectedNumberAbsolutePromises);
		for (int i = 0; i < expectedNumberAbsolutePromises; i++)
			returnedPromises.add(cyclicalQueuedPlaybackProvider.promiseNextPreparedPlaybackFile(0));
	}

	@Test
	public void thenEachFileIsPreparedTheAppropriateAmountOfTimes() {
		Stream.of(fileActionMap).forEach(entry -> verify(entry.getValue(), times(expectedCycles)).runWith(any(), any(), any()));
	}

	@Test
	public void thenTheCorrectNumberOfPromisesIsReturned() {
		Assert.assertEquals(expectedNumberAbsolutePromises, returnedPromises.size());
	}

	private static class MockResolveAction implements ThreeParameterAction<IResolvedPromise<IBufferingPlaybackHandler>, IRejectedPromise, OneParameterAction<Runnable>> {
		@Override
		public void runWith(IResolvedPromise<IBufferingPlaybackHandler> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
			resolve.withResult(mock(IBufferingPlaybackHandler.class));
		}
	}
}
