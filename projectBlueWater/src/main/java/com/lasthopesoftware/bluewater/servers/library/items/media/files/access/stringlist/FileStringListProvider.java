package com.lasthopesoftware.bluewater.servers.library.items.media.files.access.stringlist;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.access.FileListParameters;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.access.IFileListParameterProvider;
import com.lasthopesoftware.providers.AbstractInputStreamProvider;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by david on 11/26/15.
 */
public class FileStringListProvider extends AbstractInputStreamProvider<String> {
	private static final Logger logger = LoggerFactory.getLogger(FileStringListProvider.class);

	public FileStringListProvider(ConnectionProvider connectionProvider, IFileListParameterProvider item) {
		this(connectionProvider, item, FileListParameters.Options.None);
	}

	public FileStringListProvider(ConnectionProvider connectionProvider, IFileListParameterProvider item, FileListParameters.Options option) {
		super(connectionProvider, FileListParameters.Helpers.processParams(option, item.getFileListParameters()));
	}

	@Override
	protected String getData(InputStream inputStream) {
		try {
			return IOUtils.toString(inputStream);
		} catch (IOException e) {
			logger.error("Error reading string from stream", e);
			return null;
		}
	}
}
