package com.lasthopesoftware.bluewater.data.objects;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;

import xmlwise.XmlElement;
import xmlwise.XmlParseException;
import xmlwise.Xmlwise;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.AsyncTask;
import android.os.PowerManager;

import com.lasthopesoftware.bluewater.data.access.connection.JrConnection;
import com.lasthopesoftware.bluewater.data.access.connection.JrTestConnection;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;

public class JrFile extends JrObject implements
	OnPreparedListener, 
	OnErrorListener, 
	OnCompletionListener	
{
	private ConcurrentSkipListMap<String, String> mProperties = null;
	
	private boolean prepared = false;
	private boolean preparing = false;
	private int mPosition = 0;
	private MediaPlayer mp;
	private LinkedList<OnJrFileCompleteListener> onJrFileCompleteListeners = new LinkedList<OnJrFileCompleteListener>();;
	private LinkedList<OnJrFilePreparedListener> onJrFilePreparedListeners = new LinkedList<OnJrFilePreparedListener>();
	private LinkedList<OnJrFileErrorListener> onJrFileErrorListeners = new LinkedList<OnJrFileErrorListener>();
	private JrFile mNextFile, mPreviousFile;
	
	public JrFile(int key) {
		super();
		this.setKey(key);
	}
	
	public JrFile(int key, String value) {
		super(key, value);
	}
	
	public JrFile() {
		super();
	}
	

	/**
	 * @return the Value
	 */
	@Override
	public String getValue() {
		if (super.getValue() == null) {
			try {
				setValue(getRefreshedProperty("Name"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return super.getValue();
	}
	
	public void addOnJrFileCompleteListener(OnJrFileCompleteListener listener) {
		onJrFileCompleteListeners.add(listener);
	}
	
	public void removeOnJrFileCompleteListener(OnJrFileCompleteListener listener) {
		if (onJrFileCompleteListeners.contains(listener)) onJrFileCompleteListeners.remove(listener);
	}
	
	public void addOnJrFilePreparedListener(OnJrFilePreparedListener listener) {
		onJrFilePreparedListeners.add(listener);
	}
	
	public void removeOnJrFilePreparedListener(OnJrFilePreparedListener listener) {
		if (onJrFilePreparedListeners.contains(listener)) onJrFilePreparedListeners.remove(listener);
	}
	
	public void addOnJrFileErrorListener(OnJrFileErrorListener listener) {
		onJrFileErrorListeners.add(listener);
	}
	
	public void removeOnJrFileErrorListener(OnJrFileErrorListener listener) {
		if (onJrFileErrorListeners.contains(listener)) onJrFileErrorListeners.remove(listener);
	}
	
	public String getSubItemUrl() {
		/* Playback:
		 * 0: Downloading (not real-time playback);
		 * 1: Real-time playback with update of playback statistics, Scrobbling, etc.; 
		 * 2: Real-time playback, no playback statistics handling (default: )
		 */
		return JrSession.accessDao.getJrUrl("File/GetFile", "File=" + Integer.toString(getKey()), "Quality=medium", "Conversion=Android", "Playback=0");
	}
	
	private String getMpUrl() {
		if (!JrTestConnection.doTest()) {
			for (OnJrFileErrorListener listener : onJrFileErrorListeners) listener.onJrFileError(this, MediaPlayer.MEDIA_ERROR_SERVER_DIED, MediaPlayer.MEDIA_ERROR_IO);
			return null;
		}
		return getSubItemUrl();
	}
	
	/**
	 * @return the prepared
	 */
	public boolean isPrepared() {
		return prepared;
	}
		
	public JrFile getNextFile() {
		return mNextFile;
	}
	
	public void setNextFile(JrFile file) {
		mNextFile = file;
	}
	
	public JrFile getPreviousFile() {
		return mPreviousFile;
	}
	
	public void setPreviousFile(JrFile file) {
		mPreviousFile = file;
	}
		
	public void setProperty(String name, String value) {
		if (mProperties.containsKey(name) && mProperties.get(name).equals(value)) return;
		
		Thread setPropertyThread = new Thread(new SetProperty(getKey(), name, value));
		setPropertyThread.setName(setPropertyThread.getName() + "setting property");
		setPropertyThread.setPriority(Thread.MIN_PRIORITY);
		setPropertyThread.start();
		mProperties.put(name, value);
	}
	
	public String getProperty(String name) throws IOException {
		if (mProperties == null || mProperties.size() == 0) {
			try {
				mProperties = GetFilePropertiesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "File/GetInfo", "File=" + String.valueOf(getKey())).get();
				if (GetFilePropertiesTask().getState() == SimpleTaskState.ERROR) {
					for (Exception e : GetFilePropertiesTask().getExceptions()) {
						if (e instanceof IOException) throw (IOException)e;
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		
		return mProperties.get(name);
	}
	
	public String getRefreshedProperty(String name) throws IOException {
		
		try {
			mProperties.putAll(GetFilePropertiesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "File/GetInfo", "File=" + String.valueOf(getKey()), mProperties != null ? ("Fields=" + name) : "").get());
			if (GetFilePropertiesTask().getState() == SimpleTaskState.ERROR) {
				for (Exception e : GetFilePropertiesTask().getExceptions()) {
					if (e instanceof IOException) throw (IOException)e;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return getProperty(name);
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
	
	public boolean isMediaPlayerCreated() {
		return mp != null;
	}
	
//	public synchronized MediaPlayer getMediaPlayer() {
//		return mp;
//	}
	
	public void prepareMediaPlayer() {
		if (!preparing && !prepared) {
			try {
				String url = getMpUrl();
				if (!url.isEmpty()) {
					mp.setDataSource(url);
					mp.prepareAsync();
					preparing = true;
					return;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void prepareMpSynchronously() {
		if (!preparing && !prepared) {
			try {
				String url = getMpUrl();
				if (!url.isEmpty()) {
					mp.setDataSource(url);
					preparing = true;
					mp.prepare();
					prepared = true;
					return;
				}
				
				preparing = false;
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
		Thread updateStatsThread = new Thread(new UpdatePlayStats(this));
		updateStatsThread.setName("Asynchronous Update Stats Thread for " + getValue());
		updateStatsThread.setPriority(Thread.MIN_PRIORITY);
		updateStatsThread.start();
		
		releaseMediaPlayer();
		for (OnJrFileCompleteListener listener : onJrFileCompleteListeners) listener.onJrFileComplete(this);
	}
	
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		mp.reset();
		boolean handled = false;
		for (OnJrFileErrorListener listener : onJrFileErrorListeners) handled |= listener.onJrFileError(this, what, extra);
		if (handled) releaseMediaPlayer();
		return handled;
	}

	public int getBufferPercentage() {
		return (mp.getCurrentPosition() * 100) / mp.getDuration();
	}

	public int getCurrentPosition() {
		if (mp != null && isPrepared() && isPlaying()) mPosition = mp.getCurrentPosition();
		return mPosition;
	}
	
	public int getDuration() throws IOException {
		if (mp == null) {
			return (int) (Double.parseDouble(getProperty("Duration")) * 1000);
		}
		return mp.getDuration();
	}

	public boolean isPlaying() {
		return mp != null && mp.isPlaying();
	}

	public void pause() {
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
	
	public void setVolume(float volume) {
		mp.setVolume(volume, volume);
	}
	
	private SimpleTask<String, Void, ConcurrentSkipListMap<String, String>> GetFilePropertiesTask() {
		SimpleTask<String, Void, ConcurrentSkipListMap<String,String>> filePropertiesTask = new SimpleTask<String, Void, ConcurrentSkipListMap<String,String>>();
		filePropertiesTask.addOnExecuteListener(new OnExecuteListener<String, Void, ConcurrentSkipListMap<String,String>>() {
			
			@Override
			public void onExecute(ISimpleTask<String, Void, ConcurrentSkipListMap<String, String>> owner, String... params) throws IOException {
				ConcurrentSkipListMap<String, String> returnProperties = new ConcurrentSkipListMap<String, String>();
				
				JrConnection conn;
				try {
					conn = new JrConnection(params);
			    	XmlElement xml = Xmlwise.createXml(JrFileUtils.InputStreamToString(conn.getInputStream()));
			    	
			    	for (XmlElement el : xml.get(0)) {
			    		returnProperties.put(el.getAttribute("Name"), el.getValue());
			    	}
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (XmlParseException e) {
					e.printStackTrace();
				}
				
				owner.setResult(returnProperties != null ? returnProperties : new ConcurrentSkipListMap<String, String>());
			}
		});
		
		filePropertiesTask.addOnErrorListener(new com.lasthopesoftware.threading.ISimpleTask.OnErrorListener<String, Void, ConcurrentSkipListMap<String,String>>() {
			
			@Override
			public boolean onError(ISimpleTask<String, Void, ConcurrentSkipListMap<String, String>> owner, Exception innerException) {
				return !(innerException instanceof IOException);
			}
		});
		
		return filePropertiesTask;
	}
	
	private static class SetProperty implements Runnable {
		private int mKey;
		private String mName;
		private String mValue;
		
		public SetProperty(int key, String name, String value) {
			mKey = key;
			mName = name;
			mValue = value;
		}
		
		@Override
		public void run() {
			try {
				JrConnection conn = new JrConnection("File/SetInfo", "File=" + String.valueOf(mKey), "Field=" + mName, "Value=" + mValue);
				conn.getInputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static class UpdatePlayStats implements Runnable {
		private JrFile mFile;
		
		public UpdatePlayStats(JrFile file) {
			mFile = file;
		}
		
		@Override
		public void run() {
			int numberPlays;
			try {
				numberPlays = Integer.parseInt(mFile.getRefreshedProperty("Number Plays"));
				mFile.setProperty("Number Plays", String.valueOf(++numberPlays));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			String lastPlayed = String.valueOf(System.currentTimeMillis()/1000);
			mFile.setProperty("Last Played", lastPlayed);
		}
	}
}
