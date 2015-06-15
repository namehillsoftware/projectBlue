package com.lasthopesoftware.bluewater.servers.library.items.access;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.access.FilesystemResponse;
import com.lasthopesoftware.bluewater.servers.library.access.LibraryViewsProvider;
import com.lasthopesoftware.bluewater.servers.library.access.RevisionChecker;
import com.lasthopesoftware.bluewater.servers.library.items.Item;
import com.lasthopesoftware.threading.ISimpleTask;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

public class ItemProvider extends AbstractCollectionProvider<Item> {

    private static class ItemHolder {
        public ItemHolder(Integer revision, List<Item> items) {
            this.revision = revision;
            this.items = items;
        }

        public final Integer revision;
        public final List<Item> items;
    }

    private static final int maxSize = 500;
    private static final ConcurrentLinkedHashMap<Integer, ItemHolder> mItemsCache = new ConcurrentLinkedHashMap
                                                                                            .Builder<Integer, ItemHolder>()
                                                                                            .maximumWeightedCapacity(maxSize)
                                                                                            .build();

    private final int mItemKey;

	public static ItemProvider provide(int itemKey) {
		return new ItemProvider(itemKey);
	}

	public ItemProvider(int itemKey) {
		this(null, itemKey);
	}
	
	public ItemProvider(HttpURLConnection connection, int itemKey) {
		super(connection, LibraryViewsProvider.browseLibraryParameter, "ID=" + String.valueOf(itemKey), "Version=2");

        mItemKey = itemKey;
	}

    protected List<Item> getItems(ISimpleTask<Void, Void, List<Item>> task, HttpURLConnection connection, String... params) throws Exception {
        final Integer serverRevision = RevisionChecker.getRevision();
        final Integer boxedItemKey = mItemKey;
        ItemHolder itemHolder = mItemsCache.get(boxedItemKey);
        if (itemHolder != null && itemHolder.revision.equals(serverRevision))
            return itemHolder.items;

        final HttpURLConnection conn = connection == null ? ConnectionProvider.getConnection(params) : connection;
        try {
            if (task.isCancelled()) return new ArrayList<>();

            final InputStream is = conn.getInputStream();
            try {
                final List<Item> items = FilesystemResponse.GetItems(is);

                itemHolder = new ItemHolder(serverRevision, items);
                mItemsCache.put(boxedItemKey, itemHolder);

                return items;
            } finally {
                is.close();
            }
        } finally {
            if (connection == null) conn.disconnect();
        }
	}
}
