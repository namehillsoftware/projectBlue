package com.lasthopesoftware.bluewater.client.stored.library.items.conversion.specs.GivenAStoredItemThatIsNotAPlaylist;

import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.playlists.FindPlaylistItem;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem;
import com.lasthopesoftware.bluewater.client.stored.library.items.conversion.StoredPlaylistItemsConverter;
import com.lasthopesoftware.bluewater.client.stored.library.items.specs.FakeStoredItemAccess;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenConvertingTheStoredItem {

	private static StoredItem convertedItem;
	private static final FakeStoredItemAccess storedItemAccess = new FakeStoredItemAccess(
		new StoredItem(1, 17, StoredItem.ItemType.ITEM));

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final StoredItem storedItem = new StoredItem(1, 17, StoredItem.ItemType.ITEM);

		final FindPlaylistItem playlistItem = mock(FindPlaylistItem.class);
		when(playlistItem.promiseItem(any())).thenReturn(Promise.empty());
		when(playlistItem.promiseItem(argThat(p -> p.getKey() == 17)))
			.thenReturn(new Promise<>(new Item(34)));

		final StoredPlaylistItemsConverter playlistItemsConverter = new StoredPlaylistItemsConverter(
			() -> new Promise<>(new Library().setId(12)),
			playlistItem,
			storedItemAccess);
		convertedItem = new FuturePromise<>(playlistItemsConverter.promiseConvertedStoredItem(storedItem)).get();
	}

	@Test
	public void thenItemTypeIsTheCorrectItemType() {
		assertThat(convertedItem.getItemType()).isEqualTo(StoredItem.ItemType.ITEM);
	}

	@Test
	public void thenTheItemHasTheOriginalId() {
		assertThat(convertedItem.getServiceId()).isEqualTo(17);
	}

	@Test
	public void thenTheOriginalItemIsMarkedForSync() throws ExecutionException, InterruptedException {
		assertThat(new FuturePromise<>(storedItemAccess.isItemMarkedForSync(new LibraryId(12), new Item(17))).get()).isTrue();
	}
}
