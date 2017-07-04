package com.lasthopesoftware.bluewater.client.library.access;

import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.access.ItemResponse;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.queued.cancellation.CancellationToken;
import com.lasthopesoftware.providers.AbstractConnectionProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

public class LibraryViewsProvider extends AbstractConnectionProvider<List<Item>> {

    private static final Logger logger = LoggerFactory.getLogger(LibraryViewsProvider.class);

    public final static String browseLibraryParameter = "Browse/Children";

    private static List<Item> cachedFileSystemItems;
    private static Integer revision;

    private final ConnectionProvider connectionProvider;

    public static Promise<List<Item>> provide(ConnectionProvider connectionProvider) {
        return new LibraryViewsProvider(connectionProvider).promiseData();
    }

    public LibraryViewsProvider(ConnectionProvider connectionProvider) {
        super(connectionProvider, browseLibraryParameter);

        this.connectionProvider = connectionProvider;
    }

    @Override
    protected List<Item> getData(HttpURLConnection connection, CancellationToken cancellation) throws IOException {
        final Integer serverRevision = RevisionChecker.getRevision(connectionProvider);

        synchronized(browseLibraryParameter) {
            if (cachedFileSystemItems != null && revision.equals(serverRevision))
                return cachedFileSystemItems;
        }

        if (cancellation.isCancelled()) return new ArrayList<>();

        try {
            try (InputStream is = connection.getInputStream()) {
                final List<Item> items = ItemResponse.GetItems(connectionProvider, is);

                synchronized (browseLibraryParameter) {
                    revision = serverRevision;
                    cachedFileSystemItems = items;
                }

                return items;
            }
        } catch (IOException e) {
            logger.error("There was an error getting the inputstream", e);
            throw e;
        }
    }
}
