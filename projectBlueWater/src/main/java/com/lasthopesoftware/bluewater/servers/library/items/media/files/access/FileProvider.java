package com.lasthopesoftware.bluewater.servers.library.items.media.files.access;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.providers.AbstractCollectionProvider;
import com.lasthopesoftware.threading.ISimpleTask;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;

/**
 * Created by david on 11/25/15.
 */
public class FileProvider extends AbstractCollectionProvider<IFile> {
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
	protected List<IFile> getData(ISimpleTask<Void, Void, List<IFile>> task, HttpURLConnection connection) throws Exception {
		final InputStream is = connection.getInputStream();
		try {
			return FileStringListUtilities.parseFileStringList(connectionProvider, IOUtils.toString(is));
		} finally {
			is.close();
		}
	}
}
