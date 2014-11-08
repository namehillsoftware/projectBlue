package com.lasthopesoftware.bluewater.data.service.access;

import java.io.InputStream;
import java.net.HttpURLConnection;

import com.lasthopesoftware.bluewater.data.service.access.connection.ConnectionManager;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

public class RevisionTask implements OnExecuteListener<Void, Void, String> {

	public static SimpleTask<Void, Void, String> GetNewRevisionTask() {
		return new SimpleTask<Void, Void, String>(new RevisionTask());
	}

	@Override
	public String onExecute(ISimpleTask<Void, Void, String> owner, Void... params) throws Exception {
		final HttpURLConnection conn = ConnectionManager.getConnection("Library/GetRevision");
		try {
			final InputStream is = conn.getInputStream();
			try {
				return StandardRequest.fromInputStream(is).items.get("Master");
			} finally {
				is.close();
			}
		} finally {
			conn.disconnect();
		}
	}
}
