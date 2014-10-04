package com.lasthopesoftware.bluewater.data.service.access;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.lasthopesoftware.bluewater.data.service.access.connection.ConnectionManager;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTask;

public class DataTask<TResult> extends SimpleTask<String, Void, TResult> implements IDataTask<TResult> {

	ConcurrentLinkedQueue<OnConnectListener<TResult>> onConnectListeners = new ConcurrentLinkedQueue<OnConnectListener<TResult>>();
	ArrayList<TResult> mResults;
	
	public DataTask() {
		super();
		super.setOnExecuteListener(new OnExecuteListener<String, Void, TResult>() {

			@Override
			public TResult onExecute(ISimpleTask<String, Void, TResult> owner, String... params) throws Exception {
				if (mResults == null) mResults = new ArrayList<TResult>();
				mResults.clear();

				final HttpURLConnection conn = ConnectionManager.getConnection(params);
				try {
					final InputStream is = conn.getInputStream();
					try {
						for (OnConnectListener<TResult> workEvent : onConnectListeners)
							mResults.add(workEvent.onConnect(is));
					} finally {
						is.close();
					}
				} finally {
					conn.disconnect();
				}
				
				return mResults.get(mResults.size() - 1);
			}
		});
	}
		
	public ArrayList<TResult> getResults() {
		return mResults;
	}

	@Override
	public void addOnConnectListener(OnConnectListener<TResult> listener) {
		onConnectListeners.add(listener);
	}
	
	@Override
	public void setOnExecuteListener(OnExecuteListener<String, Void, TResult> listener) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("The OnExecuteListener operation is not supported in the Jr Data Task. Please use the OnConnectListener.");
	}

	@Override
	public void removeOnConnectListener(com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnConnectListener<TResult> listener) {
		onConnectListeners.remove(listener);
	}
}
