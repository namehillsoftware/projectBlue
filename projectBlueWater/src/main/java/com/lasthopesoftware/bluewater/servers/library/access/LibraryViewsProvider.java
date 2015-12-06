package com.lasthopesoftware.bluewater.servers.library.access;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.Item;
import com.lasthopesoftware.bluewater.servers.library.items.access.ItemResponse;
import com.lasthopesoftware.providers.AbstractCollectionProvider;
import com.lasthopesoftware.threading.IFluentTask;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by david on 3/17/15.
 */
public class LibraryViewsProvider extends AbstractCollectionProvider<Item> {

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
    protected List<Item> getData(IFluentTask<Void, Void, List<Item>> task, HttpURLConnection connection) throws Exception {
        final Integer serverRevision = RevisionChecker.getRevision(connectionProvider);

        synchronized(browseLibraryParameter) {
            if (cachedFileSystemItems != null && revision.equals(serverRevision))
                return cachedFileSystemItems;
        }

        if (task.isCancelled()) return new ArrayList<>();

        final InputStream is = connection.getInputStream();
        try {
            final List<Item> items = ItemResponse.GetItems(connectionProvider, is);

            synchronized(browseLibraryParameter) {
                revision = serverRevision;
                cachedFileSystemItems = items;
            }

            return items;
        } finally {
            is.close();
        }
    }
}
