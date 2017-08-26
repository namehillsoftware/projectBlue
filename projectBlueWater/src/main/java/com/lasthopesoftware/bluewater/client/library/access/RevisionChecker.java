package com.lasthopesoftware.bluewater.client.library.access;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.shared.StandardRequest;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.queued.MessageTask;
import com.lasthopesoftware.messenger.promises.queued.QueuedPromise;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RevisionChecker implements MessageTask<Integer> {
	
	private static final Integer badRevision = -1;
    private static final long checkedExpirationTime = 30000;

	private static final Map<String, Integer> cachedRevisions = new HashMap<>();
	private static final Map<String, Long> lastRevisions = new HashMap<>();

	private static final ExecutorService revisionExecutor = Executors.newSingleThreadExecutor();

	private final IConnectionProvider connectionProvider;

	public static Promise<Integer> promiseRevision(IConnectionProvider connectionProvider) {
		return new QueuedPromise<>(new RevisionChecker(connectionProvider), revisionExecutor);
	}

    private static Integer getCachedRevision(IConnectionProvider connectionProvider) {
        final String serverUrl = connectionProvider.getUrlProvider().getBaseUrl();
        if (!cachedRevisions.containsKey(serverUrl))
            cachedRevisions.put(serverUrl, badRevision);

        return cachedRevisions.get(serverUrl);
    }

    private RevisionChecker(IConnectionProvider connectionProvider) {
	    this.connectionProvider = connectionProvider;
    }

	@Override
	public Integer prepareMessage() throws Throwable {
		return getRevision();
	}

	private Integer getRevision() {
		final String baseServerUrl = connectionProvider.getUrlProvider().getBaseUrl();
		final Long lastRevisionCheckedTime = lastRevisions.get(baseServerUrl);
        if (lastRevisionCheckedTime != null && !getCachedRevision(connectionProvider).equals(badRevision) && System.currentTimeMillis() - checkedExpirationTime < lastRevisionCheckedTime) {
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

					if (revisionValue == null || revisionValue.isEmpty()) return badRevision;

					cachedRevisions.put(baseServerUrl, Integer.valueOf(revisionValue));
					lastRevisions.put(baseServerUrl, System.currentTimeMillis());
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
