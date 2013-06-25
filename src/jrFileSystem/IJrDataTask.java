package jrFileSystem;

import java.io.InputStream;
import java.util.LinkedList;

public interface IJrDataTask<TResult> {

	void addOnStartListener(OnStartListener listener);
	void addOnConnectListener(OnConnectListener<TResult> listener);
	void addOnCompleteListener(OnCompleteListener<TResult> listener);
	void addOnErrorListener(OnErrorListener listener);
	
	LinkedList<OnCompleteListener<TResult>> getOnCompleteListeners();
	
	/* Events */
	public interface OnStartListener {
		void onStart();
	}
	
	public interface OnConnectListener<TResult> {
		TResult onConnect(InputStream is);
	}
	
	public interface OnCompleteListener<TResult> {
		void onComplete(TResult result);
	}
	
	public interface OnErrorListener {
		boolean onError(String message);
	}
}
