package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller.test;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.PlaybackQueuesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller.PlaybackController;

import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;

import static org.mockito.Mockito.mock;

public class PlaybackControllerTest extends TestCase {

	private PlaybackController playbackController;
	private ArrayList<IFile> mockFiles;
	private ArrayList<IBufferingPlaybackHandler> bufferingPlaybackHandlers;
	private IBufferingPlaybackHandler bufferingPlaybackHandler;

	public PlaybackControllerTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		
		mockFiles =
			new ArrayList<>(
				Arrays.asList(
					new IFile[] {
						new MockFile(1),
						new MockFile(2),
						new MockFile(3)
				}));

		bufferingPlaybackHandlers =
			new ArrayList<>(
				Stream
					.of(mockFiles)
					.map(file -> mock(IBufferingPlaybackHandler.class))
					.collect(Collectors.toList()));

		playbackController =
			new PlaybackController(
				mockFiles,
				new PlaybackQueuesProvider(
					(file, preparedAt) -> (resolve, reject, onCancelled) -> {
						bufferingPlaybackHandler = bufferingPlaybackHandlers.get(mockFiles.indexOf(file));
						resolve.withResult(bufferingPlaybackHandler);
					}));
	}

	protected void tearDown() throws Exception {
		playbackController.release();
	}

	public final void testSeekToInt() {
		playbackController.seekTo(2);
		assertEquals(2, playbackController.getCurrentPosition());
		assertEquals(bufferingPlaybackHandlers.get(2), bufferingPlaybackHandler);
	}

	public final void testSeekToIntInt() {
		final int filePosition = 10;
		playbackController.seekTo(1, filePosition);
		assertEquals(filePosition, bufferingPlaybackHandler.getCurrentPosition());
	}

	public final void testStartAtInt() {
		playbackController.startAt(0);
		Assert.assertTrue(playbackController.isPlaying());
	}
	

	public final void testSeekWhilePlaying() {
		playbackController.startAt(0);
		playbackController.seekTo(2);
		assertEquals(bufferingPlaybackHandlers.get(2), bufferingPlaybackHandler);
		Assert.assertTrue(playbackController.isPlaying());
	}

	public final void testResume() {
		playbackController.startAt(0);
		playbackController.pause();
		playbackController.resume();
		Assert.assertTrue(playbackController.isPlaying());
	}

	public final void testPause() {
		playbackController.startAt(0);
		playbackController.pause();
		Assert.assertFalse(playbackController.isPlaying());
	}

	public final void testVolumeMaintainsStateAfterPlaybackFileChange() {
		final float testVolume = 0.5f;
		playbackController.setVolume(testVolume);
		playbackController.startAt(0);
		assertEquals(testVolume, bufferingPlaybackHandler.getVolume());
		playbackController.seekTo(1);
		assertEquals(testVolume, bufferingPlaybackHandler.getVolume());
	}
//
//	public final void testAddFile() {
//		final File testFile = new File(5);
//		final int originalSize = mPlaybackFileProvider.size();
//		playbackController.addFile(testFile);
//
//		Assert.assertEquals(originalSize + 1, mPlaybackFileProvider.size());
//		Assert.assertEquals(testFile, mPlaybackFileProvider.get(originalSize));
//	}

	public final void testRemoveMiddleFile() {
		final int fileIndex = 1;
		playbackController.seekTo(fileIndex);
		playbackController.removeFile(fileIndex);
		assertEquals(fileIndex, playbackController.getCurrentPosition());
	}
//
//	public final void testRemoveLastFile() {
//		final int lastFileIndex = mPlaybackFileProvider.size() - 1;
//		playbackController.seekTo(lastFileIndex);
//		playbackController.removeFile(lastFileIndex);
//		assertEquals(lastFileIndex - 1, playbackController.getCurrentPosition());
//	}

	public final void testGetPlaylist() {
		Class<?> parentClass = null, nextParentClass = playbackController.getPlaylist().getClass();
		
		while (nextParentClass != null && !nextParentClass.getSimpleName().equals("Object")) {
			parentClass = nextParentClass;
			nextParentClass = parentClass.getSuperclass();
		}
		
		Assert.assertEquals("getPlaylist should return an unmodifiable collection", "UnmodifiableCollection", parentClass.getSimpleName());
	}

	private static final class MockFile implements IFile {
		private int mKey;
		
		MockFile(int key) {
			mKey = key;
		}
		
		@Override
		public int getKey() {
			return mKey;
		}

		@Override
		public void setKey(int key) {
			mKey = key;
		}

		@Override
		public int compareTo(IFile another) {
			// TODO Auto-generated method stub
			return 0;
		}
		
	}
}
