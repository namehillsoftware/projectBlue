package com.lasthopesoftware.bluewater.data.service.helpers.playback;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
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
import android.os.AsyncTask;
import android.os.PowerManager;
import android.provider.MediaStore;

import com.lasthopesoftware.bluewater.data.service.access.FileProperties;
import com.lasthopesoftware.bluewater.data.service.objects.File;
import com.lasthopesoftware.bluewater.data.service.objects.OnFileCompleteListener;
import com.lasthopesoftware.bluewater.data.service.objects.OnFileErrorListener;
import com.lasthopesoftware.bluewater.data.service.objects.OnFilePreparedListener;
import com.lasthopesoftware.bluewater.data.sqlite.access.LibrarySession;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

public class FilePlayer implements
	OnPreparedListener, 
	OnErrorListener, 
	OnCompletionListener
{
	private static final Logger mLogger = LoggerFactory.getLogger(FilePlayer.class);
	
	private volatile MediaPlayer mp;
	private AtomicBoolean isPrepared = new AtomicBoolean();
	private AtomicBoolean isPreparing = new AtomicBoolean();
	private AtomicBoolean isInErrorState = new AtomicBoolean();
	private int mPosition = 0;
	private float mVolume = 1.0f;
	private final Context mMpContext;
	private final File mFile;
	
	private static final String FILE_URI_SCHEME = "file://";
	private static final String MEDIA_QUERY = "(" + 
												MediaStore.Audio.Media.DATA + " LIKE '%' || ? || '%') OR (" +
												MediaStore.Audio.Media.ARTIST + " = ? AND " +
												MediaStore.Audio.Media.ALBUM + " = ? AND " +
												MediaStore.Audio.Media.TITLE + " = ? AND " +
												MediaStore.Audio.Media.TRACK + " = ?" +
											  ")";
	
	private HashSet<OnFileCompleteListener> onFileCompleteListeners = new HashSet<OnFileCompleteListener>();
	private HashSet<OnFilePreparedListener> onFilePreparedListeners = new HashSet<OnFilePreparedListener>();
	private HashSet<OnFileErrorListener> onFileErrorListeners = new HashSet<OnFileErrorListener>();
	
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
	
		isPrepared.set(false);
		isPreparing.set(false);
		isInErrorState.set(false);
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
		    		// The file object will produce a properly escaped File URI, as opposed to what is stored in the DB
		    		final java.io.File file = new java.io.File(fileUriString.replaceFirst(FILE_URI_SCHEME, ""));
		    		
		    		if (file != null) return Uri.fromFile(file);
		    	}
		    }
	    } catch (IllegalArgumentException ie) {
	    	mLogger.info("Illegal column name.", ie);
	    } finally {
	    	cursor.close();
	    }
	    
	    final String itemUrl = mFile.getSubItemUrl();
	    if (itemUrl != null && !itemUrl.isEmpty())
	    	return Uri.parse(itemUrl);
	    
	    return null;
	}
	
	public void prepareMediaPlayer() {
		if (isPreparing.get() || isPrepared.get()) return;
		
		try {
			final Uri uri = getMpUri();
			if (uri != null) {
				setMpDataSource(uri);
				isPreparing.set(true);
				mLogger.info("Preparing " + mFile.getValue() + " asynchronously.");
				mp.prepareAsync();
			}
		} catch (IOException io) {
			throwIoErrorEvent();
			isPreparing.set(false);
		} catch (Exception e) {
			mLogger.error(e.toString(), e);
			resetMediaPlayer();
			isPreparing.set(false);
		}
	}
	
	public void prepareMpSynchronously() {
		if (isPreparing.get() || isPrepared.get()) return;
		
		try {
			final Uri uri = getMpUri();
			if (uri != null) {
				setMpDataSource(uri);
				
				isPreparing.set(true);
				mLogger.info("Preparing " + mFile.getValue() + " synchronously.");
				mp.prepare();
				isPrepared.set(true);
				return;
			}
			
			isPreparing.set(false);
		} catch (IOException io) {
			throwIoErrorEvent();
			isPreparing.set(false);
		} catch (Exception e) {
			mLogger.error(e.toString(), e);
			resetMediaPlayer();
			isPreparing.set(false);
		}
	}
	
	private void throwIoErrorEvent() {
		isInErrorState.set(true);
		resetMediaPlayer();
		for (OnFileErrorListener listener : onFileErrorListeners)
			listener.onJrFileError(this, MediaPlayer.MEDIA_ERROR_SERVER_DIED, MediaPlayer.MEDIA_ERROR_IO);
	}
	
	private void setMpDataSource(Uri uri) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException {
		final Map<String, String> headers = new HashMap<String, String>();
		if (mMpContext == null)
			throw new NullPointerException("The file player's context cannot be null");
		if (!LibrarySession.GetLibrary().getAuthKey().isEmpty())
			headers.put("Authorization", "basic " + LibrarySession.GetLibrary().getAuthKey());
		mp.setDataSource(mMpContext, uri, headers);
	}
	
	private void resetMediaPlayer() {
		
		final int position = getCurrentPosition();
		releaseMediaPlayer();
		
		initMediaPlayer();
		
		if (position > 0)
			seekTo(position);
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
		mLogger.info(mFile.getValue() + " prepared!");
		for (OnFilePreparedListener listener : onFilePreparedListeners) listener.onJrFilePrepared(this);
	}
	
	@Override
	public void onCompletion(MediaPlayer mp) {
		SimpleTask<Void, Void, Void> updateStatsTask = new SimpleTask<Void, Void, Void>(new UpdatePlayStatsOnExecute(mFile));
		updateStatsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		
		releaseMediaPlayer();
		for (OnFileCompleteListener listener : onFileCompleteListeners) listener.onJrFileComplete(this);
	}
	
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		mp.setOnErrorListener(null);
		isInErrorState.set(true);
		mLogger.error("Media Player error.");
		mLogger.error("What: ");
		mLogger.error(what == MediaPlayer.MEDIA_ERROR_UNKNOWN ? "MEDIA_ERROR_UNKNOWN" : "MEDIA_ERROR_SERVER_DIED");
		mLogger.error("Extra: ");
		switch (extra) {
			case MediaPlayer.MEDIA_ERROR_IO:
				mLogger.error("MEDIA_ERROR_IO");
				break;
			case MediaPlayer.MEDIA_ERROR_MALFORMED:
				mLogger.error("MEDIA_ERROR_MALFORMED");
				break;
			case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
				mLogger.error("MEDIA_ERROR_UNSUPPORTED");
				break;
			case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
				mLogger.error("MEDIA_ERROR_TIMED_OUT");
				break;
			case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
				mLogger.error("MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK");
				break;
			default:
				mLogger.error("Unknown");
				break;
		}
		resetMediaPlayer();
		
		for (OnFileErrorListener listener : onFileErrorListeners) listener.onJrFileError(this, what, extra);
		return true;
	}

	public int getBufferPercentage() {
		return mp == null ? 0 : (mp.getCurrentPosition() * 100) / mp.getDuration();
	}

	public int getCurrentPosition() {
		try {
			if (mp != null && isPrepared()) mPosition = mp.getCurrentPosition();
		} catch (IllegalStateException ie) {
			handleIllegalStateException(ie);
		}
		return mPosition;
	}
	
	public int getDuration() throws IOException {
		if (mp == null || isInErrorState.get() || !isPrepared())
			return mFile.getDuration();
		
		try {
			return mp.getDuration();
		} catch (IllegalStateException ie) {
			handleIllegalStateException(ie);
			return mFile.getDuration();
		}
	}

	public boolean isPlaying() {
		try {
			return mp != null && mp.isPlaying();
		} catch (IllegalStateException ie) {
			handleIllegalStateException(ie);
			return false;
		}
	}

	public void pause() {
		if (isPreparing.get()) {
			try {
				mp.reset();
			} catch (IllegalStateException e) {
				handleIllegalStateException(e);
				resetMediaPlayer();
				return;
			}
		}
		
		try {
			mPosition = mp.getCurrentPosition();
			mp.pause();
		} catch (IllegalStateException ie) {
			handleIllegalStateException(ie);
		}
	}

	public void seekTo(int pos) {
		mPosition = pos;
		try {
			if (mp != null && !isInErrorState.get() && isPrepared() && isPlaying()) mp.seekTo(mPosition);
		} catch (IllegalStateException ie) {
			handleIllegalStateException(ie);
		}
	}

	public void start() throws IllegalStateException {
		mLogger.info("Playback started on " + mFile.getValue());
		mp.seekTo(mPosition);
		mp.start();
	}
	
	public void stop() throws IllegalStateException {
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
	
	private void handleIllegalStateException(IllegalStateException ise) {
		mLogger.warn("The media player was in an incorrect state.", ise);
	}
	
	private static class UpdatePlayStatsOnExecute implements OnExecuteListener<Void, Void, Void> {
		private File mFile;
		
		public UpdatePlayStatsOnExecute(File file) {
			mFile = file;
		}
		
		@Override
		public Void onExecute(ISimpleTask<Void, Void, Void> owner, Void... params) throws Exception {
			try {
				final String numberPlaysString = mFile.getRefreshedProperty("Number Plays");
				
				int numberPlays = 0;
				if (numberPlaysString != null && !numberPlaysString.isEmpty()) numberPlays = Integer.parseInt(numberPlaysString);
				
				mFile.setProperty(FileProperties.NUMBER_PLAYS, String.valueOf(++numberPlays));	
				
				final String lastPlayed = String.valueOf(System.currentTimeMillis()/1000);
				mFile.setProperty(FileProperties.LAST_PLAYED, lastPlayed);
			} catch (IOException e) {
				mLogger.warn(e.toString(), e);
			} catch (NumberFormatException ne) {
				mLogger.error(ne.toString(), ne);
			}
			
			return null;
		}
	}
}
