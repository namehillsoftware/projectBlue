package jrFileSystem;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.widget.MediaController.MediaPlayerControl;
import jrAccess.JrConnection;
import jrAccess.JrFilePropertiesHandler;
import jrAccess.JrFileXmlHandler;
import jrAccess.JrSession;
import jrAccess.JrTestConnection;

public class JrFile extends JrListing implements
	OnPreparedListener, 
	OnErrorListener, 
	OnCompletionListener	
{
	private TreeMap<String, String> mProperties= null;
	
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
	
	public void setProperty(String name, String value) {
		Thread setPropertyThread = new Thread(new SetProperty(getKey(), name, value));
		setPropertyThread.setName(setPropertyThread.getName() + "setting property");
		setPropertyThread.start();
		mProperties.put(name, value);
	}
	
	public String getProperty(String name) {
		if (mProperties == null) {
			try {
				mProperties = new JrFilePropertyResponse().execute("File/GetInfo", "File=" + String.valueOf(getKey())).get();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return mProperties.get(name);
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
	
	public synchronized MediaPlayer getMediaPlayer() {
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

	public int getBufferPercentage() {
		return (mp.getCurrentPosition() * 100) / mp.getDuration();
	}

	public int getCurrentPosition() {
		return mp.getCurrentPosition();
	}

	public boolean isPlaying() {
		return mp != null && mp.isPlaying();
	}

	public void pause() {
		mp.pause();
	}

	public void seekTo(int pos) {
		mp.seekTo(pos);
	}

	public void start() {
		mp.start();
	}
	
	private static class JrFilePropertyResponse extends AsyncTask<String, Void, TreeMap<String, String>> {

		@Override
		protected TreeMap<String, String> doInBackground(String... params) {
			TreeMap<String, String> returnProperties = new TreeMap<String, String>();
			
			JrConnection conn;
			try {
				conn = new JrConnection(params);
				SAXParserFactory parserFactory = SAXParserFactory.newInstance();
				SAXParser sp = parserFactory.newSAXParser();
		    	JrFilePropertiesHandler jrFileProperties = new JrFilePropertiesHandler();
		    	sp.parse(conn.getInputStream(), jrFileProperties);
		    	
		    	returnProperties = jrFileProperties.getProperties();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return returnProperties != null ? returnProperties : new TreeMap<String, String>();
		}
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
		
		public void run() {
			try {
				JrConnection conn = new JrConnection("File/SetInfo", "File=" + String.valueOf(mKey), "Field=" + mName, "Value=" + mValue);
				conn.getInputStream();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
	}
}
