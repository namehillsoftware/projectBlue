package com.lasthopesoftware.bluewater.client.library.items.media.files.menu;

import android.os.Handler;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.SessionConnection;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.shared.promises.resolutions.Dispatch;
import com.lasthopesoftware.promises.IPromise;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;

import java.util.Map;

/**
 * Created by david on 4/14/15.
 */
public class FileNameTextViewSetter implements CarelessOneParameterFunction<Map<String, String>, Void>, Runnable {

	private final TextView textView;

	public static IPromise<Map<String, String>> startNew(IFile file, TextView textView) {
		final FileNameTextViewSetter fileNameTextViewSetter = new FileNameTextViewSetter(textView);
		final Handler handler = new Handler(textView.getContext().getMainLooper());
		handler.post(fileNameTextViewSetter);

		final IConnectionProvider connectionProvider = SessionConnection.getSessionConnectionProvider();
		final FilePropertyCache filePropertyCache = FilePropertyCache.getInstance();
		final CachedFilePropertiesProvider cachedFilePropertiesProvider = new CachedFilePropertiesProvider(connectionProvider, filePropertyCache, new FilePropertiesProvider(connectionProvider, filePropertyCache));

		final IPromise<Map<String, String>> promise = cachedFilePropertiesProvider.promiseFileProperties(file.getKey());

		promise.then(Dispatch.toHandler(fileNameTextViewSetter, handler));

		return promise;
	}

	private FileNameTextViewSetter(TextView textView) {
		this.textView = textView;
	}

	@Override
	public void run() {
		textView.setText(R.string.lbl_loading);
	}

	@Override
	public Void resultFrom(Map<String, String> properties) {
		final String fileName = properties.get(FilePropertiesProvider.NAME);

		if (fileName != null)
			textView.setText(fileName);

		return null;
	}
}
