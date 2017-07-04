package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.system;

import android.database.Cursor;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.messenger.promises.Promise;

/**
 * Created by david on 6/13/16.
 */
public interface IMediaQueryCursorProvider {
	Promise<Cursor> getMediaQueryCursor(ServiceFile serviceFile);
}
