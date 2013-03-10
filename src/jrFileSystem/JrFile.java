package jrFileSystem;

import java.util.LinkedList;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.PowerManager;
import jrAccess.JrSession;

public class JrFile extends JrListing implements
	OnPreparedListener, 
	OnErrorListener, 
	OnCompletionListener
{

	private String mArtist;
	private String mAlbum;
	private String mGenre;
	private int mTrackNumber;
	private boolean prepared;
	private boolean preparing;
	private MediaPlayer mp;
	private LinkedList<OnJrFileCompleteListener> onJrFileCompleteListeners;
	private LinkedList<OnJrFilePreparedListener> onJrFilePreparedListeners;
	
	public JrFile(int key) {
		this.mKey = key;
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
		if (onJrFileCompleteListeners == null) onJrFileCompleteListeners = new LinkedList<OnJrFileCompleteListener>();
		onJrFileCompleteListeners.add(listener);
	}
	
	public void setOnFilePreparedListener(OnJrFilePreparedListener listener) {
		if (onJrFilePreparedListeners == null) onJrFilePreparedListeners = new LinkedList<OnJrFilePreparedListener>();
		onJrFilePreparedListeners.add(listener);
	}
	
	public String getUrl() {
		return JrSession.accessDao.getJrUrl("File/GetFile", "File=" + Integer.toString(mKey), "conversion=2");
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
	
	public void initMediaPlayer(Context context) {
		if (mp != null) return;
		
		mp = new MediaPlayer(); // initialize it here
		mp.setOnPreparedListener(this);
		mp.setOnErrorListener(this);
		mp.setOnCompletionListener(this);
		mp.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
        try {
        	mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        	mp.setDataSource(getUrl());
//        	if (url == mUrl) mp.prepareAsync(); // prepare async to not block main thread
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public MediaPlayer getMediaPlayer() {
		return mp;
	}
	
	public void prepareMediaPlayer() {
		if (!preparing && !prepared) {
			mp.prepareAsync();
			preparing = true;
		}
	}
	
	public void releaseMediaPlayer() {
		mp.release();
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
	
}
