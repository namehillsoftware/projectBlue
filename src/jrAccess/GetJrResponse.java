package jrAccess;

import android.os.AsyncTask;

public class GetJrResponse<T> extends AsyncTask<String, Void, T> {

	@Override
	protected T doInBackground(String... params) {
		// Add base url
		String url = JrSession.accessDao.getActiveUrl();
		
		// Add action
		url += params[1];
		
		// Add token
		url += "?Token=" + JrSession.accessDao.getToken();
		
		// add arguments
		if (params.length > 2) {
			for (int i = 3; i < params.length; i++) {
				url += "&" + params[i];
			}
		}
		
		return null;
		//return new T();
	}

}
