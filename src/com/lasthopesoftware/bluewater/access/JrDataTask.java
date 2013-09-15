package com.lasthopesoftware.bluewater.access;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

import com.lasthopesoftware.bluewater.FileSystem.IJrDataTask;

import android.os.AsyncTask;

public class JrDataTask<TResult> extends AsyncTask<String, Void, TResult> implements IJrDataTask<TResult> {

	LinkedList<OnConnectListener<TResult>> onConnectListeners = new LinkedList<IJrDataTask.OnConnectListener<TResult>>();
	LinkedList<OnCompleteListener<TResult>> onCompleteListeners = new LinkedList<IJrDataTask.OnCompleteListener<TResult>>();
	LinkedList<OnStartListener> onStartListeners = new LinkedList<OnStartListener>();
	LinkedList<OnErrorListener> onErrorListeners = new LinkedList<OnErrorListener>();
	ArrayList<TResult> mResults;
	
	@Override
	protected void onPreExecute() {
		for (OnStartListener listener : onStartListeners) listener.onStart();
	}
	
	@Override
	protected TResult doInBackground(String... params) {
		if (mResults == null) mResults = new ArrayList<TResult>();
		mResults.clear();
		JrConnection conn;
		try {
			conn = new JrConnection(params);
			for (OnConnectListener<TResult> workEvent : onConnectListeners) mResults.add(workEvent.onConnect(conn.getInputStream()));
		} catch (IOException ioEx) {
			boolean executeAgain = true;
			
			for (OnErrorListener errorListener : onErrorListeners) executeAgain &= errorListener.onError(ioEx.getMessage());
			if (executeAgain) return doInBackground(params);
			
			return null;
		}
		return mResults.get(mResults.size() - 1);
	}
	
	public ArrayList<TResult> getResults() {
		return mResults;
	}
	
	@Override
	protected void onPostExecute(TResult result) {
		for (OnCompleteListener<TResult> completeListener : onCompleteListeners) completeListener.onComplete(result);
	}
	
	@Override
	public void addOnStartListener(OnStartListener listener) {
		if (onStartListeners == null) onStartListeners = new LinkedList<OnStartListener>();
		onStartListeners.add(listener);
	}
	
	@Override
	public void addOnConnectListener(OnConnectListener<TResult> listener) {
		if (onConnectListeners == null) onConnectListeners = new LinkedList<OnConnectListener<TResult>>();
		onConnectListeners.add(listener);
	}
	
	@Override
	public void addOnCompleteListener(IJrDataTask.OnCompleteListener<TResult> listener) {
		if (onCompleteListeners == null) onCompleteListeners = new LinkedList<OnCompleteListener<TResult>>();
		onCompleteListeners.add(listener);
	}

	@Override
	public void addOnErrorListener(com.lasthopesoftware.bluewater.FileSystem.IJrDataTask.OnErrorListener listener) {
		if (onErrorListeners == null) onErrorListeners = new LinkedList<OnErrorListener>();
		onErrorListeners.add(listener);
	}

	@Override
	public LinkedList<com.lasthopesoftware.bluewater.FileSystem.IJrDataTask.OnCompleteListener<TResult>> getOnCompleteListeners() {
		return onCompleteListeners;
	}
}
