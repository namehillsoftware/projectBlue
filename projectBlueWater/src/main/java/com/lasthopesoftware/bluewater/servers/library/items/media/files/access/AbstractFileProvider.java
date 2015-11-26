package com.lasthopesoftware.bluewater.servers.library.items.media.files.access;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.File;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.providers.AbstractCollectionProvider;
import com.lasthopesoftware.threading.ISimpleTask;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by david on 11/25/15.
 */
public abstract class AbstractFileProvider extends AbstractCollectionProvider<IFile> {
	public static final int GET_SHUFFLED = 1;
	private final ConnectionProvider connectionProvider;


	protected AbstractFileProvider(ConnectionProvider connectionProvider, int option, String... params) {
		super(connectionProvider, processParams(option, params));

		this.connectionProvider = connectionProvider;
	}

	private static String[] processParams(int option, String... params) {
		final ArrayList<String> newParams = new ArrayList<>(Arrays.asList(params));
		newParams.add("Action=Serialize");
		if (option == GET_SHUFFLED)
			newParams.add("Shuffle=1");
		return newParams.toArray(new String[newParams.size()]);
	}

	@Override
	protected List<IFile> getCollection(ISimpleTask<Void, Void, List<IFile>> task, HttpURLConnection connection) throws Exception {
		final InputStream is = connection.getInputStream();
		try {
			return parseFileStringList(connectionProvider, IOUtils.toString(is));
		} finally {
			is.close();
		}
	}

	public static ArrayList<IFile> parseFileStringList(ConnectionProvider connectionProvider, String fileList) {
		final String[] keys = fileList.split(";");

		final int offset = Integer.parseInt(keys[0]) + 1;
		final ArrayList<IFile> files = new ArrayList<>(Integer.parseInt(keys[1]));

		for (int i = offset; i < keys.length; i++) {
			if (keys[i].equals("-1")) continue;

			files.add(new File(connectionProvider, Integer.parseInt(keys[i])));
		}

		return files;
	}
}
