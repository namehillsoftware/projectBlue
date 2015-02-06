package com.lasthopesoftware.bluewater.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.lasthopesoftware.bluewater.data.service.objects.File;
import com.lasthopesoftware.bluewater.data.service.objects.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.IPlaybackFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.IPlaybackFileProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackController;
import com.lasthopesoftware.bluewater.test.mock.MockFilePlayer;

public class PlaybackControllerTest extends TestCase {

	private PlaybackController mPlaybackController;
	private IPlaybackFileProvider mPlaybackFileProvider;
	private ArrayList<IFile> mMockFiles; 
	
	public PlaybackControllerTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		
		mMockFiles = new ArrayList<IFile>(
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
		Assert.assertEquals(2, mPlaybackController.getCurrentPosition());
		Assert.assertEquals(mMockFiles.get(2), mPlaybackController.getCurrentPlaybackFile().getFile());
	}

	public final void testSeekToIntInt() {
		final int filePosition = 10;
		mPlaybackController.seekTo(1, filePosition);
		Assert.assertEquals(filePosition, mPlaybackController.getCurrentPlaybackFile().getCurrentPosition());
	}

	public final void testStartAtInt() {
		mPlaybackController.startAt(0);
		Assert.assertTrue(mPlaybackController.isPlaying());
	}
	

	public final void testSeekWhilePlaying() {
		mPlaybackController.startAt(0);
		mPlaybackController.seekTo(2);
		Assert.assertEquals(mMockFiles.get(2), mPlaybackController.getCurrentPlaybackFile().getFile());
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

	public final void testSetVolume() {
		fail("Not yet implemented"); // TODO
	}

	public final void testSetIsRepeating() {
		fail("Not yet implemented"); // TODO
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
		Assert.assertEquals(fileIndex, mPlaybackController.getCurrentPosition());
	}
	
	public final void testRemoveLastFile() {
		final int lastFileIndex = mPlaybackFileProvider.size() - 1;
		mPlaybackController.seekTo(lastFileIndex);
		mPlaybackController.removeFile(lastFileIndex);
		Assert.assertEquals(lastFileIndex - 1, mPlaybackController.getCurrentPosition());
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
		public String getValue() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setValue(String mValue) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setProperty(String name, String value) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public String getProperty(String name) throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getRefreshedProperty(String name) throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getDuration() throws IOException {
			return 100;
		}
		
	}
}
