package com.lasthopesoftware.bluewater.test;

import java.util.ArrayList;

import junit.framework.Assert;
import junit.framework.TestCase;
import android.test.mock.MockContext;

import com.lasthopesoftware.bluewater.data.service.helpers.playback.PlaylistController;
import com.lasthopesoftware.bluewater.data.service.objects.File;

public class PlaylistControllerTest extends TestCase {

	private PlaylistController mPlaylistController;
	
	public PlaylistControllerTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		
		MockContext mockContext = new MockContext();
		mPlaylistController = new PlaylistController(mockContext, new ArrayList<File>());
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

	public final void testAddFileInt() {
		fail("Not yet implemented"); // TODO
	}

	public final void testAddFileFile() {
		fail("Not yet implemented"); // TODO
	}

	public final void testRemoveFileAt() {
		fail("Not yet implemented"); // TODO
	}

	public final void testRemoveFile() {
		fail("Not yet implemented"); // TODO
	}

	public final void testGetPlaylist() {
		Class<?> parentClass = null, nextParentClass = mPlaylistController.getPlaylist().getClass();
		
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
