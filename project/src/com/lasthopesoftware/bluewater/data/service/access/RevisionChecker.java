package com.lasthopesoftware.bluewater.data.service.access;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.concurrent.atomic.AtomicInteger;

import com.lasthopesoftware.bluewater.data.service.access.connection.ConnectionManager;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

public class RevisionChecker implements OnExecuteListener<Void, Void, Boolean> {

	private static final AtomicInteger mRevision = new AtomicInteger(-1);
	
	public static SimpleTask<Void, Void, Boolean> getIsNewRevisionTask() {
		return new SimpleTask<Void, Void, Boolean>(new RevisionChecker());
	}

	@Override
	public Boolean onExecute(ISimpleTask<Void, Void, Boolean> owner, Void... params) throws Exception {
		final HttpURLConnection conn = ConnectionManager.getConnection("Library/GetRevision");
		try {
			final InputStream is = conn.getInputStream();
			try {
				final String revisionValue = StandardRequest.fromInputStream(is).items.get("Sync");
				
				// Return that it is a new revision if a revision was not returned
				if (revisionValue == null || revisionValue.isEmpty()) return Boolean.TRUE;
								
				final int newRevision = Integer.parseInt(revisionValue);
				
				if (newRevision == mRevision.get()) return Boolean.FALSE;
				
				mRevision.set(newRevision);
				return Boolean.TRUE;
			} finally {
				is.close();
			}
		} finally {
			conn.disconnect();
		}
	}
}
