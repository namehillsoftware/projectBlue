package com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync;

import android.content.Context;

import com.lasthopesoftware.bluewater.repository.InsertBuilder;
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.servers.library.items.repository.StoredItem;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.runnables.ITwoParameterRunnable;
import com.lasthopesoftware.threading.FluentTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by david on 7/5/15.
 */
public class StoredItemAccess {

    private static final Logger logger = LoggerFactory.getLogger(StoredItemAccess.class);

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

    public void isItemMarkedForSync(final IItem item, ITwoParameterRunnable<FluentTask<Void, Void, Boolean>, Boolean> isItemSyncedResult) {
        final FluentTask<Void, Void, Boolean> isItemSyncedTask = new FluentTask<Void, Void, Boolean>() {

            @Override
            protected Boolean executeInBackground(Void... params) {
                final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
                try {
                    return isItemMarkedForSync(repositoryAccessHelper, library, item, getListType(item));
                } finally {
                    repositoryAccessHelper.close();
                }
            }
        };

        if (isItemSyncedResult != null)
            isItemSyncedTask.onComplete(isItemSyncedResult);

        isItemSyncedTask.execute(RepositoryAccessHelper.databaseExecutor);
    }

    private void enableItemSync(final IItem item, final StoredItem.ItemType itemType) {
        RepositoryAccessHelper.databaseExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
                try {
                    if (isItemMarkedForSync(repositoryAccessHelper, library, item, itemType)) return;

                    final String storedItemInsertSql =
                        InsertBuilder
                            .fromTable(StoredItem.tableName)
                            .addColumn(StoredItem.libraryIdColumnName)
                            .addColumn(StoredItem.serviceIdColumnName)
		                    .addColumn(StoredItem.itemTypeColumnName)
		                    .build();

                    repositoryAccessHelper
                            .mapSql(storedItemInsertSql)
		                    .addParameter(StoredItem.libraryIdColumnName, library.getId())
		                    .addParameter(StoredItem.serviceIdColumnName, item.getKey())
		                    .addParameter(StoredItem.itemTypeColumnName, itemType)
		                    .execute();
                } finally {
	                repositoryAccessHelper.close();
                }
            }
        });
    }

    private void disableItemSync(final IItem item, final StoredItem.ItemType itemType) {
        RepositoryAccessHelper.databaseExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
	            try {
		            repositoryAccessHelper
				            .mapSql(
					            "DELETE FROM " + StoredItem.tableName +
					            "WHERE " + StoredItem.serviceIdColumnName + " = @" + StoredItem.serviceIdColumnName +
					            "AND " + StoredItem.libraryIdColumnName + " = @" + StoredItem.libraryIdColumnName +
					            "AND " + StoredItem.itemTypeColumnName + " = @" + StoredItem.itemTypeColumnName)
				            .addParameter(StoredItem.serviceIdColumnName, item.getKey())
				            .addParameter(StoredItem.libraryIdColumnName, library.getId())
				            .addParameter(StoredItem.itemTypeColumnName, itemType)
				            .execute();
	            } finally {
		            repositoryAccessHelper.close();
	            }
            }
        });
    }

    public void getStoredItems(ITwoParameterRunnable<FluentTask<Void, Void, List<StoredItem>>, List<StoredItem>> onStoredListsRetrieved) {
        final FluentTask<Void, Void, List<StoredItem>> getAllStoredItemsTasks = new FluentTask<Void, Void, List<StoredItem>>() {

            @Override
            protected List<StoredItem> executeInBackground(Void... params) {
                final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
                try {
	                return
			                repositoryAccessHelper
					                .mapSql("SELECT * FROM " + StoredItem.tableName + " WHERE " + StoredItem.libraryIdColumnName + " = @" + StoredItem.libraryIdColumnName)
					                .addParameter(StoredItem.libraryIdColumnName, library.getId())
					                .fetch(StoredItem.class);

                } finally {
                    repositoryAccessHelper.close();
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
