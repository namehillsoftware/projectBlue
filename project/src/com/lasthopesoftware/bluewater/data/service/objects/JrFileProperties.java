package com.lasthopesoftware.bluewater.data.service.objects;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

import xmlwise.XmlElement;
import xmlwise.XmlParseException;
import xmlwise.Xmlwise;
import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.data.service.access.connection.JrConnection;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;

public class JrFileProperties {
	private Integer mFileKey;
	private ConcurrentSkipListMap<String, String> mProperties = null;
	private static ExecutorService fileStatsExecutor = Executors.newSingleThreadExecutor();
	private static ConcurrentSkipListMap<Integer, ConcurrentSkipListMap<String, String>> mPropertiesCache = new ConcurrentSkipListMap<Integer, ConcurrentSkipListMap<String,String>>();
	
	public JrFileProperties(Integer fileKey) {
		
		mFileKey = fileKey;
		
		if (mPropertiesCache.containsKey(mFileKey)) mProperties = mPropertiesCache.get(mFileKey);
		
		if (mProperties == null) {
			mProperties = new ConcurrentSkipListMap<String, String>(String.CASE_INSENSITIVE_ORDER); 
			mPropertiesCache.put(mFileKey, mProperties);
		}
	}
	
	public void setProperty(String name, String value) {
		if (mProperties.containsKey(name) && mProperties.get(name).equals(value)) return;

		AsyncTask<String, Void, Boolean> setPropertyTask = new AsyncTask<String, Void, Boolean>() {
			
			@Override
			protected Boolean doInBackground(String... params) {
				try {
					JrConnection conn = new JrConnection("File/SetInfo", "File=" + params[0], "Field=" + params[1], "Value=" + params[2]);
					conn.setReadTimeout(5000);
					conn.getInputStream();
					return true;
				} catch (Exception e) {
					return false;
				}
			}
		};
		setPropertyTask.executeOnExecutor(fileStatsExecutor, String.valueOf(mFileKey), name, value);
		
		mProperties.put(name, value);
	}
	
	public String getProperty(String name) throws IOException {
		
		if (mProperties.size() == 0 || !mProperties.containsKey(name))
			return getRefreshedProperty(name);
		
		return mProperties.get(name);
	}
	
	public String getRefreshedProperty(String name) throws IOException {
		String result = null;
		
		// Much simpler to just refresh all properties, and shouldn't be very costly (compared to just getting the basic property)
		SimpleTask<String, Void, Map<String,String>> filePropertiesTask = new SimpleTask<String, Void, Map<String,String>>();
		filePropertiesTask.addOnExecuteListener(new OnExecuteListener<String, Void, Map<String,String>>() {
			
			@Override
			public void onExecute(ISimpleTask<String, Void, Map<String, String>> owner, String... params) throws IOException {
				HashMap<String, String> returnProperties = new HashMap<String, String>();

				try {
					JrConnection conn = new JrConnection("File/GetInfo", "File=" + String.valueOf(mFileKey));
					conn.setReadTimeout(45000);
					try {
				    	XmlElement xml = Xmlwise.createXml(IOUtils.toString(conn.getInputStream()));
				    	if (xml.size() < 1) return;
				    	returnProperties = new HashMap<String, String>(xml.get(0).size());
				    	for (XmlElement el : xml.get(0))
				    		returnProperties.put(el.getAttribute("Name"), el.getValue());
					} finally {
						conn.disconnect();
					}
				} catch (MalformedURLException e) {
					LoggerFactory.getLogger(JrFile.class).error(e.toString(), e);
				} catch (XmlParseException e) {
					LoggerFactory.getLogger(JrFile.class).error(e.toString(), e);
				}
				
				owner.setResult(returnProperties);
			}
		});
		
		filePropertiesTask.addOnErrorListener(new com.lasthopesoftware.threading.ISimpleTask.OnErrorListener<String, Void, Map<String,String>>() {
			
			@Override
			public boolean onError(ISimpleTask<String, Void, Map<String, String>> owner, Exception innerException) {
				return !(innerException instanceof IOException);
			}
		});

		try {
			Map<String, String> filePropertiesResult = filePropertiesTask.executeOnExecutor(fileStatsExecutor).get();
			
			if (filePropertiesTask.getState() == SimpleTaskState.ERROR) {
				for (Exception e : filePropertiesTask.getExceptions()) {
					if (e instanceof IOException) throw (IOException)e;
				}
			}
			
			if (filePropertiesResult == null) return mProperties.containsKey(name) ? mProperties.get(name) : null;
			
			if (filePropertiesResult.containsKey(name))
				result = filePropertiesResult.get(name);
			
			mProperties.putAll(filePropertiesResult);
		} catch (InterruptedException e) {
			LoggerFactory.getLogger(JrFile.class).error(e.toString(), e);
		} catch (ExecutionException e) {
			LoggerFactory.getLogger(JrFile.class).error(e.toString(), e);
		}
		
		return result;
	}
}
