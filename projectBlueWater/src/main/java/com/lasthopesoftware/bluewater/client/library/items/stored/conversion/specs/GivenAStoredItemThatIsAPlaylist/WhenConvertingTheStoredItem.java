package com.lasthopesoftware.bluewater.client.library.items.stored.conversion.specs.GivenAStoredItemThatIsAPlaylist;

import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.playlists.FindPlaylistItem;
import com.lasthopesoftware.bluewater.client.library.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItem;
import com.lasthopesoftware.bluewater.client.library.items.stored.conversion.StoredPlaylistItemsConverter;
import com.lasthopesoftware.bluewater.client.library.items.stored.specs.FakeStoredItemAccess;
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
		new StoredItem(1, 15, StoredItem.ItemType.PLAYLIST));

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final StoredItem storedItem = new StoredItem();
		storedItem.setServiceId(15);
		storedItem.setItemType(StoredItem.ItemType.PLAYLIST);

		final FindPlaylistItem playlistItem = mock(FindPlaylistItem.class);
		when(playlistItem.promiseItem(any())).thenReturn(Promise.empty());
		when(playlistItem.promiseItem(argThat(p -> p.getKey() == 15)))
			.thenReturn(new Promise<>(new Item(34)));

		final StoredPlaylistItemsConverter playlistItemsConverter = new StoredPlaylistItemsConverter(
			playlistItem,
			storedItemAccess);
		convertedItem = new FuturePromise<>(playlistItemsConverter.promiseConvertedStoredItem(storedItem)).get();
	}

	@Test
	public void thenTheNewItemTypeIsTheCorrectItemType() {
		assertThat(convertedItem.getItemType()).isEqualTo(StoredItem.ItemType.ITEM);
	}

	@Test
	public void thenTheNewItemTypeHasTheCorrectId() {
		assertThat(convertedItem.getServiceId()).isEqualTo(34);
	}

	@Test
	public void thenTheConvertedItemIsMarkedForSync() throws ExecutionException, InterruptedException {
		assertThat(new FuturePromise<>(storedItemAccess.isItemMarkedForSync(new Item(34))).get()).isTrue();
	}

	@Test
	public void thenTheOriginalItemIsNotMarkedForSync() throws ExecutionException, InterruptedException {
		assertThat(new FuturePromise<>(storedItemAccess.isItemMarkedForSync(new Playlist(15))).get()).isFalse();
	}
}
