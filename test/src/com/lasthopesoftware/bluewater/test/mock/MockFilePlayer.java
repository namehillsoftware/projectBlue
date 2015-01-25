package com.lasthopesoftware.bluewater.test.mock;

import java.io.IOException;

import com.lasthopesoftware.bluewater.data.service.objects.File;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.IFilePlayer;

public class MockFilePlayer implements IFilePlayer {

	@Override
	public File getFile() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initMediaPlayer() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isMediaPlayerCreated() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isPrepared() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void prepareMediaPlayer() {
		// TODO Auto-generated method stub

	}

	@Override
	public void prepareMpSynchronously() {
		// TODO Auto-generated method stub

	}

	@Override
	public void releaseMediaPlayer() {
		// TODO Auto-generated method stub

	}

	@Override
	public int getCurrentPosition() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isBuffered() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getBufferPercentage() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getDuration() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isPlaying() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub

	}

	@Override
	public void seekTo(int pos) {
		// TODO Auto-generated method stub

	}

	@Override
	public void start() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	@Override
	public float getVolume() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setVolume(float volume) {
		// TODO Auto-generated method stub

	}

}
