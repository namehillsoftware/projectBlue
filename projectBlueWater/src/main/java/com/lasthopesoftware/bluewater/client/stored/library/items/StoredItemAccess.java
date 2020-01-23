package com.lasthopesoftware.bluewater.client.stored.library.items;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.browsing.items.IItem;
import com.lasthopesoftware.bluewater.client.browsing.items.Item;
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.repository.CloseableTransaction;
import com.lasthopesoftware.bluewater.repository.InsertBuilder;
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;
import com.namehillsoftware.lazyj.Lazy;

import java.util.Collection;

public final class StoredItemAccess implements IStoredItemAccess {

	private static final Lazy<String> storedItemInsertSql = new Lazy<>(
		() -> InsertBuilder
			.fromTable(StoredItem.tableName)
			.addColumn(StoredItem.libraryIdColumnName)
			.addColumn(StoredItem.serviceIdColumnName)
			.addColumn(StoredItem.itemTypeColumnName)
			.build());

	private final Context context;

	public StoredItemAccess(Context context) {
		this.context = context;
	}

	private static boolean isItemMarkedForSync(RepositoryAccessHelper helper, LibraryId libraryId, IItem item, StoredItem.ItemType itemType) {
		return getStoredItem(helper, libraryId, item, itemType) != null;
	}

	private static StoredItem getStoredItem(RepositoryAccessHelper helper, LibraryId libraryId, IItem item, StoredItem.ItemType itemType) {
		return helper.mapSql(
			" SELECT * FROM " + StoredItem.tableName +
				" WHERE " + StoredItem.serviceIdColumnName + " = @" + StoredItem.serviceIdColumnName +
				" AND " + StoredItem.libraryIdColumnName + " = @" + StoredItem.libraryIdColumnName +
				" AND " + StoredItem.itemTypeColumnName + " = @" + StoredItem.itemTypeColumnName)
			.addParameter(StoredItem.serviceIdColumnName, item.getKey())
			.addParameter(StoredItem.libraryIdColumnName, libraryId.getId())
			.addParameter(StoredItem.itemTypeColumnName, itemType)
			.fetchFirst(StoredItem.class);
	}

	@Override
	public void toggleSync(LibraryId libraryId, IItem item, boolean enable) {
		item = inferItem(item);
		if (enable)
			enableItemSync(libraryId, item, StoredItemHelpers.getListType(item));
		else
			disableItemSync(libraryId, item, StoredItemHelpers.getListType(item));
	}

	@Override
	public Promise<Boolean> isItemMarkedForSync(LibraryId libraryId, final IItem item) {
		return new QueuedPromise<>(() -> {
			try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				final IItem inferredItem = inferItem(item);
				return isItemMarkedForSync(repositoryAccessHelper, libraryId, inferredItem, StoredItemHelpers.getListType(inferredItem));
			}
		}, RepositoryAccessHelper.databaseExecutor());
	}

	private void enableItemSync(final LibraryId libraryId, final IItem item, final StoredItem.ItemType itemType) {
		RepositoryAccessHelper.databaseExecutor().execute(() -> {
			try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				if (isItemMarkedForSync(repositoryAccessHelper, libraryId, item, itemType))
					return;

				try (final CloseableTransaction closeableTransaction = repositoryAccessHelper.beginTransaction()) {
					repositoryAccessHelper
						.mapSql(storedItemInsertSql.getObject())
						.addParameter(StoredItem.libraryIdColumnName, libraryId.getId())
						.addParameter(StoredItem.serviceIdColumnName, item.getKey())
						.addParameter(StoredItem.itemTypeColumnName, itemType)
						.execute();

					closeableTransaction.setTransactionSuccessful();
				}
			}
		});
	}

	private void disableItemSync(LibraryId libraryId, final IItem item, final StoredItem.ItemType itemType) {
		RepositoryAccessHelper.databaseExecutor().execute(() -> {
			try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				try (CloseableTransaction closeableTransaction = repositoryAccessHelper.beginTransaction()) {
					repositoryAccessHelper
						.mapSql(
							" DELETE FROM " + StoredItem.tableName +
								" WHERE " + StoredItem.serviceIdColumnName + " = @" + StoredItem.serviceIdColumnName +
								" AND " + StoredItem.libraryIdColumnName + " = @" + StoredItem.libraryIdColumnName +
								" AND " + StoredItem.itemTypeColumnName + " = @" + StoredItem.itemTypeColumnName)
						.addParameter(StoredItem.serviceIdColumnName, item.getKey())
						.addParameter(StoredItem.libraryIdColumnName, libraryId.getId())
						.addParameter(StoredItem.itemTypeColumnName, itemType)
						.execute();

					closeableTransaction.setTransactionSuccessful();
				}
			}
		});
	}

	@Override
	public Promise<Collection<StoredItem>> promiseStoredItems(LibraryId libraryId) {
		return new QueuedPromise<>(() -> {
			try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				return
					repositoryAccessHelper
						.mapSql("SELECT * FROM " + StoredItem.tableName + " WHERE " + StoredItem.libraryIdColumnName + " = @" + StoredItem.libraryIdColumnName)
						.addParameter(StoredItem.libraryIdColumnName, libraryId.getId())
						.fetch(StoredItem.class);
			}
		}, RepositoryAccessHelper.databaseExecutor());
	}

	private static IItem inferItem(IItem item) {
		if (item instanceof Item) {
			final Playlist playlist = ((Item)item).getPlaylist();
			if (playlist != null) return playlist;
		}

		return item;
	}
}
