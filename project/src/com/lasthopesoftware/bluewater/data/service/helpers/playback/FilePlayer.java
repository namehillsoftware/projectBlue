package com.lasthopesoftware.bluewater.data.service.helpers.playback;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.LoggerFactory;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.MediaStore;
import ch.qos.logback.classic.Logger;

import com.lasthopesoftware.bluewater.data.service.access.FileProperties;
import com.lasthopesoftware.bluewater.data.service.objects.File;
import com.lasthopesoftware.bluewater.data.service.objects.OnFileCompleteListener;
import com.lasthopesoftware.bluewater.data.service.objects.OnFileErrorListener;
import com.lasthopesoftware.bluewater.data.service.objects.OnFilePreparedListener;
import com.lasthopesoftware.bluewater.data.session.JrSession;

public class FilePlayer implements
	OnPreparedListener, 
	OnErrorListener, 
	OnCompletionListener
{
	private volatile MediaPlayer mp;
	private AtomicBoolean isPrepared = new AtomicBoolean();
	private AtomicBoolean isPreparing = new AtomicBoolean();
	private int mPosition = 0;
	private float mVolume = 1.0f;
	private Context mMpContext;
	private File mFile;
	
	private static final String FILE_URI_SCHEME = "file://";
	private static final String MEDIA_QUERY = "(" + 
												MediaStore.Audio.Media.DATA + " LIKE '%' || ? || '%') OR (" +
												MediaStore.Audio.Media.ARTIST + " = ? AND " +
												MediaStore.Audio.Media.ALBUM + " = ? AND " +
												MediaStore.Audio.Media.TITLE + " = ? AND " +
												MediaStore.Audio.Media.TRACK + " = ?" +
											  ")";
	
	private LinkedList<OnFileCompleteListener> onFileCompleteListeners = new LinkedList<OnFileCompleteListener>();
	private LinkedList<OnFilePreparedListener> onFilePreparedListeners = new LinkedList<OnFilePreparedListener>();
	private LinkedList<OnFileErrorListener> onFileErrorListeners = new LinkedList<OnFileErrorListener>();
	
	public FilePlayer(Context context, File file) {
		mMpContext = context;
		mFile = file;
	}
	
	public void addOnFileCompleteListener(OnFileCompleteListener listener) {
		onFileCompleteListeners.add(listener);
	}
	
	public void removeOnFileCompleteListener(OnFileCompleteListener listener) {
		if (onFileCompleteListeners.contains(listener)) onFileCompleteListeners.remove(listener);
	}
	
	public void addOnFilePreparedListener(OnFilePreparedListener listener) {
		onFilePreparedListeners.add(listener);
	}
	
	public void removeOnFilePreparedListener(OnFilePreparedListener listener) {
		if (onFilePreparedListeners.contains(listener)) onFilePreparedListeners.remove(listener);
	}
	
	public void addOnFileErrorListener(OnFileErrorListener listener) {
		onFileErrorListeners.add(listener);
	}
	
	public void removeOnFileErrorListener(OnFileErrorListener listener) {
		if (onFileErrorListeners.contains(listener)) onFileErrorListeners.remove(listener);
	}
	
	public File getFile() {
		return mFile;
	}
	
	public void initMediaPlayer() {
		if (mp != null) return;
	
		mp = new MediaPlayer(); // initialize it here
		mp.setOnPreparedListener(this);
		mp.setOnErrorListener(this);
		mp.setOnCompletionListener(this);
		mp.setWakeMode(mMpContext, PowerManager.PARTIAL_WAKE_LOCK);
		mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
	}
	
	public boolean isMediaPlayerCreated() {
		return mp != null;
	}
	
	public boolean isPrepared() {
		return isPrepared.get();
	}
	
	@SuppressLint("InlinedApi")
	private Uri getMpUri() throws IOException {
		if (mMpContext == null)
			throw new NullPointerException("The file player's context cannot be null");
				
		final String originalFilename = mFile.getProperty(FileProperties.FILENAME);
		if (originalFilename == null)
			throw new IOException("The filename property was not retrieved. A connection needs to be re-established.");
		
		final String filename = originalFilename.substring(originalFilename.lastIndexOf('\\') + 1, originalFilename.lastIndexOf('.'));
		final String[] params = { 	filename,
									mFile.getProperty(FileProperties.ARTIST) != null ? mFile.getProperty(FileProperties.ARTIST) : "",
									mFile.getProperty(FileProperties.ALBUM) != null ? mFile.getProperty(FileProperties.ALBUM) : "",
									mFile.getProperty(FileProperties.NAME) != null ? mFile.getProperty(FileProperties.NAME) : "",
									mFile.getProperty(FileProperties.TRACK) != null ? mFile.getProperty(FileProperties.TRACK) : ""};
	    
		final String[] projection = { MediaStore.Audio.Media.DATA };
		final Cursor cursor = mMpContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, MEDIA_QUERY, params, null);
	    try {
		    if (cursor.moveToFirst()) {
		    	final String fileUriString = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
		    	if (fileUriString != null && !fileUriString.isEmpty()) {
		    		final java.io.File file = new java.io.File(fileUriString.replaceFirst(FILE_URI_SCHEME, ""));
		    		
		    		if (file != null) return Uri.fromFile(file);
		    	}
		    }
	    } catch (IllegalArgumentException ie) {
	    	LoggerFactory.getLogger(getClass()).info("Illegal column name.", ie);
	    } finally {
	    	cursor.close();
	    }
	    
	    final String itemUrl = mFile.getSubItemUrl();
	    if (itemUrl != null && !itemUrl.isEmpty())
	    	return Uri.parse(itemUrl);
	    
	    return null;
	}
	
	public void prepareMediaPlayer() {
		if (!isPreparing.get() && !isPrepared.get()) {
			try {
				final Uri uri = getMpUri();
				if (uri != null) {
					setMpDataSource(uri);
					isPreparing.set(true);
					mp.prepareAsync();
					return;
				}
			} catch (IOException io) {
				throwIoErrorEvent();
			} catch (Exception e) {
				LoggerFactory.getLogger(getClass()).error(e.toString(), e);
			}
		}
	}
	
	public void prepareMpSynchronously() {
		if (!isPreparing.get() && !isPrepared.get()) {
			try {
				final Uri uri = getMpUri();
				if (uri != null) {
					setMpDataSource(uri);
					
					isPreparing.set(true);
					mp.prepare();
					isPrepared.set(true);
					return;
				}
				
				isPreparing.set(false);
			} catch (IOException io) {
				throwIoErrorEvent();
			} catch (Exception e) {
				LoggerFactory.getLogger(getClass()).error(e.toString(), e);
				resetMediaPlayer();
				isPreparing.set(false);
			}
		}
	}
	
	private void throwIoErrorEvent() {
		for (OnFileErrorListener listener : onFileErrorListeners)
			listener.onJrFileError(this, MediaPlayer.MEDIA_ERROR_SERVER_DIED, MediaPlayer.MEDIA_ERROR_IO);
	}
	
	private void setMpDataSource(Uri uri) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException {
		final Map<String, String> headers = new HashMap<String, String>();
		if (mMpContext == null)
			throw new NullPointerException("The file player's context cannot be null");
		if (!JrSession.GetLibrary().getAuthKey().isEmpty())
			headers.put("Authorization", "basic " + JrSession.GetLibrary().getAuthKey());
		mp.setDataSource(mMpContext, uri, headers);
	}
	
	private void resetMediaPlayer() {
		if (mp == null) {
			initMediaPlayer();
			return;
		}
		
		mp.reset();
		
		if (mMpContext != null)
			mp.setWakeMode(mMpContext, PowerManager.PARTIAL_WAKE_LOCK);
		
		mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
	}
	
	public void releaseMediaPlayer() {
		if (mp != null) mp.release();
		mp = null;
		isPrepared.set(false);
	}
	
	@Override
	public void onPrepared(MediaPlayer mp) {
		isPrepared.set(true);
		isPreparing.set(false);
		for (OnFilePreparedListener listener : onFilePreparedListeners) listener.onJrFilePrepared(this);
	}
	
	@Override
	public void onCompletion(MediaPlayer mp) {
		Thread updateStatsThread = new Thread(new UpdatePlayStatsRunner(mFile));
		updateStatsThread.setName("Asynchronous Update Stats Thread for " + mFile.getValue());
		updateStatsThread.setPriority(Thread.MIN_PRIORITY);
		updateStatsThread.start();
		
		releaseMediaPlayer();
		for (OnFileCompleteListener listener : onFileCompleteListeners) listener.onJrFileComplete(this);
	}
	
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		Logger logger = (Logger) LoggerFactory.getLogger(File.class);
		logger.error("Media Player error.");
		logger.error("What: ");
		logger.error(what == MediaPlayer.MEDIA_ERROR_UNKNOWN ? "MEDIA_ERROR_UNKNOWN" : "MEDIA_ERROR_SERVER_DIED");
		logger.error("Extra: ");
		switch (extra) {
			case MediaPlayer.MEDIA_ERROR_IO:
				logger.error("MEDIA_ERROR_IO");
				break;
			case MediaPlayer.MEDIA_ERROR_MALFORMED:
				logger.error("MEDIA_ERROR_MALFORMED");
				break;
			case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
				logger.error("MEDIA_ERROR_UNSUPPORTED");
				break;
			case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
				logger.error("MEDIA_ERROR_TIMED_OUT");
				break;
			case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
				logger.error("MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK");
				break;
			default:
				logger.error("Unknown");
				break;
		}
		resetMediaPlayer();
		boolean handled = false;
		for (OnFileErrorListener listener : onFileErrorListeners) handled |= listener.onJrFileError(this, what, extra);
		return handled;
	}

	public int getBufferPercentage() {
		return mp == null ? 0 : (mp.getCurrentPosition() * 100) / mp.getDuration();
	}

	public int getCurrentPosition() {
		if (mp != null && isPrepared() && isPlaying()) mPosition = mp.getCurrentPosition();
		return mPosition;
	}
	
	public int getDuration() throws IOException {
		if (mp == null || !isPrepared())
			return mFile.getDuration();
		
		return mp.getDuration();
	}

	public boolean isPlaying() {
		return mp != null && mp.isPlaying();
	}

	public void pause() {
		if (isPreparing.get()) {
			try {
				mp.reset();
			} catch (IllegalStateException e) {
				releaseMediaPlayer();
				initMediaPlayer();
				return;
			}
		}
		
		mPosition = mp.getCurrentPosition();
		mp.pause();
	}

	public void seekTo(int pos) {
		mPosition = pos;
		if (mp != null && isPrepared() && isPlaying()) mp.seekTo(mPosition);
	}

	public void start() {
		mp.seekTo(mPosition);
		mp.start();
	}
	
	public void stop() {
		mPosition = 0;
		mp.stop();
	}
	
	public float getVolume() {
		return mVolume;
	}
	
	public void setVolume(float volume) {
		mVolume = volume;
		
		if (mp != null)
			mp.setVolume(mVolume, mVolume);
	}
	
	private static class UpdatePlayStatsRunner implements Runnable {
		private File mFile;
		
		public UpdatePlayStatsRunner(File file) {
			mFile = file;
		}
		
		@Override
		public void run() {
			try {
				final String numberPlaysString = mFile.getRefreshedProperty("Number Plays");
				
				int numberPlays = 0;
				if (numberPlaysString != null && !numberPlaysString.isEmpty()) numberPlays = Integer.parseInt(numberPlaysString);
				
				mFile.setProperty("Number Plays", String.valueOf(++numberPlays));	
				
				final String lastPlayed = String.valueOf(System.currentTimeMillis()/1000);
				mFile.setProperty("Last Played", lastPlayed);
			} catch (IOException e) {
				LoggerFactory.getLogger(FilePlayer.class).warn(e.toString(), e);
			} catch (NumberFormatException ne) {
				LoggerFactory.getLogger(FilePlayer.class).error(ne.toString(), ne);
			}
		}
	}
}
