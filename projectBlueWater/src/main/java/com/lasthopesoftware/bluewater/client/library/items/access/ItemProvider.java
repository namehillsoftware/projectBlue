package com.lasthopesoftware.bluewater.client.library.items.access;

import android.util.LruCache;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.access.RevisionChecker;
import com.lasthopesoftware.bluewater.client.library.access.views.LibraryViewsProvider;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.lasthopesoftware.providers.AbstractProvider;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

public class ItemProvider implements ProvideItems {

    private static final Logger logger = LoggerFactory.getLogger(ItemProvider.class);

    private static class ItemHolder {
        ItemHolder(Integer revision, List<Item> items) {
            this.revision = revision;
            this.items = items;
        }

        final Integer revision;
        public final List<Item> items;
    }

    private static final int maxSize = 50;
    private static final LruCache<UrlKeyHolder<Integer>, ItemHolder> itemsCache = new LruCache<>(maxSize);

    private final int itemKey;

	private final IConnectionProvider connectionProvider;

	public static Promise<List<Item>> provide(IConnectionProvider connectionProvider, int itemKey) {
		return new ItemProvider(connectionProvider, itemKey).promiseItems();
	}
	
	public ItemProvider(IConnectionProvider connectionProvider, int itemKey) {
		this.connectionProvider = connectionProvider;
        this.itemKey = itemKey;
	}

	@Override
    public Promise<List<Item>> promiseItems() {
		return
			RevisionChecker.promiseRevision(connectionProvider)
				.eventually(serverRevision -> new QueuedPromise<>((cancellationToken) -> {
					final UrlKeyHolder<Integer> boxedItemKey = new UrlKeyHolder<>(connectionProvider.getUrlProvider().getBaseUrl(), itemKey);

					final ItemHolder itemHolder;
					synchronized (itemsCache) {
						itemHolder = itemsCache.get(boxedItemKey);
					}

					if (itemHolder != null && itemHolder.revision.equals(serverRevision)) {
						return itemHolder.items;
					}

					if (cancellationToken.isCancelled()) {
						return new ArrayList<>();
					}

					final HttpURLConnection connection;
					connection = connectionProvider.getConnection(
						LibraryViewsProvider.browseLibraryParameter,
						"ID=" + String.valueOf(itemKey),
						"Version=2");

					try (InputStream is = connection.getInputStream()) {
						final List<Item> items = ItemResponse.GetItems(is);

						final ItemHolder newItemHolder = new ItemHolder(serverRevision, items);

						synchronized (itemsCache) {
							itemsCache.put(boxedItemKey, newItemHolder);
						}

						return items;
					} catch (IOException e) {
						logger.error("There was an error getting the inputstream", e);
						throw e;
					} finally {
						if (connection != null)
							connection.disconnect();
					}
				}, AbstractProvider.providerExecutor));
	}
}
