package com.lasthopesoftware.bluewater.client.library.items.stored;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.client.library.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.repository.CloseableTransaction;
import com.lasthopesoftware.bluewater.repository.InsertBuilder;
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.vedsoft.fluent.FluentCallable;
import com.vedsoft.fluent.IFluentTask;
import com.vedsoft.futures.runnables.TwoParameterRunnable;
import com.vedsoft.lazyj.Lazy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by david on 7/5/15.
 */
public class StoredItemAccess {

    private static final Logger logger = LoggerFactory.getLogger(StoredItemAccess.class);

	private static final Lazy<String> storedItemInsertSql = new Lazy<>(
			() -> InsertBuilder
					.fromTable(StoredItem.tableName)
					.addColumn(StoredItem.libraryIdColumnName)
					.addColumn(StoredItem.serviceIdColumnName)
					.addColumn(StoredItem.itemTypeColumnName)
					.build());

    private final Context context;
	private final Library library;

    public StoredItemAccess(Context context, Library library) {
        this.context = context;
	    this.library = library;
    }

    public void toggleSync(IItem item, boolean enable) {
	    if (enable)
            enableItemSync(item, getListType(item));
	    else
		    disableItemSync(item, getListType(item));
    }

    public void isItemMarkedForSync(final IItem item, TwoParameterRunnable<IFluentTask<Void, Void, Boolean>, Boolean> isItemSyncedResult) {
        final FluentCallable<Boolean> isItemSyncedTask = new FluentCallable<Boolean>() {

            @Override
            protected Boolean executeInBackground() {
	            try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
		            return isItemMarkedForSync(repositoryAccessHelper, library, item, getListType(item));
	            }
            }
        };

        if (isItemSyncedResult != null)
            isItemSyncedTask.onComplete(isItemSyncedResult);

        isItemSyncedTask.execute(RepositoryAccessHelper.databaseExecutor);
    }

    private void enableItemSync(final IItem item, final StoredItem.ItemType itemType) {
        RepositoryAccessHelper.databaseExecutor.execute(() -> {
	        try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
		        if (isItemMarkedForSync(repositoryAccessHelper, library, item, itemType))
			        return;

		        try (final CloseableTransaction closeableTransaction = repositoryAccessHelper.beginTransaction()) {
			        repositoryAccessHelper
					        .mapSql(storedItemInsertSql.getObject())
					        .addParameter(StoredItem.libraryIdColumnName, library.getId())
					        .addParameter(StoredItem.serviceIdColumnName, item.getKey())
					        .addParameter(StoredItem.itemTypeColumnName, itemType)
					        .execute();

			        closeableTransaction.setTransactionSuccessful();
		        }
	        }
        });
    }

    private void disableItemSync(final IItem item, final StoredItem.ItemType itemType) {
        RepositoryAccessHelper.databaseExecutor.execute(() -> {
	        try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
		        try (CloseableTransaction closeableTransaction = repositoryAccessHelper.beginTransaction()) {
			        repositoryAccessHelper
					        .mapSql(
							        " DELETE FROM " + StoredItem.tableName +
									        " WHERE " + StoredItem.serviceIdColumnName + " = @" + StoredItem.serviceIdColumnName +
									        " AND " + StoredItem.libraryIdColumnName + " = @" + StoredItem.libraryIdColumnName +
									        " AND " + StoredItem.itemTypeColumnName + " = @" + StoredItem.itemTypeColumnName)
					        .addParameter(StoredItem.serviceIdColumnName, item.getKey())
					        .addParameter(StoredItem.libraryIdColumnName, library.getId())
					        .addParameter(StoredItem.itemTypeColumnName, itemType)
					        .execute();

			        closeableTransaction.setTransactionSuccessful();
		        }
	        }
        });
    }

    public void getStoredItems(TwoParameterRunnable<IFluentTask<Void, Void, List<StoredItem>>, List<StoredItem>> onStoredListsRetrieved) {
        final FluentCallable<List<StoredItem>> getAllStoredItemsTasks = new FluentCallable<List<StoredItem>>() {

            @Override
            protected List<StoredItem> executeInBackground() {
	            try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
		            return
			            repositoryAccessHelper
				            .mapSql("SELECT * FROM " + StoredItem.tableName + " WHERE " + StoredItem.libraryIdColumnName + " = @" + StoredItem.libraryIdColumnName)
				            .addParameter(StoredItem.libraryIdColumnName, library.getId())
				            .fetch(StoredItem.class);
	            }
            }
        };

        if (onStoredListsRetrieved != null)
            getAllStoredItemsTasks.onComplete(onStoredListsRetrieved);

        getAllStoredItemsTasks.execute(RepositoryAccessHelper.databaseExecutor);
    }

    private static boolean isItemMarkedForSync(RepositoryAccessHelper helper, Library library, IItem item, StoredItem.ItemType itemType) {
        return getStoredList(helper, library, item, itemType) != null;
    }

    private  static StoredItem getStoredList(RepositoryAccessHelper helper, Library library, IItem item, StoredItem.ItemType itemType) {
            return
                helper
                    .mapSql(
                            " SELECT * FROM " + StoredItem.tableName +
                                    " WHERE " + StoredItem.serviceIdColumnName + " = @" + StoredItem.serviceIdColumnName +
                                    " AND " + StoredItem.libraryIdColumnName + " = @" + StoredItem.libraryIdColumnName +
                                    " AND " + StoredItem.itemTypeColumnName + " = @" + StoredItem.itemTypeColumnName)
                    .addParameter(StoredItem.serviceIdColumnName, item.getKey())
                    .addParameter(StoredItem.libraryIdColumnName, library.getId())
                    .addParameter(StoredItem.itemTypeColumnName, itemType)
                    .fetchFirst(StoredItem.class);
    }

	private static StoredItem.ItemType getListType(IItem item) {
		return item instanceof Playlist ? StoredItem.ItemType.PLAYLIST : StoredItem.ItemType.ITEM;
	}
}
