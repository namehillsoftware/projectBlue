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

    private static final int maxSize = 50;
    private static final ConcurrentLinkedHashMap<Integer, ItemHolder> itemsCache = new ConcurrentLinkedHashMap
                                                                                            .Builder<Integer, ItemHolder>()
                                                                                            .maximumWeightedCapacity(maxSize)
                                                                                            .build();

    private final int itemKey;

	private final ConnectionProvider connectionProvider;

	public static ItemProvider provide(ConnectionProvider connectionProvider, int itemKey) {
		return new ItemProvider(connectionProvider, itemKey);
	}
	
	public ItemProvider(ConnectionProvider connectionProvider, int itemKey) {
		super(connectionProvider, LibraryViewsProvider.browseLibraryParameter, "ID=" + String.valueOf(itemKey), "Version=2");

		this.connectionProvider = connectionProvider;
        this.itemKey = itemKey;
	}

    @Override
    protected List<Item> getItems(ISimpleTask<Void, Void, List<Item>> task, HttpURLConnection connection) throws Exception {
        final Integer serverRevision = RevisionChecker.getRevision(connectionProvider);
        final Integer boxedItemKey = itemKey;
        ItemHolder itemHolder = itemsCache.get(boxedItemKey);
        if (itemHolder != null && itemHolder.revision.equals(serverRevision))
            return itemHolder.items;

        if (task.isCancelled()) return new ArrayList<>();

        final InputStream is = connection.getInputStream();
        try {
            final List<Item> items = FilesystemResponse.GetItems(is);

            itemHolder = new ItemHolder(serverRevision, items);
            itemsCache.put(boxedItemKey, itemHolder);

            return items;
        } finally {
            is.close();
        }
	}
}
