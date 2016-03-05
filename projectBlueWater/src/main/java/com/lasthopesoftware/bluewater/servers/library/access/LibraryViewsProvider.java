package com.lasthopesoftware.bluewater.servers.library.access;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.Item;
import com.lasthopesoftware.bluewater.servers.library.items.access.ItemResponse;
import com.lasthopesoftware.providers.AbstractConnectionProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by david on 3/17/15.
 */
public class LibraryViewsProvider extends AbstractConnectionProvider<List<Item>> {

    private static final Logger logger = LoggerFactory.getLogger(LibraryViewsProvider.class);

    public final static String browseLibraryParameter = "Browse/Children";

    private static List<Item> cachedFileSystemItems;
    private static Integer revision;

    private final ConnectionProvider connectionProvider;

    public static LibraryViewsProvider provide(ConnectionProvider connectionProvider) {
        return new LibraryViewsProvider(connectionProvider);
    }

    public LibraryViewsProvider(ConnectionProvider connectionProvider) {
        super(connectionProvider, browseLibraryParameter);

        this.connectionProvider = connectionProvider;
    }

    @Override
    protected List<Item> getData(HttpURLConnection connection) {
        final Integer serverRevision = RevisionChecker.getRevision(connectionProvider);

        synchronized(browseLibraryParameter) {
            if (cachedFileSystemItems != null && revision.equals(serverRevision))
                return cachedFileSystemItems;
        }

        if (isCancelled()) return new ArrayList<>();

        try {
            final InputStream is = connection.getInputStream();
            try {
                final List<Item> items = ItemResponse.GetItems(connectionProvider, is);

                synchronized (browseLibraryParameter) {
                    revision = serverRevision;
                    cachedFileSystemItems = items;
                }

                return items;
            } finally {
                is.close();
            }
        } catch (IOException e) {
            logger.error("There was an error getting the inputstream", e);
            setException(e);
            return new ArrayList<>();
        }
    }
}
