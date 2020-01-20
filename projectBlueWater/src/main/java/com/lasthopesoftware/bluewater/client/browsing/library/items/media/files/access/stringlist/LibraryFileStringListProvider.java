package com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.access.stringlist;

import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;

import okhttp3.Response;
import okhttp3.ResponseBody;

public final class LibraryFileStringListProvider implements ImmediateResponse<Response, String> {
	private final ProvideLibraryConnections libraryConnections;

	public LibraryFileStringListProvider(ProvideLibraryConnections libraryConnections) {
		this.libraryConnections = libraryConnections;
	}

	public Promise<String> promiseFileStringList(LibraryId libraryId, FileListParameters.Options option, String... params) {
		return libraryConnections
			.promiseLibraryConnection(libraryId)
			.eventually(connection -> connection.promiseResponse(FileListParameters.Helpers.processParams(option, params)))
			.then(this);
	}

	@Override
	public String respond(Response response) throws Throwable {
		final ResponseBody body = response.body();
		if (body == null) return null;

		try {
			return body.string();
		} finally {
			body.close();
		}
	}
}
