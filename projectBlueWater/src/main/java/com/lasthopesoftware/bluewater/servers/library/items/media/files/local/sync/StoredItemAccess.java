package com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.servers.library.items.repository.StoredItem;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.runnables.ITwoParameterRunnable;
import com.lasthopesoftware.threading.FluentTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
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
            protected Boolean doInBackground(Void... params) {
                final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
                try {
                    final Dao<StoredItem, Integer> storedListAccess = repositoryAccessHelper.getDataAccess(StoredItem.class);
                    return isItemMarkedForSync(storedListAccess, library, item, getListType(item));
                } catch (SQLException e) {
                    logger.error("There was an error getting the stored item table", e);
                    setException(e);
                    return null;
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
                    final Dao<StoredItem, Integer> storedListAccess = repositoryAccessHelper.getDataAccess(StoredItem.class);

                    if (isItemMarkedForSync(storedListAccess, library, item, itemType)) return;

                    final StoredItem storedItem = new StoredItem();
                    storedItem.setLibraryId(library.getId());
                    storedItem.setServiceId(item.getKey());
                    storedItem.setItemType(itemType);

                    try {
                        storedListAccess.create(storedItem);
                    } catch (SQLException e) {
                        logger.error("Error while creating new stored list", e);
                    }
                } catch (SQLException e) {
                    logger.error("Error getting access to the stored list table", e);
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
                    final Dao<StoredItem, Integer> storedListAccess = repositoryAccessHelper.getDataAccess(StoredItem.class);

                    final StoredItem storedItem = getStoredList(storedListAccess, library, item, itemType);
	                if (storedItem == null) return;

	                try {
		                storedListAccess.delete(storedItem);
                    } catch (SQLException e) {
                        logger.error("Error removing stored list", e);
                    }
                } catch (SQLException e) {
                    logger.error("Error getting access to the stored list table", e);
                } finally {
		            repositoryAccessHelper.close();
	            }
            }
        });
    }

    public void getStoredItems(ITwoParameterRunnable<FluentTask<Void, Void, List<StoredItem>>, List<StoredItem>> onStoredListsRetrieved) {
        final FluentTask<Void, Void, List<StoredItem>> getAllStoredItemsTasks = new FluentTask<Void, Void, List<StoredItem>>() {

            @Override
            protected List<StoredItem> doInBackground(Void... params) {
                final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
                try {
                    final Dao<StoredItem, Integer> storedItemAccess = repositoryAccessHelper.getDataAccess(StoredItem.class);
                    return storedItemAccess.queryForEq(StoredItem.libraryIdColumnName, library.getId());
                } catch (SQLException e) {
                    logger.error("Error accessing the stored list access", e);
                } finally {
                    repositoryAccessHelper.close();
                }

                return new ArrayList<>();
            }
        };

        if (onStoredListsRetrieved != null)
            getAllStoredItemsTasks.onComplete(onStoredListsRetrieved);

        getAllStoredItemsTasks.execute(RepositoryAccessHelper.databaseExecutor);
    }

    private static boolean isItemMarkedForSync(Dao<StoredItem, Integer> storedListAccess, Library library, IItem item, StoredItem.ItemType itemType) {
        return getStoredList(storedListAccess, library, item, itemType) != null;
    }

    private  static StoredItem getStoredList(Dao<StoredItem, Integer> storedListAccess, Library library, IItem item, StoredItem.ItemType itemType) {
        try {
            final PreparedQuery<StoredItem> storedListPreparedQuery =
                    storedListAccess
                            .queryBuilder()
                            .where()
                            .eq(StoredItem.serviceIdColumnName, item.getKey())
                            .and()
                            .eq(StoredItem.libraryIdColumnName, library.getId())
                            .and()
                            .eq(StoredItem.itemTypeColumnName, itemType)
                            .prepare();
            return storedListAccess.queryForFirst(storedListPreparedQuery);
        } catch (SQLException e) {
            logger.error("Error while checking whether stored list exists.", e);
        }

        return null;
    }

	private static StoredItem.ItemType getListType(IItem item) {
		return item instanceof Playlist ? StoredItem.ItemType.PLAYLIST : StoredItem.ItemType.ITEM;
	}
}
