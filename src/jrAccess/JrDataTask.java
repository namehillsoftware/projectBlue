package jrAccess;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.ConnectionSettings;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.ViewUtils;

public class JrDataTask<TResult> extends AsyncTask<String, Void, TResult> {

	LinkedList<OnConnectListener<TResult>> onWorkListeners;
	LinkedList<OnCompleteListener<TResult>> onCompleteListeners;
	LinkedList<OnStartListener> onStartListeners;
	ArrayList<TResult> mResults;
	private Context mContext;
	
	public JrDataTask(Context context) {
		mContext = context;
	}
	
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
			for (OnConnectListener<TResult> workEvent : onWorkListeners) mResults.add(workEvent.onConnect(conn.getInputStream()));
		} catch (IOException ioEx) {
			if (ViewUtils.OkCancelDialog(mContext, mContext.getString(R.string.lbl_connection_lost_title), mContext.getString(R.string.lbl_connection_lost))) {
				return doInBackground(params);
			}
			
			Intent connectIntent = new Intent(mContext, ConnectionSettings.class);
			mContext.startActivity(connectIntent);
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
	
	public void addOnStartListener(OnStartListener listener) {
		if (onStartListeners == null) onStartListeners = new LinkedList<OnStartListener>();
		onStartListeners.add(listener);
	}
	
	public void addOnConnectListener(OnConnectListener<TResult> listener) {
		if (onWorkListeners == null) onWorkListeners = new LinkedList<OnConnectListener<TResult>>();
		onWorkListeners.add(listener);
	}
	
	public void addOnCompleteListener(OnCompleteListener<TResult> listener) {
		if (onCompleteListeners == null) onCompleteListeners = new LinkedList<OnCompleteListener<TResult>>();
		onCompleteListeners.add(listener);
	}
	
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
}
