package com.lasthopesoftware.bluewater.data.service.access;

import java.io.InputStream;
import java.net.HttpURLConnection;

import com.lasthopesoftware.bluewater.data.service.access.connection.ConnectionManager;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

public class RevisionChecker implements OnExecuteListener<Void, Void, Integer> {
	
	private final static Integer mBadRevision = Integer.valueOf(-1);
	
	public static SimpleTask<Void, Void, Integer> getRevisionTask() {
		return new SimpleTask<Void, Void, Integer>(new RevisionChecker());
	}

	@Override
	public Integer onExecute(ISimpleTask<Void, Void, Integer> owner, Void... params) throws Exception {
		final HttpURLConnection conn = ConnectionManager.getConnection("Library/GetRevision");
		try {
			final InputStream is = conn.getInputStream();
			try {
				final String revisionValue = StandardRequest.fromInputStream(is).items.get("Sync");
				
				if (revisionValue == null || revisionValue.isEmpty()) return mBadRevision;
								
				return Integer.valueOf(revisionValue);
			} finally {
				is.close();
			}
		} catch (Exception e) {
			return mBadRevision;
		} finally {
			conn.disconnect();
		}
	}
}
