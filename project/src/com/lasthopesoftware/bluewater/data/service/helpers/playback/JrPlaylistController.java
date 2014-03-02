package com.lasthopesoftware.bluewater.data.service.helpers.playback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.slf4j.LoggerFactory;

import android.content.Context;

import com.lasthopesoftware.bluewater.BackgroundFilePreparer;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnNowPlayingChangeListener;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnNowPlayingStopListener;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnPlaylistStateControlErrorListener;
import com.lasthopesoftware.bluewater.data.service.objects.JrFile;
import com.lasthopesoftware.bluewater.data.service.objects.JrFiles;
import com.lasthopesoftware.bluewater.data.service.objects.OnJrFileCompleteListener;
import com.lasthopesoftware.bluewater.data.service.objects.OnJrFileErrorListener;
import com.lasthopesoftware.bluewater.data.service.objects.OnJrFilePreparedListener;

public class JrPlaylistController implements
	OnJrFilePreparedListener,
	OnJrFileErrorListener, 
	OnJrFileCompleteListener
{
	private HashSet<OnNowPlayingChangeListener> mOnNowPlayingChangeListeners = new HashSet<OnNowPlayingChangeListener>();
	private HashSet<OnNowPlayingStopListener> mOnNowPlayingStopListeners = new HashSet<OnNowPlayingStopListener>();
	private HashSet<OnPlaylistStateControlErrorListener> mOnPlaylistStateControlErrorListeners = new HashSet<OnPlaylistStateControlErrorListener>();
	private ArrayList<JrFile> mPlaylist;
	private JrFilePlayer mCurrentFilePlayer, mNextFilePlayer;
	private Context mContext;
	private Thread mBackgroundFilePreparerThread;
	private float mVolume;
	
	public JrPlaylistController(Context context, String playlistString) {
		this(context, JrFiles.deserializeFileStringList(playlistString));
	}
	
	public JrPlaylistController(Context context, ArrayList<JrFile> playlist) {
		mContext = context;
		mPlaylist = playlist;
	}
	
	/* Begin playlist control */
	
	public void seekTo(int fileKey) {
		seekTo(fileKey, 0);
	}
	
	public void seekTo(int fileKey, int startPos) {
		// If the track is already playing, keep on playing
		if (mCurrentFilePlayer != null && mCurrentFilePlayer.getFile().getKey() == fileKey && mCurrentFilePlayer.isPlaying()) return;
		
		// stop any playback that is in action
		if (mCurrentFilePlayer != null) {
			if (mCurrentFilePlayer.isPlaying()) mCurrentFilePlayer.stop();
			
			throwStopEvent(mCurrentFilePlayer);
			
			mCurrentFilePlayer.releaseMediaPlayer();
			mCurrentFilePlayer = null;
		}
		
		fileKey = fileKey < 0 ? mPlaylist.get(0).getKey() : fileKey;
		startPos = startPos < 0 ? 0 : startPos;
        
		for (JrFile file : mPlaylist) {
			if (file.getKey() != fileKey) continue;
		
			JrFilePlayer filePlayer = new JrFilePlayer(mContext, file);
			filePlayer.addOnJrFileCompleteListener(this);
			filePlayer.addOnJrFilePreparedListener(this);
			filePlayer.addOnJrFileErrorListener(this);
			filePlayer.initMediaPlayer();
			filePlayer.seekTo(startPos);
			filePlayer.prepareMediaPlayer(); // prepare async to not block main thread
        	break;
		}
	}
	
	public boolean resume() {
		if (mCurrentFilePlayer == null) return false;
		
		startFilePlayback(mCurrentFilePlayer);
		return true;
	}

	private void startFilePlayback(JrFilePlayer mediaPlayer) {
		mCurrentFilePlayer = mediaPlayer;
		
		mediaPlayer.setVolume(mVolume);
		mediaPlayer.start();
		
        if (mediaPlayer.getFile().getNextFile() != null) {
        	mNextFilePlayer = new JrFilePlayer(mContext, mediaPlayer.getFile().getNextFile());
        	BackgroundFilePreparer backgroundFilePreparer = new BackgroundFilePreparer(mCurrentFilePlayer, mNextFilePlayer);
        	if (mBackgroundFilePreparerThread != null && mBackgroundFilePreparerThread.isAlive()) mBackgroundFilePreparerThread.interrupt();
        	mBackgroundFilePreparerThread = new Thread(backgroundFilePreparer);
        	mBackgroundFilePreparerThread.setName("Thread to prepare next file");
        	mBackgroundFilePreparerThread.setPriority(Thread.MIN_PRIORITY);
        	mBackgroundFilePreparerThread.start();
        }
		
		throwChangeEvent(mediaPlayer);
	}
	
	public void pause() {
		if (mCurrentFilePlayer != null) {
			if (mCurrentFilePlayer.isPlaying()) mCurrentFilePlayer.pause();
			
			throwStopEvent(mCurrentFilePlayer);
		}
		
		release();
	}
	
	public boolean isPrepared() {
		return mCurrentFilePlayer != null && mCurrentFilePlayer.isPrepared();
	}
	
	public boolean isPlaying() {
		return mCurrentFilePlayer != null && mCurrentFilePlayer.isPlaying();
	}
	
	public void setVolume(float volume) {
		mVolume = volume;
		if (mCurrentFilePlayer != null && mCurrentFilePlayer.isPlaying()) mCurrentFilePlayer.setVolume(mVolume);
	}
	
	/* End playlist control */
	
	public JrFilePlayer getCurrentFilePlayer() {
		return mCurrentFilePlayer;
	}
	
	public List<JrFile> getPlaylist() {
		return Collections.unmodifiableList(mPlaylist);
	}

	/* Event handlers */
	@Override
	public void onJrFilePrepared(JrFilePlayer mediaPlayer) {
		if (mediaPlayer.isPlaying()) return;
		
		startFilePlayback(mediaPlayer);
	}
	
	@Override
	public void onJrFileComplete(JrFilePlayer mediaPlayer) {
		throwStopEvent(mediaPlayer);
		
		mediaPlayer.releaseMediaPlayer();
		
		if (mNextFilePlayer == null) {
			if (mediaPlayer.getFile().getNextFile() == null) return;
			
			mNextFilePlayer = new JrFilePlayer(mContext, mediaPlayer.getFile().getNextFile());
		}
		
		mNextFilePlayer.addOnJrFileCompleteListener(this);
		mNextFilePlayer.addOnJrFileErrorListener(this);
		if (!mNextFilePlayer.isPrepared()) {
			mNextFilePlayer.addOnJrFilePreparedListener(this);
			mNextFilePlayer.prepareMediaPlayer();
			return;
		}
		
		startFilePlayback(mNextFilePlayer);
	}
	
	@Override
	public boolean onJrFileError(JrFilePlayer mediaPlayer, int what, int extra) {
		LoggerFactory.getLogger(JrPlaylistController.class).error("JR File error - " + what + " - " + extra);
		pause();
		
		for (OnPlaylistStateControlErrorListener listener : mOnPlaylistStateControlErrorListeners) {
			if (listener.onPlaylistStateControlError(this, mediaPlayer)) return true;
		}
		
		return false;
	}
	/* End event handlers */
	
	/* Listener callers */
	private void throwChangeEvent(JrFilePlayer filePlayer) {
		for (OnNowPlayingChangeListener listener : mOnNowPlayingChangeListeners)
			listener.onNowPlayingChange(this, filePlayer);
	}
	
	private void throwStopEvent(JrFilePlayer filePlayer) {
		for (OnNowPlayingStopListener listener : mOnNowPlayingStopListeners)
			listener.onNowPlayingStop(this, filePlayer);
	}
	
	/* Listener collection helpers */
	public void addOnNowPlayingChangeListener(OnNowPlayingChangeListener listener) {
		mOnNowPlayingChangeListeners.add(listener);
	}
	
	public void removeOnNowPlayingChangeListener(OnNowPlayingChangeListener listener) {
		if (mOnNowPlayingChangeListeners.contains(listener))
			mOnNowPlayingChangeListeners.remove(listener);
	}
	
	public void addOnNowPlayingStopListener(OnNowPlayingStopListener listener) {
		mOnNowPlayingStopListeners.add(listener);
	}
	
	public void removeOnNowPlayingStopListener(OnNowPlayingStopListener listener) {
		if (mOnNowPlayingStopListeners.contains(listener))
			mOnNowPlayingStopListeners.remove(listener);
	}
	
	public void addOnPlaylistStateControlErrorListener(OnPlaylistStateControlErrorListener listener) {
		mOnPlaylistStateControlErrorListeners.add(listener);
	}
	
	public void removeOnPlaylistStateControlErrorListener(OnPlaylistStateControlErrorListener listener) {
		if (mOnPlaylistStateControlErrorListeners.contains(listener))
			mOnPlaylistStateControlErrorListeners.remove(listener);
	}
	
	// Release all heavy resources
	public void release() {
		if (mCurrentFilePlayer != null) mCurrentFilePlayer.releaseMediaPlayer();
		if (mNextFilePlayer != null) mNextFilePlayer.releaseMediaPlayer();
		
		if (mBackgroundFilePreparerThread != null && mBackgroundFilePreparerThread.isAlive())
			mBackgroundFilePreparerThread.interrupt();
	}
}
