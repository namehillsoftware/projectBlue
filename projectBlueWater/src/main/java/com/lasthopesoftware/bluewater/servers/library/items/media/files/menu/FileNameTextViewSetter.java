package com.lasthopesoftware.bluewater.servers.library.items.media.files.menu;

import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.connection.SessionConnection;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FilePropertiesProvider;

import java.io.FileNotFoundException;

/**
 * Created by david on 4/14/15.
 */
public class FileNameTextViewSetter {

	public static CachedFilePropertiesProvider startNew(IFile file, TextView textView) {
		textView.setText(R.string.lbl_loading);

		final CachedFilePropertiesProvider cachedFilePropertiesProvider = new CachedFilePropertiesProvider(SessionConnection.getSessionConnectionProvider(), file.getKey());
		cachedFilePropertiesProvider
				.onComplete(properties -> {
					if (cachedFilePropertiesProvider.isCancelled()) return;

					final String fileName = properties.get(FilePropertiesProvider.NAME);
					if (fileName != null)
						textView.setText(fileName);
				})
				.onError(exception -> {
					if (exception instanceof FileNotFoundException) {
						textView.setText(R.string.file_not_found);
						return true;
					}

					return false;
				})
				.execute();

		return cachedFilePropertiesProvider;
	}
}
