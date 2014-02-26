package com.lasthopesoftware.bluewater.data.service.helpers.playback;

import java.util.ArrayList;

import android.content.Context;

import com.lasthopesoftware.bluewater.BackgroundFilePreparer;
import com.lasthopesoftware.bluewater.data.service.objects.JrFile;
import com.lasthopesoftware.bluewater.data.service.objects.JrFiles;
import com.lasthopesoftware.bluewater.data.service.objects.OnJrFileCompleteListener;
import com.lasthopesoftware.bluewater.data.service.objects.OnJrFileErrorListener;
import com.lasthopesoftware.bluewater.data.service.objects.OnJrFilePreparedListener;
import com.lasthopesoftware.bluewater.data.session.JrSession;

public class JrPlaylistStateControl implements
	OnJrFilePreparedListener,
	OnJrFileErrorListener, 
	OnJrFileCompleteListener
{
	private ArrayList<OnNowPlayingChangeListener> mOnNowPlayingChangeListeners = new ArrayList<OnNowPlayingChangeListener>();
	private ArrayList<OnNowPlayingStopListener> mOnNowPlayingStopListeners = new ArrayList<OnNowPlayingStopListener>();
	private ArrayList<JrFile> mPlaylist;
	private JrFileMediaPlayer mCurrentFilePlayer, mNextFilePlayer;
	private Context mContext;
	private Thread mBackgroundFilePreparerThread;
	
	public JrPlaylistStateControl(Context context, String playlistString) {
		this(context, JrFiles.deserializeFileStringList(playlistString));
	}
	
	public JrPlaylistStateControl(Context context, ArrayList<JrFile> playlist) {
		mContext = context;
		mPlaylist = playlist;
	}
	
	public void seekTo(int fileKey) {
		seekTo(fileKey, 0);
	}
	
	public void seekTo(int fileKey, int startPos) {
		// If the track is already playing, keep on playing
		if (mCurrentFilePlayer != null && mCurrentFilePlayer.getFile().getKey() == fileKey && mCurrentFilePlayer.isPlaying()) return;
		
		// stop any playback that is in action
		if (mCurrentFilePlayer != null) {
			if (mCurrentFilePlayer.isPlaying()) mCurrentFilePlayer.stop();
			
			throwStopEvent(mCurrentFilePlayer.getFile());
			
			mCurrentFilePlayer.releaseMediaPlayer();
			mCurrentFilePlayer = null;
		}
		
		fileKey = fileKey < 0 ? mPlaylist.get(0).getKey() : fileKey;
		startPos = startPos < 0 ? 0 : startPos;
        
		for (JrFile file : mPlaylist) {
			if (file.getKey() != fileKey) continue;
		
			JrFileMediaPlayer filePlayer = new JrFileMediaPlayer(mContext, file);
			filePlayer.addOnJrFileCompleteListener(this);
			filePlayer.addOnJrFilePreparedListener(this);
			filePlayer.addOnJrFileErrorListener(this);
			filePlayer.initMediaPlayer();
			filePlayer.seekTo(startPos);
			filePlayer.prepareMediaPlayer(); // prepare async to not block main thread
        	break;
		}
	}
	
	public void pause() {
		if (mCurrentFilePlayer != null) {
			if (mCurrentFilePlayer.isPlaying()) {
				mCurrentFilePlayer.pause();
				JrSession.GetLibrary(mContext).setNowPlayingId(mCurrentFilePlayer.getFile().getKey());
				JrSession.GetLibrary(mContext).setNowPlayingProgress(mCurrentFilePlayer.getCurrentPosition());
			}
			JrSession.SaveSession(mContext);
			throwStopEvent(mCurrentFilePlayer.getFile());
			mCurrentFilePlayer.releaseMediaPlayer();
		}
		
		if (mNextFilePlayer != null)
			mNextFilePlayer.releaseMediaPlayer();
	}
	
	private void startFilePlayback(JrFileMediaPlayer mediaPlayer) {
		mCurrentFilePlayer = mediaPlayer;
		JrSession.GetLibrary(mContext).setNowPlayingId(mediaPlayer.getFile().getKey());
		JrSession.SaveSession(mContext);
		
		mediaPlayer.start();
		
        if (mediaPlayer.getFile().getNextFile() != null) {
        	mNextFilePlayer = new JrFileMediaPlayer(mContext, mediaPlayer.getFile().getNextFile());
        	BackgroundFilePreparer backgroundFilePreparer = new BackgroundFilePreparer(mCurrentFilePlayer, mNextFilePlayer);
        	if (mBackgroundFilePreparerThread != null && mBackgroundFilePreparerThread.isAlive()) mBackgroundFilePreparerThread.interrupt();
        	mBackgroundFilePreparerThread = new Thread(backgroundFilePreparer);
        	mBackgroundFilePreparerThread.setName("Thread to prepare next file.");
        	mBackgroundFilePreparerThread.setPriority(Thread.MIN_PRIORITY);
        	mBackgroundFilePreparerThread.start();
        }
		
		throwChangeEvent(mediaPlayer.getFile());
	}
	
	public boolean isPlaying() {
		return mCurrentFilePlayer != null && mCurrentFilePlayer.isPlaying();
	}

	/* Event handlers */
	@Override
	public void onJrFilePrepared(JrFileMediaPlayer mediaPlayer) {
		if (mediaPlayer.isPlaying()) return;
		
		startFilePlayback(mediaPlayer);
	}
	
	@Override
	public void onJrFileComplete(JrFileMediaPlayer mediaPlayer) {
		throwStopEvent(mediaPlayer.getFile());
		
		mediaPlayer.releaseMediaPlayer();
		
		if (mNextFilePlayer == null) {
			if (mediaPlayer.getFile().getNextFile() == null) return;
			
			mNextFilePlayer = new JrFileMediaPlayer(mContext, mediaPlayer.getFile().getNextFile());
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
	public boolean onJrFileError(JrFileMediaPlayer mediaPlayer, int what, int extra) {
		// TODO Auto-generated method stub
		return false;
	}
	
	private void throwChangeEvent(JrFile nowPlayingFile) {
		for (OnNowPlayingChangeListener listener : mOnNowPlayingChangeListeners)
			listener.onNowPlayingChange(this, nowPlayingFile);
	}
	
	private void throwStopEvent(JrFile stoppedFile) {
		for (OnNowPlayingStopListener listener : mOnNowPlayingStopListeners)
			listener.onNowPlayingStop(this, stoppedFile);
	}
	
	public void release() {
		mCurrentFilePlayer.releaseMediaPlayer();
	}

	/* Listener interfaces */
	public interface OnNowPlayingChangeListener {
		void onNowPlayingChange(JrPlaylistStateControl controller, JrFile nowPlayingFile);
	}
	
	public interface OnNowPlayingStopListener {
		void onNowPlayingStop(JrPlaylistStateControl controller, JrFile stoppedFile);
	}
}
