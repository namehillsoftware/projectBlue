package com.lasthopesoftware.bluewater.client.library.items.playlists;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.access.ProvideItems;
import com.lasthopesoftware.bluewater.client.library.views.KnownViews;
import com.lasthopesoftware.bluewater.client.library.views.access.ProvideLibraryViews;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.Collection;

public class PlaylistItemFinder implements FindPlaylistItem {

	private final ProvideLibraryViews libraryViews;
	private final ProvideItems itemProvider;

	public PlaylistItemFinder(ProvideLibraryViews libraryViews, ProvideItems itemProvider) {
		this.libraryViews = libraryViews;
		this.itemProvider = itemProvider;
	}

	@Override
	public Promise<Item> promiseItem(Playlist playlist) {
		return libraryViews.promiseLibraryViews()
			.eventually(v -> {
				final Item playlistItem = Stream.of(v).filter(i -> KnownViews.Playlists.equals(i.getValue())).single();

				return recursivelySearchForPlaylist(playlistItem, playlist);
			});
	}

	private Promise<Item> recursivelySearchForPlaylist(Item rootItem, Playlist playlist) {
		return itemProvider.promiseItems(rootItem.getKey())
			.eventually(items -> {
				if (items.isEmpty()) return Promise.empty();

				final Optional<Item> possiblePlaylistItem = Stream.of(items)
					.filter(i -> i.getPlaylistId() != null && i.getPlaylistId() == playlist.getKey())
					.findFirst();

				if (possiblePlaylistItem.isPresent())
					return new Promise<>(possiblePlaylistItem.get());

				final Promise<Collection<Item>> promiseAllChildren = Promise.whenAll(
					Stream.of(items).map(i -> recursivelySearchForPlaylist(i, playlist)).toList());

				return promiseAllChildren.then(aggregatedItems -> {
					final Optional<Item> possiblyFoundItem = Stream.of(aggregatedItems)
						.filter(item -> item != null)
						.findFirst();

					return possiblyFoundItem.isPresent()
						? possiblyFoundItem.get()
						: null;
				});
			});
	}
}
