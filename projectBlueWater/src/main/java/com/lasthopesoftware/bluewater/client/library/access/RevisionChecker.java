package com.lasthopesoftware.bluewater.client.library.access;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.shared.StandardRequest;
import com.vedsoft.fluent.FluentTask;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RevisionChecker extends FluentTask<Void, Void, Integer> {
	
	private final static Integer mBadRevision = -1;
    private static final Map<String, Integer> cachedRevisions = new HashMap<>();

    private static long mLastCheckedTime = -1;
    private final static long mCheckedExpirationTime = 30000;

    private static final ExecutorService revisionExecutor = Executors.newSingleThreadExecutor();

	private final IConnectionProvider connectionProvider;

	public static Integer getRevision(IConnectionProvider connectionProvider) {
        try {
            return (new RevisionChecker(connectionProvider)).get(revisionExecutor);
        } catch (ExecutionException | InterruptedException e) {
            return getCachedRevision(connectionProvider);
        }
    }

    private static Integer getCachedRevision(IConnectionProvider connectionProvider) {
        final String serverUrl = connectionProvider.getUrlProvider().getBaseUrl();
        if (!cachedRevisions.containsKey(serverUrl))
            cachedRevisions.put(serverUrl, mBadRevision);

        return cachedRevisions.get(serverUrl);
    }

    public RevisionChecker(IConnectionProvider connectionProvider) {
	    this.connectionProvider = connectionProvider;
    }

    @Override
    protected Integer executeInBackground(Void... params) {
        if (!getCachedRevision(connectionProvider).equals(mBadRevision) && System.currentTimeMillis() - mCheckedExpirationTime < mLastCheckedTime) {
            return getCachedRevision(connectionProvider);
        }

        try {
            final HttpURLConnection conn = connectionProvider.getConnection("Library/GetRevision");
            try {
                final InputStream is = conn.getInputStream();
                try {
                    final StandardRequest standardRequest = StandardRequest.fromInputStream(is);
                    if (standardRequest == null)
                        return getCachedRevision(connectionProvider);

                    final String revisionValue = standardRequest.items.get("Sync");

                    if (revisionValue == null || revisionValue.isEmpty()) return mBadRevision;

                    cachedRevisions.put(connectionProvider.getUrlProvider().getBaseUrl(), Integer.valueOf(revisionValue));
                    mLastCheckedTime = System.currentTimeMillis();
                    return getCachedRevision(connectionProvider);
                } finally {
                    is.close();
                }
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            return getCachedRevision(connectionProvider);
        }
    }
}
