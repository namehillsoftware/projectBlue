package com.lasthopesoftware.bluewater.client.library.items.access;

import android.util.LruCache;

import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.access.LibraryViewsProvider;
import com.lasthopesoftware.bluewater.client.library.access.RevisionChecker;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.lasthopesoftware.providers.AbstractConnectionProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

public class ItemProvider extends AbstractConnectionProvider<List<Item>> {

    private static final Logger logger = LoggerFactory.getLogger(ItemProvider.class);

    private static class ItemHolder {
        public ItemHolder(Integer revision, List<Item> items) {
            this.revision = revision;
            this.items = items;
        }

        public final Integer revision;
        public final List<Item> items;
    }

    private static final int maxSize = 50;
    private static final LruCache<UrlKeyHolder<Integer>, ItemHolder> itemsCache = new LruCache<>(maxSize);

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
    protected List<Item> getData(HttpURLConnection connection) {
        final Integer serverRevision = RevisionChecker.getRevision(connectionProvider);
        final UrlKeyHolder<Integer> boxedItemKey = new UrlKeyHolder<>(connectionProvider.getUrlProvider().getBaseUrl(), itemKey);

        ItemHolder itemHolder;
        synchronized (itemsCache) {
            itemHolder = itemsCache.get(boxedItemKey);
        }

        if (itemHolder != null && itemHolder.revision.equals(serverRevision))
            return itemHolder.items;

        if (isCancelled()) return new ArrayList<>();

        try {
            final InputStream is = connection.getInputStream();
            try {
                final List<Item> items = ItemResponse.GetItems(connectionProvider, is);

                itemHolder = new ItemHolder(serverRevision, items);

                synchronized (itemsCache) {
                    itemsCache.put(boxedItemKey, itemHolder);
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
