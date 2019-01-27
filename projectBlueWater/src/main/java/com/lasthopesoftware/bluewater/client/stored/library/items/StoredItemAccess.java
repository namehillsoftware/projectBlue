package com.lasthopesoftware.bluewater.client.stored.library.items;

import android.content.Context;
import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
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
	private final Library library;

	public StoredItemAccess(Context context, Library library) {
		this.context = context;
		this.library = library;
	}

	private static boolean isItemMarkedForSync(RepositoryAccessHelper helper, Library library, IItem item, StoredItem.ItemType itemType) {
		return getStoredItem(helper, library, item, itemType) != null;
	}

	private static StoredItem getStoredItem(RepositoryAccessHelper helper, Library library, IItem item, StoredItem.ItemType itemType) {
		return helper.mapSql(
			" SELECT * FROM " + StoredItem.tableName +
				" WHERE " + StoredItem.serviceIdColumnName + " = @" + StoredItem.serviceIdColumnName +
				" AND " + StoredItem.libraryIdColumnName + " = @" + StoredItem.libraryIdColumnName +
				" AND " + StoredItem.itemTypeColumnName + " = @" + StoredItem.itemTypeColumnName)
			.addParameter(StoredItem.serviceIdColumnName, item.getKey())
			.addParameter(StoredItem.libraryIdColumnName, library.getId())
			.addParameter(StoredItem.itemTypeColumnName, itemType)
			.fetchFirst(StoredItem.class);
	}

	@Override
	public void toggleSync(IItem item, boolean enable) {
		final IItem inferredItemType = inferItemType(item);
		if (enable)
			enableItemSync(inferredItemType, StoredItemHelpers.getListType(inferredItemType));
		else
			disableItemSync(inferredItemType, StoredItemHelpers.getListType(inferredItemType));
	}

	@Override
	public Promise<Boolean> isItemMarkedForSync(final IItem item) {
		return new QueuedPromise<>(() -> {
			try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				final IItem inferredItemType = inferItemType(item);
				return isItemMarkedForSync(repositoryAccessHelper, library, inferredItemType, StoredItemHelpers.getListType(inferredItemType));
			}
		}, RepositoryAccessHelper.databaseExecutor);
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

	@Override
	public Promise<Collection<StoredItem>> promiseStoredItems() {
		return new QueuedPromise<>(() -> {
			try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				return
					repositoryAccessHelper
						.mapSql("SELECT * FROM " + StoredItem.tableName + " WHERE " + StoredItem.libraryIdColumnName + " = @" + StoredItem.libraryIdColumnName)
						.addParameter(StoredItem.libraryIdColumnName, library.getId())
						.fetch(StoredItem.class);
			}
		}, RepositoryAccessHelper.databaseExecutor);
	}

	private IItem inferItemType(IItem item) {
		if (item instanceof Item) {
			final Item castItem = (Item)item;
			final Playlist playlist = castItem.getPlaylist();
			if (playlist != null) return playlist;
		}

		return item;
	}
}
