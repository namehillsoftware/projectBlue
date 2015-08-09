package com.lasthopesoftware.bluewater.servers.library.access;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.shared.StandardRequest;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RevisionChecker implements OnExecuteListener<Void, Void, Integer> {
	
	private final static Integer mBadRevision = -1;
	private static Integer mCachedRevision = mBadRevision;

    private static long mLastCheckedTime = -1;
    private final static long mCheckedExpirationTime = 30000;

    private static final ExecutorService mRevisionExecutor = Executors.newSingleThreadExecutor();

	public static Integer getRevision() {
        try {
            return (new SimpleTask<>(new RevisionChecker())).execute(mRevisionExecutor).get();
        } catch (ExecutionException | InterruptedException e) {
            return mCachedRevision;
        }
    }

	@Override
	public Integer onExecute(ISimpleTask<Void, Void, Integer> owner, Void... params) throws Exception {
        if (!mCachedRevision.equals(mBadRevision) && System.currentTimeMillis() - mCheckedExpirationTime < mLastCheckedTime) {
            return mCachedRevision;
        }

        final HttpURLConnection conn = ConnectionProvider.getActiveConnection("Library/GetRevision");
        try {
            final InputStream is = conn.getInputStream();
            try {
                final String revisionValue = StandardRequest.fromInputStream(is).items.get("Sync");

                if (revisionValue == null || revisionValue.isEmpty()) return mBadRevision;

                mCachedRevision = Integer.valueOf(revisionValue);
                mLastCheckedTime = System.currentTimeMillis();
                return mCachedRevision;
            } finally {
                is.close();
            }
        } catch (Exception e) {
            return mCachedRevision;
        } finally {
            conn.disconnect();
        }
	}
}
