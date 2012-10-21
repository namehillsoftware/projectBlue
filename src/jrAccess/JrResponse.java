package jrAccess;

import android.os.AsyncTask;

public abstract class JrResponse<T> extends AsyncTask<String, Void, T> {


	@Override
	protected T doInBackground(String... params) {
		return null;
	}

}
