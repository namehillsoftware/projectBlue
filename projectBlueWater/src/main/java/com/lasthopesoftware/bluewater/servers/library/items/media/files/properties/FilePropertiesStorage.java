package com.lasthopesoftware.bluewater.servers.library.items.media.files.properties;

import com.lasthopesoftware.bluewater.servers.connection.IConnectionProvider;
import com.lasthopesoftware.providers.AbstractInputStreamProvider;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by david on 3/5/16.
 */
public class FilePropertiesStorage extends AbstractInputStreamProvider<Void> {
	public static void storeFileProperty(IConnectionProvider connectionProvider, int fileKey, String property, String value) {
		new FilePropertiesStorage(connectionProvider, fileKey, property, value).execute();
	}

	public FilePropertiesStorage(IConnectionProvider connectionProvider, int fileKey, String property, String value) {
		super(connectionProvider, "File/SetInfo", "File=" + String.valueOf(fileKey), "Field=" + property, "Value=" + value);
	}

	@Override
	protected Void getData(InputStream inputStream) {
		try {
			inputStream.close();
		} catch (IOException e) {
			setException(e);
		}

		return null;
	}
}
