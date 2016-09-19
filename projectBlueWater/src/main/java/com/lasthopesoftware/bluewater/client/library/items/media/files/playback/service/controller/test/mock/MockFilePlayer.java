package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller.test.mock;

import android.support.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackFilePreparation;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.listeners.OnFileBufferedListener;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.listeners.OnFileCompleteListener;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.listeners.OnFileErrorListener;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.listeners.OnFilePreparedListener;

public class MockFilePlayer implements IPlaybackFile {
	private final IFile mFile;
	private boolean mIsMediaPlayerCreated = false;
	private boolean mIsPlaying = false;
	private boolean mIsPrepared = false;
	private int mPosition = 0;
	private float mVolume = 0f;
	
	private OnFilePreparedListener onFilePreparedListener;
	
	public MockFilePlayer(IFile file) {
		mFile = file;
	}
	
	@NonNull
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
		if (onFilePreparedListener != null)
			onFilePreparedListener.onFilePrepared(this);
	}

	@Override
	public void prepareMpSynchronously() {
		mIsPrepared = true; 
	}

	@Override
	public IPlaybackFilePreparation setOnFilePreparedListener(OnFilePreparedListener listener) {
		onFilePreparedListener = listener;
		return this;
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
	public int getDuration() {
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
	public void setOnFileCompleteListener(OnFileCompleteListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setOnFileErrorListener(OnFileErrorListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setOnFileBufferedListener(OnFileBufferedListener listener) {
		// TODO Auto-generated method stub
		
	}
}
