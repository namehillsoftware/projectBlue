package com.lasthopesoftware.bluewater.data.service.helpers.playback;

import java.util.ArrayList;

import android.content.Context;
import android.support.v4.app.NotificationCompat;

import com.lasthopesoftware.bluewater.BackgroundFilePreparer;
import com.lasthopesoftware.bluewater.R;
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
	private ArrayList<JrFile> mPlaylist;
	private JrFileMediaPlayer mCurrentFilePlayer;
	private Context mContext;
	private int mFileKey;
	
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
		if (mFileKey == fileKey && mCurrentFilePlayer.isPlaying()) return;
		
		// stop any playback that is in action
		if (mCurrentFilePlayer != null) {
			if (mCurrentFilePlayer.isPlaying())
				mCurrentFilePlayer.stop();
			
			throwStopEvent(mPlayingFile);
			
			mCurrentFilePlayer = null;
			release();
		}
		
		mFileKey = fileKey < 0 ? mPlaylist.get(0).getKey() : fileKey;
		startPos = startPos < 0 ? 0 : startPos;
        
		for (JrFile file : mPlaylist) {
			if (file.getKey() != mFileKey) continue;
		
			mCurrentFilePlayer = new JrFileMediaPlayer(mContext, file);
			mCurrentFilePlayer.addOnJrFileCompleteListener(this);
			mCurrentFilePlayer.addOnJrFilePreparedListener(this);
			mCurrentFilePlayer.addOnJrFileErrorListener(this);
			mCurrentFilePlayer.initMediaPlayer();
			mCurrentFilePlayer.seekTo(startPos);
			mCurrentFilePlayer.prepareMediaPlayer(); // prepare async to not block main thread
        	break;
		}
	}
	
	public void release() {
		mCurrentFilePlayer.releaseMediaPlayer();
	}
	
	@Override
	public void onJrFilePrepared(JrFileMediaPlayer mediaPlayer, JrFile file) {
		if (mediaPlayer.isPlaying()) return;
		
		JrSession.GetLibrary(mContext).setNowPlayingId(file.getKey());
		JrSession.SaveSession(mContext);
		
		mediaPlayer.start();
		
        if (file.getNextFile() != null) {
        	BackgroundFilePreparer backgroundProgressThread = new BackgroundFilePreparer(this, playingFile);
        	if (trackProgressThread != null && trackProgressThread.isAlive()) trackProgressThread.interrupt();
	        trackProgressThread = new Thread(backgroundProgressThread);
	        trackProgressThread.setName("Thread to prepare next file.");
	        trackProgressThread.setPriority(Thread.MIN_PRIORITY);
	        trackProgressThread.start();
        }
		
		for (OnNowPlayingChangeListener listener : mOnNowPlayingChangeListeners)
			listener.onNowPlayingChange(this, file);
	}

	@Override
	public void onJrFileComplete(JrFileMediaPlayer mediaPlayer, JrFile file) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onJrFileError(JrFileMediaPlayer mediaPlayer, JrFile file,
			int what, int extra) {
		// TODO Auto-generated method stub
		return false;
	}
	

	public interface OnNowPlayingChangeListener {
		void onNowPlayingChange(JrPlaylistStateControl controller, JrFile nowPlayingFile);
	}
}
