package com.lasthopesoftware.bluewater.data.service.access;

import java.io.InputStream;

import com.lasthopesoftware.threading.ISimpleTask;

public interface IDataTask<TResult> extends ISimpleTask<String, Void, TResult> {

	void addOnConnectListener(OnConnectListener<TResult> listener);
	void removeOnConnectListener(OnConnectListener<TResult> listener);
	
	public interface OnConnectListener<TResult> {
		TResult onConnect(InputStream is);
	}
	
	/* Events */
	public interface OnStartListener<TResult> extends ISimpleTask.OnStartListener<String, Void, TResult> {
	}
		
	public interface OnProgressListener<TResult> extends ISimpleTask.OnProgressListener<String, Void, TResult> {
	}
	
	public interface OnCompleteListener<TResult> extends ISimpleTask.OnCompleteListener<String, Void, TResult> {
	}
		
	public interface OnErrorListener<TResult> extends ISimpleTask.OnErrorListener<String, Void, TResult> {
	}
}
