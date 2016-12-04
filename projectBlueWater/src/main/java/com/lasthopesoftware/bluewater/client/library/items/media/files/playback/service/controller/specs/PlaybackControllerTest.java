package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller.specs;

import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackFileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller.PlaybackController;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller.specs.mock.MockFilePlayer;

import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlaybackControllerTest extends TestCase {

	private PlaybackController mPlaybackController;
	private IPlaybackFileProvider mPlaybackFileProvider;
	private ArrayList<IFile> mMockFiles; 
	
	public PlaybackControllerTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		
		mMockFiles = new ArrayList<>(
							Arrays.asList(
								new IFile[] { 
										new MockFile(1), 
										new MockFile(2), 
										new MockFile(3) 
							})); 
		
		mPlaybackFileProvider = new IPlaybackFileProvider() {
			
			@Override
			public String toPlaylistString() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public int size() {
				return mMockFiles.size();
			}
			
			@Override
			public IFile remove(int filePos) {
				return mMockFiles.remove(filePos);
			}
						
			@Override
			public int indexOf(int startingIndex, IFile file) {
				for (int i = startingIndex; i < mMockFiles.size(); i++)
					if (mMockFiles.get(i).equals(file)) return i;
				
				return -1;
			}
			
			@Override
			public int indexOf(IFile file) {
				return mMockFiles.indexOf(file);
			}
			
			@Override
			public IPlaybackFile getNewPlaybackFile(int filePos) {
				return new MockFilePlayer(mMockFiles.get(filePos));
			}
			
			@Override
			public List<IFile> getFiles() {
				return mMockFiles;
			}
			
			@Override
			public IFile get(int filePos) {
				return mMockFiles.get(filePos);
			}
						
			@Override
			public boolean add(IFile file) {
				return mMockFiles.add(file);
			}
		};
		
		mPlaybackController = new PlaybackController(mPlaybackFileProvider);
	}

	protected void tearDown() throws Exception {
		mPlaybackController.release();
	}

	public final void testSeekToInt() {
		mPlaybackController.seekTo(2);
		assertEquals(2, mPlaybackController.getCurrentPosition());
		assertEquals(mMockFiles.get(2), mPlaybackController.getCurrentPlaybackFile().getFile());
	}

	public final void testSeekToIntInt() {
		final int filePosition = 10;
		mPlaybackController.seekTo(1, filePosition);
		assertEquals(filePosition, mPlaybackController.getCurrentPlaybackFile().getCurrentPosition());
	}

	public final void testStartAtInt() {
		mPlaybackController.startAt(0);
		Assert.assertTrue(mPlaybackController.isPlaying());
	}
	

	public final void testSeekWhilePlaying() {
		mPlaybackController.startAt(0);
		mPlaybackController.seekTo(2);
		assertEquals(mMockFiles.get(2), mPlaybackController.getCurrentPlaybackFile().getFile());
		Assert.assertTrue(mPlaybackController.isPlaying());
	}

	public final void testResume() {
		mPlaybackController.startAt(0);
		mPlaybackController.pause();
		mPlaybackController.resume();
		Assert.assertTrue(mPlaybackController.isPlaying());
	}

	public final void testPause() {
		mPlaybackController.startAt(0);
		mPlaybackController.pause();
		Assert.assertFalse(mPlaybackController.isPlaying());
	}

	public final void testVolumeMaintainsStateAfterPlaybackFileChange() {
		final float testVolume = 0.5f;
		mPlaybackController.setVolume(testVolume);
		mPlaybackController.startAt(0);
		assertEquals(testVolume, mPlaybackController.getCurrentPlaybackFile().getVolume());
		mPlaybackController.seekTo(1);
		assertEquals(testVolume, mPlaybackController.getCurrentPlaybackFile().getVolume());
	}

	public final void testAddFile() {
		final File testFile = new File(5);
		final int originalSize = mPlaybackFileProvider.size();
		mPlaybackController.addFile(testFile);
		
		Assert.assertEquals(originalSize + 1, mPlaybackFileProvider.size());
		Assert.assertEquals(testFile, mPlaybackFileProvider.get(originalSize));
	}

	public final void testRemoveMiddleFile() {
		final int fileIndex = 1;
		mPlaybackController.seekTo(fileIndex);
		mPlaybackController.removeFile(fileIndex);
		assertEquals(fileIndex, mPlaybackController.getCurrentPosition());
	}
	
	public final void testRemoveLastFile() {
		final int lastFileIndex = mPlaybackFileProvider.size() - 1;
		mPlaybackController.seekTo(lastFileIndex);
		mPlaybackController.removeFile(lastFileIndex);
		assertEquals(lastFileIndex - 1, mPlaybackController.getCurrentPosition());
	}

	public final void testGetPlaylist() {
		Class<?> parentClass = null, nextParentClass = mPlaybackController.getPlaylist().getClass();
		
		while (nextParentClass != null && !nextParentClass.getSimpleName().equals("Object")) {
			parentClass = nextParentClass;
			nextParentClass = parentClass.getSuperclass();
		}
		
		Assert.assertEquals("getPlaylist should return an unmodifiable collection", "UnmodifiableCollection", parentClass.getSimpleName());
	}

	private static final class MockFile implements IFile {
		private int mKey;
		
		public MockFile(int key) {
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
		public String getPlaybackUrl(ConnectionProvider connectionProvider) {
			return null;
		}

		@Override
		public String[] getPlaybackParams() {
			return new String[0];
		}

		@Override
		public int compareTo(IFile another) {
			// TODO Auto-generated method stub
			return 0;
		}
		
	}
}
