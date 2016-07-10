package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.system;

import android.database.Cursor;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;

import java.io.IOException;

/**
 * Created by david on 6/13/16.
 */
public interface IMediaQueryCursorProvider {
	Cursor getMediaQueryCursor(IFile file) throws IOException;
}
