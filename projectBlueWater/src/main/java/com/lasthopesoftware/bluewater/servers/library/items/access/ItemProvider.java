package com.lasthopesoftware.bluewater.servers.library.items.access;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.access.FilesystemResponse;
import com.lasthopesoftware.bluewater.servers.library.access.RevisionChecker;
import com.lasthopesoftware.bluewater.servers.library.items.Item;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemProvider extends AbstractCollectionProvider<Item> {

    private static final String mBrowseChildren = "Browse/Children";

    private static class ItemHolder {
        public ItemHolder(Integer revision, List<Item> items) {
            this.revision = revision;
            this.items = items;
        }

        public final Integer revision;
        public final List<Item> items;
    }

    private static final int maxSize = 2000;
    private static final ConcurrentLinkedHashMap<Integer, ItemHolder> mItemsCache = new ConcurrentLinkedHashMap
                                                                                            .Builder<Integer, ItemHolder>()
                                                                                            .maximumWeightedCapacity(maxSize)
                                                                                            .build();

    private static ItemHolder mParentItemHolder;

    private final Integer mItemKey;

	public static ItemProvider provide(int itemKey) {
		return new ItemProvider(itemKey);
	}

    public ItemProvider() {
        super(mBrowseChildren);

        mItemKey = null;
    }
	
	public ItemProvider(int itemKey) {
		this(null, itemKey);
	}
	
	public ItemProvider(HttpURLConnection connection, int itemKey) {
		super(connection, mBrowseChildren, "ID=" + String.valueOf(itemKey));

        mItemKey = Integer.valueOf(itemKey);
	}

    protected List<Item> getItems(HttpURLConnection connection, String... params) throws Exception {
        final Integer serverRevision = RevisionChecker.getRevision();

        final ItemHolder itemHolder = mItemKey != null ? mItemsCache.get(mItemKey) : mParentItemHolder;
        if (itemHolder != null && itemHolder.revision.equals(serverRevision))
            return itemHolder.items;

        final InputStream is = connection.getInputStream();
        try {
            final List<Item> items = FilesystemResponse.GetItems(is);

            if (mItemKey != null) mItemsCache.put(mItemKey, new ItemHolder(serverRevision, items));
            else mParentItemHolder = new ItemHolder(serverRevision, items);

            return items;
        } finally {
            is.close();
        }
	}
}
