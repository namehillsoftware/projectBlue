package com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.PowerManager;

import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.listeners.OnFileBufferedListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.listeners.OnFileCompleteListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.listeners.OnFileErrorListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.listeners.OnFilePreparedListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.servers.store.Library;
import com.lasthopesoftware.bluewater.shared.IoCommon;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlaybackFile implements
	IPlaybackFile,
	OnPreparedListener,
	OnErrorListener, 
	OnCompletionListener,
	OnBufferingUpdateListener
{
	@SuppressLint("InlinedApi")
	public static final Set<Integer> MEDIA_ERROR_EXTRAS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(new Integer[] {
		MediaPlayer.MEDIA_ERROR_IO,
		MediaPlayer.MEDIA_ERROR_MALFORMED,
		MediaPlayer.MEDIA_ERROR_UNSUPPORTED, 
		MediaPlayer.MEDIA_ERROR_TIMED_OUT, 
		MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK
	}))); 
	
	private static final Logger mLogger = LoggerFactory.getLogger(PlaybackFile.class);
	
	private volatile MediaPlayer mMediaPlayer;
	
	// FilePlayer State Variables
	private volatile boolean mIsPrepared = false;
	private volatile boolean mIsPreparing = false;
	private volatile boolean mIsInErrorState = false;
	private volatile int mPosition = 0;
	private volatile int mBufferPercentage = 0;
	private volatile int mLastBufferPercentage = 0;
	private volatile float mVolume = 1.0f;
	
	private final Context mMpContext;
	private final IFile mFile;
	
	private static final int mBufferMin = 0, mBufferMax = 100;

	private final HashSet<OnFileCompleteListener> onFileCompleteListeners = new HashSet<>();
	private final HashSet<OnFilePreparedListener> onFilePreparedListeners = new HashSet<>();
	private final HashSet<OnFileErrorListener> onFileErrorListeners = new HashSet<>();
	private final HashSet<OnFileBufferedListener> onFileBufferedListeners = new HashSet<>();
	
	public PlaybackFile(Context context, IFile file) {
		mMpContext = context;
		mFile = file;
	}
	
	public IFile getFile() {
		return mFile;
	}
	
	public void initMediaPlayer() {
		if (mMediaPlayer != null) return;
		
		mIsPrepared = false;
		mIsPreparing = false;
		mIsInErrorState = false;
		mBufferPercentage = mBufferMin;
		
		mMediaPlayer = new MediaPlayer(); // initialize it here
		mMediaPlayer.setOnPreparedListener(this);
		mMediaPlayer.setOnErrorListener(this);
		mMediaPlayer.setOnCompletionListener(this);
		mMediaPlayer.setOnBufferingUpdateListener(this);
		mMediaPlayer.setWakeMode(mMpContext, PowerManager.PARTIAL_WAKE_LOCK);
		mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
	}
	
	public boolean isMediaPlayerCreated() {
		return mMediaPlayer != null;
	}
	
	public boolean isPrepared() {
		return mIsPrepared;
	}

	private Uri getFileUri() throws IOException {
		final BestMatchUriProvider bestMatchUriProvider = new BestMatchUriProvider(mMpContext, LibrarySession.GetActiveLibrary(mMpContext), mFile);
		return bestMatchUriProvider.getFileUri();
	}

	public void prepareMediaPlayer() {
		if (mIsPreparing || mIsPrepared) return;
		
		try {
			final Uri uri = getFileUri();
			if (uri == null) return;
			
			setMpDataSource(uri);
			initializeBufferPercentage(uri);
			
			mIsPreparing = true;
			
			mLogger.info("Preparing " + mFile.getValue() + " asynchronously.");
			mMediaPlayer.prepareAsync();
		} catch (FileNotFoundException fe) {
			mLogger.error(fe.toString(), fe);
			resetMediaPlayer();
			mIsPreparing = false;
		} catch (IOException io) {
			throwIoErrorEvent();
			mIsPreparing = false;
		} catch (Exception e) {
			mLogger.error(e.toString(), e);
			resetMediaPlayer();
			mIsPreparing = false;
		}
	}
	
	public void prepareMpSynchronously() {
		if (mIsPreparing || mIsPrepared) return;
		
		try {
			final Uri uri = getFileUri();
			if (uri == null) return;
			
			setMpDataSource(uri);
			initializeBufferPercentage(uri);
			
			mIsPreparing = true;
			
			mLogger.info("Preparing " + mFile.getValue() + " synchronously.");
			mMediaPlayer.prepare();
			
			mIsPrepared = true;
			mIsPreparing = false;
		} catch (FileNotFoundException fe) {
			mLogger.error(fe.toString(), fe);
			resetMediaPlayer();
			mIsPreparing = false;
		} catch (IOException io) {
			throwIoErrorEvent();
			mIsPreparing = false;
		} catch (Exception e) {
			mLogger.error(e.toString(), e);
			resetMediaPlayer();
			mIsPreparing = false;
		}
	}
	
	private void initializeBufferPercentage(Uri uri) {
		final String scheme = uri.getScheme();
		mBufferPercentage = IoCommon.FileUriScheme.equalsIgnoreCase(scheme) ? mBufferMax : mBufferMin;
		mLogger.info("Initialized " + scheme + " type URI buffer percentage to " + String.valueOf(mBufferPercentage));
	}
	
	private void throwIoErrorEvent() {
		mIsInErrorState = true;
		resetMediaPlayer();
		
		for (OnFileErrorListener listener : onFileErrorListeners)
			listener.onFileError(this, MediaPlayer.MEDIA_ERROR_SERVER_DIED, MediaPlayer.MEDIA_ERROR_IO);
	}
	
	private void setMpDataSource(Uri uri) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException {
		final Map<String, String> headers = new HashMap<>();
		if (mMpContext == null)
			throw new NullPointerException("The file player's context cannot be null");

		if (!uri.getScheme().equalsIgnoreCase(IoCommon.FileUriScheme)) {
			final Library library = LibrarySession.GetActiveLibrary(mMpContext);
			if (library != null) {
				final String authKey = library.getAuthKey();

				if (authKey != null && !authKey.isEmpty())
					headers.put("Authorization", "basic " + authKey);
			}
		}
		
		mMediaPlayer.setDataSource(mMpContext, uri, headers);
	}
	
	private void resetMediaPlayer() {
		final int position = getCurrentPosition();
		releaseMediaPlayer();
		
		initMediaPlayer();
		
		if (position > 0) seekTo(position);
	}
	
	public void releaseMediaPlayer() {
		if (mMediaPlayer != null) mMediaPlayer.release();
		mMediaPlayer = null;
		mIsPrepared = false;
	}
	
	@Override
	public void onPrepared(MediaPlayer mp) {
		mIsPrepared = true;
		mIsPreparing = false;
		mLogger.info(mFile.getValue() + " prepared!");
		
		for (OnFilePreparedListener listener : onFilePreparedListeners) listener.onFilePrepared(this);
	}
	
	@Override
	public void onCompletion(MediaPlayer mp) {
		AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
			
			@Override
			public void run() {
				try {
					final String lastPlayedString = mFile.getProperty(FilePropertiesProvider.LAST_PLAYED);
					// Only update the last played data if the song could have actually played again
					if (lastPlayedString == null || (System.currentTimeMillis() - getDuration()) > Long.valueOf(lastPlayedString))
						SimpleTask.executeNew(AsyncTask.THREAD_POOL_EXECUTOR, new UpdatePlayStatsOnExecute(mFile));
				} catch (NumberFormatException e) {
					mLogger.error("There was an error parsing the last played time.");
				} catch (IOException e) {
					mLogger.warn("There was an error retrieving the duration or last played time data.");
				}
			}
		});
		
		releaseMediaPlayer();
		for (OnFileCompleteListener listener : onFileCompleteListeners) listener.onFileComplete(this);
	}
	
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		mp.setOnErrorListener(null);
		mIsInErrorState = true;
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
		
		for (OnFileErrorListener listener : onFileErrorListeners) listener.onFileError(this, what, extra);
		return true;
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		// Handle weird exceptional behavior seen online http://stackoverflow.com/questions/21925454/android-mediaplayer-onbufferingupdatelistener-percentage-of-buffered-content-i
        if (percent < 0 || percent > 100) {
            mLogger.warn("Buffering percentage was bad: " + String.valueOf(percent));
            percent = (int) Math.round((((Math.abs(percent)-1)*100.0/Integer.MAX_VALUE)));
        }
        
		mBufferPercentage = percent;
		
		if (!isBuffered()) return;
		
		// remove the listener
		mp.setOnBufferingUpdateListener(null);
		
		for (OnFileBufferedListener onFileBufferedListener : onFileBufferedListeners)
			onFileBufferedListener.onFileBuffered(this);
	}
	
	public int getCurrentPosition() {
		try {
			if (mMediaPlayer != null && isPrepared()) mPosition = mMediaPlayer.getCurrentPosition();
		} catch (IllegalStateException ie) {
			handleIllegalStateException(ie);
		}
		return mPosition;
	}
	
	public boolean isBuffered() {
		if (mLastBufferPercentage != mBufferPercentage) {
			mLastBufferPercentage = mBufferPercentage;
			mLogger.info("Buffer percentage: " + String.valueOf(mBufferPercentage) + "% Buffer Threshold: " + String.valueOf(mBufferMax) + "%");
		}
		return mBufferPercentage >= mBufferMax;
	}
	
	public int getBufferPercentage() {
		return mBufferPercentage;
	}
	
	public int getDuration() throws IOException {
		if (mMediaPlayer == null || mIsInErrorState || !isPlaying())
			return mFile.getDuration();
		
		try {
			return mMediaPlayer.getDuration();
		} catch (IllegalStateException ie) {
			handleIllegalStateException(ie);
			return mFile.getDuration();
		}
	}

	public boolean isPlaying() {
		try {
			return mMediaPlayer != null && mMediaPlayer.isPlaying();
		} catch (IllegalStateException ie) {
			handleIllegalStateException(ie);
			return false;
		}
	}

	public void pause() {
		if (mIsPreparing) {
			try {
				mMediaPlayer.reset();
			} catch (IllegalStateException e) {
				handleIllegalStateException(e);
				resetMediaPlayer();
				return;
			}
		}
		
		try {
			mPosition = mMediaPlayer.getCurrentPosition();
			mMediaPlayer.pause();
		} catch (IllegalStateException ie) {
			handleIllegalStateException(ie);
		}
	}

	public void seekTo(int pos) {
		mPosition = pos;
		try {
			if (mMediaPlayer != null && !mIsInErrorState && isPrepared() && isPlaying()) mMediaPlayer.seekTo(mPosition);
		} catch (IllegalStateException ie) {
			handleIllegalStateException(ie);
		}
	}

	public void start() throws IllegalStateException {
		mLogger.info("Playback started on " + mFile.getValue());
		mMediaPlayer.seekTo(mPosition);
		mMediaPlayer.start();
	}
	
	public void stop() throws IllegalStateException {
		mPosition = 0;
		mMediaPlayer.stop();
	}
	
	public float getVolume() {
		return mVolume;
	}
	
	public void setVolume(float volume) {
		mVolume = volume;
		
		if (mMediaPlayer != null)
			mMediaPlayer.setVolume(mVolume, mVolume);
	}
	
	private static void handleIllegalStateException(IllegalStateException ise) {
		mLogger.warn("The media player was in an incorrect state.", ise);
	}
	
	private static class UpdatePlayStatsOnExecute implements OnExecuteListener<Void, Void, Void> {
		private final IFile mFile;
		
		public UpdatePlayStatsOnExecute(IFile file) {
			mFile = file;
		}
		
		@Override
		public Void onExecute(ISimpleTask<Void, Void, Void> owner, Void... params) throws Exception {
			try {
				final String numberPlaysString = mFile.getRefreshedProperty(FilePropertiesProvider.NUMBER_PLAYS);
				
				int numberPlays = 0;
				if (numberPlaysString != null && !numberPlaysString.isEmpty()) numberPlays = Integer.parseInt(numberPlaysString);
				
				mFile.setProperty(FilePropertiesProvider.NUMBER_PLAYS, String.valueOf(++numberPlays));
				
				final String lastPlayed = String.valueOf(System.currentTimeMillis()/1000);
				mFile.setProperty(FilePropertiesProvider.LAST_PLAYED, lastPlayed);
			} catch (IOException e) {
				mLogger.warn(e.toString(), e);
			} catch (NumberFormatException ne) {
				mLogger.error(ne.toString(), ne);
			}
			
			return null;
		}
	}
	
	/* Listener methods */
	public void addOnFileCompleteListener(OnFileCompleteListener listener) {
		onFileCompleteListeners.add(listener);
	}
	
	public void removeOnFileCompleteListener(OnFileCompleteListener listener) {
		onFileCompleteListeners.remove(listener);
	}
	
	public void addOnFilePreparedListener(OnFilePreparedListener listener) {
		onFilePreparedListeners.add(listener);
	}
	
	public void removeOnFilePreparedListener(OnFilePreparedListener listener) {
		onFilePreparedListeners.remove(listener);
	}
	
	public void addOnFileErrorListener(OnFileErrorListener listener) {
		onFileErrorListeners.add(listener);
	}
	
	public void removeOnFileErrorListener(OnFileErrorListener listener) {
		onFileErrorListeners.remove(listener);
	}
	
	public void addOnFileBufferedListener(OnFileBufferedListener listener) {
		onFileBufferedListeners.add(listener);
	}
	
	public void removeOnFileErrorListener(OnFileBufferedListener listener) {
		onFileBufferedListeners.remove(listener);
	}
}
