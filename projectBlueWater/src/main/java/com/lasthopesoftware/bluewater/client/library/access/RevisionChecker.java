package com.lasthopesoftware.bluewater.client.library.access;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.shared.StandardRequest;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.queued.QueuedPromise;
import com.vedsoft.futures.callables.CarelessFunction;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RevisionChecker implements Callable<Integer>, CarelessFunction<Integer> {
	
	private final static Integer mBadRevision = -1;
    private static final Map<String, Integer> cachedRevisions = new HashMap<>();

    private static long mLastCheckedTime = -1;
    private final static long mCheckedExpirationTime = 30000;

    private static final ExecutorService revisionExecutor = Executors.newSingleThreadExecutor();

	private final IConnectionProvider connectionProvider;

	public static Integer getRevision(IConnectionProvider connectionProvider) {
        try {
            return revisionExecutor.submit(new RevisionChecker(connectionProvider)).get();
        } catch (ExecutionException | InterruptedException e) {
            return getCachedRevision(connectionProvider);
        }
    }

    public static Promise<Integer> promiseRevision(IConnectionProvider connectionProvider) {
		return new QueuedPromise<>(new RevisionChecker(connectionProvider), revisionExecutor);
	}

    private static Integer getCachedRevision(IConnectionProvider connectionProvider) {
        final String serverUrl = connectionProvider.getUrlProvider().getBaseUrl();
        if (!cachedRevisions.containsKey(serverUrl))
            cachedRevisions.put(serverUrl, mBadRevision);

        return cachedRevisions.get(serverUrl);
    }

    private RevisionChecker(IConnectionProvider connectionProvider) {
	    this.connectionProvider = connectionProvider;
    }

	@Override
	public Integer result() throws Throwable {
		return getRevision();
	}

	@Override
	public Integer call() throws Exception {
		return getRevision();
	}

    private Integer getRevision() {
        if (!getCachedRevision(connectionProvider).equals(mBadRevision) && System.currentTimeMillis() - mCheckedExpirationTime < mLastCheckedTime) {
            return getCachedRevision(connectionProvider);
        }

        try {
            final HttpURLConnection conn = connectionProvider.getConnection("Library/GetRevision");
            try {
				try (InputStream is = conn.getInputStream()) {
					final StandardRequest standardRequest = StandardRequest.fromInputStream(is);
					if (standardRequest == null)
						return getCachedRevision(connectionProvider);

					final String revisionValue = standardRequest.items.get("Sync");

					if (revisionValue == null || revisionValue.isEmpty()) return mBadRevision;

					cachedRevisions.put(connectionProvider.getUrlProvider().getBaseUrl(), Integer.valueOf(revisionValue));
					mLastCheckedTime = System.currentTimeMillis();
					return getCachedRevision(connectionProvider);
				}
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            return getCachedRevision(connectionProvider);
        }
    }
}
