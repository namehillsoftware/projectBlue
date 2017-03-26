package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.system;

import android.database.Cursor;

import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.promises.IPromise;

/**
 * Created by david on 6/13/16.
 */
public interface IMediaQueryCursorProvider {
	IPromise<Cursor> getMediaQueryCursor(File file);
}
