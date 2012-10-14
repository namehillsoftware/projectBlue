package jrAccess;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.os.AsyncTask;

public abstract class JrResponse<T> extends AsyncTask<String, Void, T> {


	@Override
	protected T doInBackground(String... params) {
		return null;
	}

}
