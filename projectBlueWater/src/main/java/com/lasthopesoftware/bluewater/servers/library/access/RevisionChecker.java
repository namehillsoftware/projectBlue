package com.lasthopesoftware.bluewater.servers.library.access;

import android.util.SparseIntArray;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.shared.StandardRequest;
import com.lasthopesoftware.threading.FluentTask;
import com.lasthopesoftware.threading.OnExecuteListener;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RevisionChecker implements OnExecuteListener<Void, Void, Integer> {
	
	private final static Integer mBadRevision = -1;
    private static final SparseIntArray cachedRevisions = new SparseIntArray();

    private static long mLastCheckedTime = -1;
    private final static long mCheckedExpirationTime = 30000;

    private static final ExecutorService revisionExecutor = Executors.newSingleThreadExecutor();

	private final ConnectionProvider connectionProvider;

	public static Integer getRevision(ConnectionProvider connectionProvider) {
        try {
            return (new FluentTask<>(new RevisionChecker(connectionProvider))).execute(revisionExecutor).get();
        } catch (ExecutionException | InterruptedException e) {
            return getCachedRevision(connectionProvider);
        }
    }

    private static Integer getCachedRevision(ConnectionProvider connectionProvider) {
        final int libraryId = connectionProvider.getAccessConfiguration().getLibraryId();
        if (cachedRevisions.indexOfKey(libraryId) < 0)
            cachedRevisions.put(libraryId, mBadRevision);

        return cachedRevisions.get(libraryId);
    }

    private RevisionChecker(ConnectionProvider connectionProvider) {
	    this.connectionProvider = connectionProvider;
    }

	@Override
	public Integer onExecute(FluentTask<Void, Void, Integer> owner, Void... params) throws Exception {
        if (!getCachedRevision(connectionProvider).equals(mBadRevision) && System.currentTimeMillis() - mCheckedExpirationTime < mLastCheckedTime) {
            return getCachedRevision(connectionProvider);
        }

        final HttpURLConnection conn = connectionProvider.getConnection("Library/GetRevision");
        try {
            final InputStream is = conn.getInputStream();
            try {
	            final StandardRequest standardRequest = StandardRequest.fromInputStream(is);
	            if (standardRequest == null)
		            return getCachedRevision(connectionProvider);

                final String revisionValue = standardRequest.items.get("Sync");

                if (revisionValue == null || revisionValue.isEmpty()) return mBadRevision;

                cachedRevisions.put(connectionProvider.getAccessConfiguration().getLibraryId(), Integer.valueOf(revisionValue));
                mLastCheckedTime = System.currentTimeMillis();
                return getCachedRevision(connectionProvider);
            } finally {
                is.close();
            }
        } catch (Exception e) {
            return getCachedRevision(connectionProvider);
        } finally {
            conn.disconnect();
        }
	}
}
