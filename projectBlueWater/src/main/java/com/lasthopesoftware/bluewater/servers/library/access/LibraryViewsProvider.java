package com.lasthopesoftware.bluewater.servers.library.access;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.Item;
import com.lasthopesoftware.bluewater.servers.library.items.access.AbstractCollectionProvider;
import com.lasthopesoftware.threading.ISimpleTask;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by david on 3/17/15.
 */
public class LibraryViewsProvider extends AbstractCollectionProvider<Item> {

    public final static String browseLibraryParameter = "Browse/Children";

    private static List<Item> mCachedFileSystemItems;
    private static Integer mRevision;

    public static LibraryViewsProvider provide() {
        return new LibraryViewsProvider();
    }

    public LibraryViewsProvider() {
        this(null);
    }

    public LibraryViewsProvider(HttpURLConnection connection) {
        super(connection, browseLibraryParameter);
    }

    @Override
    protected List<Item> getItems(ISimpleTask<Void, Void, List<Item>> task, HttpURLConnection connection, String... params) throws Exception {
        final Integer serverRevision = RevisionChecker.getRevision();

        if (mCachedFileSystemItems != null && mRevision.equals(serverRevision))
            return mCachedFileSystemItems;

        final HttpURLConnection conn = connection == null ? ConnectionProvider.getConnection(params) : connection;
        try {
            if (task.isCancelled()) return new ArrayList<>();

            final InputStream is = conn.getInputStream();
            try {
                final List<Item> items = FilesystemResponse.GetItems(is);

                mRevision = serverRevision;
                mCachedFileSystemItems = items;

                return items;
            } finally {
                is.close();
            }
        } finally {
            if (connection == null) conn.disconnect();
        }
    }
}
