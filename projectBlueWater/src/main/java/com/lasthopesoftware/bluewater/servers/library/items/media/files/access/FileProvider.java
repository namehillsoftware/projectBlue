package com.lasthopesoftware.bluewater.servers.library.items.media.files.access;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.providers.AbstractCollectionProvider;

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
public class FileProvider extends AbstractCollectionProvider<IFile> {
	private static final Logger logger = LoggerFactory.getLogger(FileProvider.class);
	private final ConnectionProvider connectionProvider;

	public FileProvider(ConnectionProvider connectionProvider, IFileListParameterProvider item) {
		this(connectionProvider, item, FileListParameters.Options.None);
	}

	private FileProvider(ConnectionProvider connectionProvider, IFileListParameterProvider item, FileListParameters.Options option) {
		this(connectionProvider, option, item.getFileListParameters());
	}

	FileProvider(ConnectionProvider connectionProvider, FileListParameters.Options option, String... parameters) {
		super(connectionProvider, FileListParameters.Helpers.processParams(option, parameters));

		this.connectionProvider = connectionProvider;
	}

	@Override
	protected List<IFile> getData(InputStream is) {
		try {
			return FileStringListUtilities.parseFileStringList(connectionProvider, IOUtils.toString(is));
		} catch (IOException e) {
			logger.warn("There was an error getting the file list", e);
			setException(e);
		}

		return new ArrayList<>();
	}
}
