package com.lasthopesoftware.bluewater.servers.library.items.access;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.access.FilesystemResponse;
import com.lasthopesoftware.bluewater.servers.library.access.RevisionChecker;
import com.lasthopesoftware.bluewater.servers.library.items.Item;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemProvider extends AbstractCollectionProvider<Item, Item> {

    private static class ItemHolder {
        public ItemHolder(Integer revision, List<Item> items) {
            this.revision = revision;
            this.items = items;
        }

        public final Integer revision;
        public final List<Item> items;
    }

    private static final int maxSize = 2000;
    private static final ConcurrentLinkedHashMap<Item, ItemHolder> mItemsCache = new ConcurrentLinkedHashMap
                                                                                            .Builder<Item, ItemHolder>()
                                                                                            .maximumWeightedCapacity(maxSize)
                                                                                            .build();

	public static ItemProvider provide(Item item) {
		return new ItemProvider(item);
	}
	
	public ItemProvider(Item item) {
		super(item);
	}
	
	public ItemProvider(HttpURLConnection connection, Item item) {
		super(connection, item);
	}

    @Override
	protected SimpleTask<Void, Void, List<Item>> buildTask(final Item item) {

		final SimpleTask<Void, Void, List<Item>> getItemsTask = new SimpleTask<Void, Void, List<Item>>(new OnExecuteListener<Void, Void, List<Item>>() {
			
			@Override
			public List<Item> onExecute(ISimpleTask<Void, Void, List<Item>> owner, Void... voidParams) throws Exception {
                final Integer serverRevision = RevisionChecker.getRevision();
                final ItemHolder itemHolder = mItemsCache.get(item);
                if (itemHolder != null && itemHolder.revision.equals(serverRevision))
                    return itemHolder.items;

                if (owner.isCancelled()) return new ArrayList<>();
                final HttpURLConnection conn = mConnection == null ? ConnectionProvider.getConnection(item.getSubItemParams()) : mConnection;
				try {
					final InputStream is = conn.getInputStream();
					try {
                        final List<Item> items = FilesystemResponse.GetItems(is);
                        mItemsCache.put(item, new ItemHolder(serverRevision, items));
						return items;
					} finally {
						is.close();
					}

				} finally {
					if (mConnection == null) conn.disconnect();
				}
			}
		});

        getItemsTask.addOnErrorListener(new ISimpleTask.OnErrorListener<Void, Void, List<Item>>() {
            @Override
            public boolean onError(ISimpleTask<Void, Void, List<Item>> owner, boolean isHandled, Exception innerException) {
                setException(innerException);
                return false;
            }
        });

		if (mOnGetItemsComplete != null)
			getItemsTask.addOnCompleteListener(mOnGetItemsComplete);
		
		if (mOnGetItemsError != null)
			getItemsTask.addOnErrorListener(mOnGetItemsError);
		
		return getItemsTask;
	}
}
