package com.lasthopesoftware.bluewater.servers.library.items.media.files.stored.system;

import android.database.Cursor;
import android.provider.MediaStore;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.permissions.IPermissionArbitrator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by david on 6/13/16.
 */
public class MediaFileIdProvider {

	private static final Logger logger = LoggerFactory.getLogger(MediaFileIdProvider.class);
	private static final String audioIdKey = MediaStore.Audio.keyFor("audio_id");

	private final IMediaQueryCursorProvider mediaQueryCursorProvider;
	private final IFile file;
	private final IPermissionArbitrator externalStorageReadPermissionsArbitrator;

	public MediaFileIdProvider(IMediaQueryCursorProvider mediaQueryCursorProvider, IFile file, IPermissionArbitrator externalStorageReadPermissionsArbitrator) {
		this.mediaQueryCursorProvider = mediaQueryCursorProvider;
		this.file = file;
		this.externalStorageReadPermissionsArbitrator = externalStorageReadPermissionsArbitrator;
	}

	public int getMediaId() throws IOException {
		if (!externalStorageReadPermissionsArbitrator.isPermissionGranted())
			return -1;

		final Cursor cursor = mediaQueryCursorProvider.getMediaQueryCursor(file);
		if (cursor == null) return -1;

		try {
			if (cursor.moveToFirst())
				return cursor.getInt(cursor.getColumnIndexOrThrow(audioIdKey));
		} catch (IllegalArgumentException ie) {
			logger.info("Illegal column name.", ie);
		} finally {
			cursor.close();
		}

		return -1;
	}
}
