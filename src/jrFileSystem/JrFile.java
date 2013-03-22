package jrFileSystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.PowerManager;
import android.widget.MediaController.MediaPlayerControl;
import jrAccess.JrConnection;
import jrAccess.JrSession;
import jrAccess.JrTestConnection;

public class JrFile extends JrListing implements
	OnPreparedListener, 
	OnErrorListener, 
	OnCompletionListener,
	MediaPlayerControl
{

	private String mArtist;
	private String mAlbum;
	private String mGenre;
	private int mTrackNumber;
	private double mDuration;
	private boolean prepared = false;
	private boolean preparing = false;
	private MediaPlayer mp;
	private ArrayList<OnJrFileCompleteListener> onJrFileCompleteListeners;
	private ArrayList<OnJrFilePreparedListener> onJrFilePreparedListeners;
	private JrFile mNextFile, mPreviousFile;
	
	public JrFile(int key) {
		this.setKey(key);
	}
	public JrFile(int key, String value) {
		super(key, value);
	}
	
	public JrFile(int key, String value, String Artist, String Album, int TrackNumber) {
		super(key, value);
		
		mArtist = Artist;
		mAlbum = Album;
		mTrackNumber = TrackNumber;
	}
	
	public JrFile() {
		super();
	}
	
	public void setOnFileCompletionListener(OnJrFileCompleteListener listener) {
		if (onJrFileCompleteListeners == null) onJrFileCompleteListeners = new ArrayList<OnJrFileCompleteListener>();
		onJrFileCompleteListeners.add(listener);
	}
	
	public void setOnFilePreparedListener(OnJrFilePreparedListener listener) {
		if (onJrFilePreparedListeners == null) onJrFilePreparedListeners = new ArrayList<OnJrFilePreparedListener>();
		onJrFilePreparedListeners.add(listener);
	}
	
	public String getUrl() {
		return JrSession.accessDao.getJrUrl("File/GetFile", "File=" + Integer.toString(getKey()), "Quality=medium", "Conversion=Android");
	}
	
	private String getMpUrl() throws InterruptedException, ExecutionException {
		if (!JrTestConnection.doTest()) return null;
		return getUrl();
	}
	/**
	 * @return the mArtist
	 */
	public String getArtist() {
		return mArtist;
	}
	/**
	 * @param mArtist the mArtist to set
	 */
	public void setArtist(String mArtist) {
		this.mArtist = mArtist;
	}
	/**
	 * @return the mAlbum
	 */
	public String getAlbum() {
		return mAlbum;
	}
	/**
	 * @param mAlbum the mAlbum to set
	 */
	public void setAlbum(String mAlbum) {
		this.mAlbum = mAlbum;
	}
	/**
	 * @return the mGenre
	 */
	public String getGenre() {
		return mGenre;
	}
	/**
	 * @param mGenre the mGenre to set
	 */
	public void setGenre(String mGenre) {
		this.mGenre = mGenre;
	}
	/**
	 * @return the mTrackNumber
	 */
	public int getTrackNumber() {
		return mTrackNumber;
	}
	/**
	 * @param mTrackNumber the mTrackNumber to set
	 */
	public void setTrackNumber(int mTrackNumber) {
		this.mTrackNumber = mTrackNumber;
	}
	/**
	 * @return the mDuration
	 */
	public int getDuration() {
		return (int)mDuration;
	}
	/**
	 * @param mDuration the mDuration to set
	 */
	public void setDuration(double mDuration) {
		this.mDuration = mDuration;
	}
	/**
	 * @return the prepared
	 */
	public boolean isPrepared() {
		return prepared;
	}
	/**
	 * @param prepared the prepared to set
	 */
	public void setPrepared(boolean prepared) {
		this.prepared = prepared;
	}
	
	public JrFile getNextFile() {
		return mNextFile;
	}
	
	public JrFile getPreviousFile() {
		return mPreviousFile;
	}
	
	public void setSiblings(ArrayList<JrFile> files) {
		int position = files.indexOf(this);
		if (position < 0) return;
		if (position > 0 && files.size() > 1) mPreviousFile = files.get(position - 1);
		if (position < files.size() - 1) mNextFile = files.get(position + 1);
	}
	
	public void initMediaPlayer(Context context) {
		if (mp != null) return;
		
		mp = new MediaPlayer(); // initialize it here
		mp.setOnPreparedListener(this);
		mp.setOnErrorListener(this);
		mp.setOnCompletionListener(this);
		mp.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
		mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
	}
	
	public MediaPlayer getMediaPlayer() {
		return mp;
	}
	
	public void prepareMediaPlayer() {
		if (!preparing && !prepared) {
			try {
				mp.setDataSource(getMpUrl());
				mp.prepareAsync();
				preparing = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void prepareMpSynchronously() {
		if (!preparing && !prepared) {
			try {
				mp.setDataSource(getMpUrl());
				preparing = true;
				mp.prepare();
			} catch (Exception e) {
				e.printStackTrace();
				preparing = false;
			}			
		}
	}
	
	public void releaseMediaPlayer() {
		if (mp != null) mp.release();
		mp = null;
		prepared = false;
	}
	
	@Override
	public void onPrepared(MediaPlayer mp) {
		prepared = true;
		preparing = false;
		for (OnJrFilePreparedListener listener : onJrFilePreparedListeners) listener.onJrFilePrepared(this);
	}
	
	@Override
	public void onCompletion(MediaPlayer mp) {
		releaseMediaPlayer();
		for (OnJrFileCompleteListener listener : onJrFileCompleteListeners) listener.onJrFileComplete(this);
	}
	
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		mp.reset();
		return false;
	}
	@Override
	public boolean canPause() {
		return true;
	}
	@Override
	public boolean canSeekBackward() {
		return true;
	}
	@Override
	public boolean canSeekForward() {
		return true;
	}
	@Override
	public int getBufferPercentage() {
		return 0;
	}
	@Override
	public int getCurrentPosition() {
		return 0;
	}
	@Override
	public boolean isPlaying() {
		return mp.isPlaying();
	}
	@Override
	public void pause() {
		mp.pause();
	}
	@Override
	public void seekTo(int pos) {
		mp.seekTo(pos);
	}
	@Override
	public void start() {
		mp.start();
	}
	
}
