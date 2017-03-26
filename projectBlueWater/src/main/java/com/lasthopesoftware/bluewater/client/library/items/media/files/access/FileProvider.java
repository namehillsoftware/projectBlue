package com.lasthopesoftware.bluewater.client.library.items.media.files.access;

import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.providers.AbstractInputStreamProvider;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by david on 11/25/15.
 */
public class FileProvider extends AbstractInputStreamProvider<List<ServiceFile>> {
	private static final Logger logger = LoggerFactory.getLogger(FileProvider.class);

	public FileProvider(ConnectionProvider connectionProvider, IFileListParameterProvider item) {
		this(connectionProvider, item, FileListParameters.Options.None);
	}

	private FileProvider(ConnectionProvider connectionProvider, IFileListParameterProvider item, FileListParameters.Options option) {
		this(connectionProvider, option, item.getFileListParameters());
	}

	FileProvider(ConnectionProvider connectionProvider, FileListParameters.Options option, String... parameters) {
		super(connectionProvider, FileListParameters.Helpers.processParams(option, parameters));
	}

	@Override
	protected List<ServiceFile> getData(InputStream is) {
		try {
			return FileStringListUtilities.parseFileStringList(IOUtils.toString(is));
		} catch (IOException e) {
			logger.warn("There was an error getting the serviceFile list", e);
			setException(e);
		}

		return new ArrayList<>();
	}
}
