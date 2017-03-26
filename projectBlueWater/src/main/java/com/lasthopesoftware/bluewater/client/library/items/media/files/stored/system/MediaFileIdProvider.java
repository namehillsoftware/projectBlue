package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.system;

import android.database.Cursor;
import android.provider.MediaStore;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.Promise;
import com.lasthopesoftware.storage.read.permissions.IStorageReadPermissionArbitratorForOs;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by david on 6/13/16.
 */
public class MediaFileIdProvider implements CarelessOneParameterFunction<Cursor, Integer> {

	private static final Logger logger = LoggerFactory.getLogger(MediaFileIdProvider.class);
	private static final String audioIdKey = MediaStore.Audio.keyFor("audio_id");

	private final IMediaQueryCursorProvider mediaQueryCursorProvider;
	private final ServiceFile serviceFile;
	private final IStorageReadPermissionArbitratorForOs externalStorageReadPermissionsArbitrator;

	public MediaFileIdProvider(IMediaQueryCursorProvider mediaQueryCursorProvider, ServiceFile serviceFile, IStorageReadPermissionArbitratorForOs externalStorageReadPermissionsArbitrator) {
		this.mediaQueryCursorProvider = mediaQueryCursorProvider;
		this.serviceFile = serviceFile;
		this.externalStorageReadPermissionsArbitrator = externalStorageReadPermissionsArbitrator;
	}

	public IPromise<Integer> getMediaId() throws IOException {
		if (!externalStorageReadPermissionsArbitrator.isReadPermissionGranted())
			return new Promise<>(-1);

		return
			mediaQueryCursorProvider
				.getMediaQueryCursor(serviceFile)
				.then(this);
	}

	@Override
	public Integer resultFrom(Cursor cursor) throws Exception {
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
