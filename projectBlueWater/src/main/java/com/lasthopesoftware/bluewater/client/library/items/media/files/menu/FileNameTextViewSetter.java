package com.lasthopesoftware.bluewater.client.library.items.media.files.menu;

import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.SessionConnection;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.vedsoft.fluent.FluentSpecifiedTask;
import com.vedsoft.fluent.IFluentTask;
import com.vedsoft.futures.callables.OneParameterCallable;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

import java.io.FileNotFoundException;
import java.util.Map;

/**
 * Created by david on 4/14/15.
 */
public class FileNameTextViewSetter implements TwoParameterRunnable<IFluentTask<Integer, Void, Map<String, String>>, Map<String, String>>, OneParameterCallable<Exception, Boolean>, Runnable {

	private final TextView textView;

	public static CachedFilePropertiesProvider startNew(IFile file, TextView textView) {
		final FileNameTextViewSetter fileNameTextViewSetter = new FileNameTextViewSetter(textView);
		final CachedFilePropertiesProvider cachedFilePropertiesProvider = new CachedFilePropertiesProvider(SessionConnection.getSessionConnectionProvider(), file.getKey());
		cachedFilePropertiesProvider
				.beforeStart(fileNameTextViewSetter)
				.onComplete(fileNameTextViewSetter)
				.onError(fileNameTextViewSetter)
				.execute();

		return cachedFilePropertiesProvider;
	}

	private FileNameTextViewSetter(TextView textView) {
		this.textView = textView;
	}

	@Override
	public Boolean call(Exception exception) {
		if (exception instanceof FileNotFoundException) {
			textView.setText(R.string.file_not_found);
			return true;
		}

		return false;
	}

	@Override
	public void run(IFluentTask<Integer, Void, Map<String, String>> provider, Map<String, String> properties) {
		if (provider.isCancelled()) return;

		final String fileName = properties.get(FilePropertiesProvider.NAME);

		if (fileName != null)
			textView.setText(fileName);
	}

	@Override
	public void run() {
		textView.setText(R.string.lbl_loading);
	}
}
