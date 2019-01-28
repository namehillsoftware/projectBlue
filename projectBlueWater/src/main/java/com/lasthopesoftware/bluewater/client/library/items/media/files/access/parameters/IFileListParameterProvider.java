package com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters;

import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.playlists.Playlist;

/**
 * Created by david on 11/26/15.
 */
public interface IFileListParameterProvider {
	String[] getFileListParameters(Item item);

	String[] getFileListParameters(Playlist playlist);
}
