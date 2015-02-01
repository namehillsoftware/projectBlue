package com.lasthopesoftware.bluewater.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.lasthopesoftware.bluewater.data.service.objects.File;
import com.lasthopesoftware.bluewater.data.service.objects.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.file.IPlaybackFile;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.file.IPlaybackFileProvider;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.service.PlaybackController;
import com.lasthopesoftware.bluewater.test.mock.MockFilePlayer;

public class PlaybackControllerTest extends TestCase {

	private PlaybackController mPlaybackController;
	private IPlaybackFileProvider mPlaybackFileProvider;
	
	public PlaybackControllerTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		
		mPlaybackFileProvider = new IPlaybackFileProvider() {
			
			private ArrayList<IFile> mockFiles = new ArrayList<IFile>(Arrays.asList(new IFile[] { new File(1), new File(2), new File(3) })); 
			
			@Override
			public String toPlaylistString() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public int size() {
				return mockFiles.size();
			}
			
			@Override
			public IFile remove(int filePos) {
				return mockFiles.remove(filePos);
			}
						
			@Override
			public int indexOf(int startingIndex, IFile file) {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public int indexOf(IFile file) {
				// TODO Auto-generated method stub
				return mockFiles.indexOf(file);
			}
			
			@Override
			public IPlaybackFile getPlaybackFile(int filePos) {
				// TODO Auto-generated method stub
				return new MockFilePlayer();
			}
			
			@Override
			public List<IFile> getFiles() {
				return mockFiles;
			}
			
			@Override
			public IFile get(int filePos) {
				return mockFiles.get(filePos);
			}
						
			@Override
			public boolean add(IFile file) {
				return mockFiles.add(file);
			}
		};
		
		mPlaybackController = new PlaybackController(mPlaybackFileProvider);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public final void testPlaylistControllerContextArrayListOfFile() {
		fail("Not yet implemented"); // TODO
	}

	public final void testSeekToInt() {
		fail("Not yet implemented"); // TODO
	}

	public final void testSeekToIntInt() {
		fail("Not yet implemented"); // TODO
	}

	public final void testStartAtInt() {
		fail("Not yet implemented"); // TODO
	}

	public final void testStartAtIntInt() {
		fail("Not yet implemented"); // TODO
	}

	public final void testResume() {
		fail("Not yet implemented"); // TODO
	}

	public final void testPause() {
		fail("Not yet implemented"); // TODO
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

	public final void testRelease() {
		fail("Not yet implemented"); // TODO
	}

}
