package com.lasthopesoftware.bluewater.test.mock;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.IPlaybackFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.listeners.OnFileBufferedListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.listeners.OnFileCompleteListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.listeners.OnFileErrorListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.listeners.OnFilePreparedListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MockFilePlayer implements IPlaybackFile {
	private final IFile mFile;
	private boolean mIsMediaPlayerCreated = false;
	private boolean mIsPlaying = false;
	private boolean mIsPrepared = false;
	private int mPosition = 0;
	private float mVolume = 0f;
	
	private final List<OnFilePreparedListener> mOnFilePreparedListeners = new ArrayList<>();
	
	public MockFilePlayer(IFile file) {
		mFile = file;
	}
	
	@Override
	public IFile getFile() {
		return mFile;
	}

	@Override
	public void initMediaPlayer() {
		mIsMediaPlayerCreated = true;
	}

	@Override
	public boolean isMediaPlayerCreated() {
		return mIsMediaPlayerCreated;
	}

	@Override
	public boolean isPrepared() {
		return mIsPrepared;
	}

	@Override
	public void prepareMediaPlayer() {
		mIsPrepared = true;
		for (OnFilePreparedListener onFilePreparedListener : mOnFilePreparedListeners)
			onFilePreparedListener.onFilePrepared(this);
	}

	@Override
	public void prepareMpSynchronously() {
		mIsPrepared = true; 
	}

	@Override
	public void releaseMediaPlayer() {
		mIsPlaying = false;
		mIsPrepared = false;
		mIsMediaPlayerCreated = false;
	}

	@Override
	public int getCurrentPosition() {
		return mPosition;
	}

	@Override
	public boolean isBuffered() {
		return true;
	}

	@Override
	public int getBufferPercentage() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getDuration() throws IOException {
		return 100;
	}

	@Override
	public boolean isPlaying() {
		return mIsPlaying;
	}

	@Override
	public void pause() {
		mIsPlaying = false;
	}

	@Override
	public void seekTo(int pos) {
		mPosition = pos;
	}

	@Override
	public void start() {
		mIsPlaying = true;
	}

	@Override
	public void stop() {
		mIsPlaying = false;
		mPosition = 0;
	}

	@Override
	public float getVolume() {
		return mVolume;
	}

	@Override
	public void setVolume(float volume) {
		mVolume = volume;
	}

	@Override
	public void addOnFileCompleteListener(OnFileCompleteListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeOnFileCompleteListener(OnFileCompleteListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addOnFilePreparedListener(OnFilePreparedListener listener) {
		mOnFilePreparedListeners.add(listener);
	}

	@Override
	public void removeOnFilePreparedListener(OnFilePreparedListener listener) {
		mOnFilePreparedListeners.remove(listener);
	}

	@Override
	public void addOnFileErrorListener(OnFileErrorListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeOnFileErrorListener(OnFileErrorListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addOnFileBufferedListener(OnFileBufferedListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeOnFileErrorListener(OnFileBufferedListener listener) {
		// TODO Auto-generated method stub
		
	}

}
