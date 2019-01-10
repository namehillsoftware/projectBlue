package com.lasthopesoftware.bluewater.client.sync.library.items.files.system;

import android.database.Cursor;
import android.provider.MediaStore;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.storage.read.permissions.IStorageReadPermissionArbitratorForOs;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by david on 6/13/16.
 */
public class MediaFileIdProvider implements ImmediateResponse<Cursor, Integer> {

	private static final Logger logger = LoggerFactory.getLogger(MediaFileIdProvider.class);
	private static final String audioIdKey = MediaStore.Audio.keyFor("audio_id");

	private final IMediaQueryCursorProvider mediaQueryCursorProvider;
	private final IStorageReadPermissionArbitratorForOs externalStorageReadPermissionsArbitrator;

	public MediaFileIdProvider(IMediaQueryCursorProvider mediaQueryCursorProvider, IStorageReadPermissionArbitratorForOs externalStorageReadPermissionsArbitrator) {
		this.mediaQueryCursorProvider = mediaQueryCursorProvider;
		this.externalStorageReadPermissionsArbitrator = externalStorageReadPermissionsArbitrator;
	}

	public Promise<Integer> getMediaId(ServiceFile serviceFile) {
		if (!externalStorageReadPermissionsArbitrator.isReadPermissionGranted())
			return new Promise<>(-1);

		return
			mediaQueryCursorProvider
				.getMediaQueryCursor(serviceFile)
				.then(this);
	}

	@Override
	public Integer respond(Cursor cursor) {
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
