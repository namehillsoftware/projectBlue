package com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.IMediaQueryCursorProvider;
import com.lasthopesoftware.bluewater.shared.IoCommon;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.storage.read.permissions.IStorageReadPermissionArbitratorForOs;
import com.namehillsoftware.handoff.promises.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaFileUriProvider implements IFileUriProvider {

	public static final String mediaFileFoundEvent = MagicPropertyBuilder.buildMagicPropertyName(MediaFileUriProvider.class, "mediaFileFoundEvent");
	public static final String mediaFileFoundMediaId = MagicPropertyBuilder.buildMagicPropertyName(MediaFileUriProvider.class, "mediaFileFoundMediaId");
	public static final String mediaFileFoundFileKey = MagicPropertyBuilder.buildMagicPropertyName(MediaFileUriProvider.class, "mediaFileFoundFileKey");
	public static final String mediaFileFoundPath = MagicPropertyBuilder.buildMagicPropertyName(MediaFileUriProvider.class, "mediaFileFoundPath");
	public static final String mediaFileFoundLibraryId = MagicPropertyBuilder.buildMagicPropertyName(MediaFileUriProvider.class, "mediaFileFoundLibraryId");

	private static final String audioIdKey = MediaStore.Audio.keyFor("audio_id");

	private static final Logger logger = LoggerFactory.getLogger(MediaFileUriProvider.class);

	private final Context context;
	private final IMediaQueryCursorProvider mediaQueryCursorProvider;
	private final IStorageReadPermissionArbitratorForOs externalStorageReadPermissionsArbitrator;
	private final Library library;
	private final boolean isSilent;

	/**
	 *
	 * @param context the application context under which to operate
	 * @param isSilent if true, will not emit broadcast events when media files are found
	 */
	public MediaFileUriProvider(Context context, IMediaQueryCursorProvider mediaQueryCursorProvider, IStorageReadPermissionArbitratorForOs externalStorageReadPermissionsArbitrator, Library library, boolean isSilent) {
		this.context = context;
		this.mediaQueryCursorProvider = mediaQueryCursorProvider;
		this.externalStorageReadPermissionsArbitrator = externalStorageReadPermissionsArbitrator;
		this.library = library;
		this.isSilent = isSilent;
	}

	@Override
	public Promise<Uri> promiseFileUri(ServiceFile serviceFile) {
		if (!externalStorageReadPermissionsArbitrator.isReadPermissionGranted())
			return Promise.empty();

		return
			mediaQueryCursorProvider
				.getMediaQueryCursor(serviceFile)
				.then(cursor -> {
					if (cursor == null) return null;

					try {
						if (!cursor.moveToFirst()) return null;

						final String fileUriString = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
						if (fileUriString == null || fileUriString.isEmpty()) return null;

						// The serviceFile object will produce a properly escaped ServiceFile URI, as opposed to what is stored in the DB
						final java.io.File systemFile = new java.io.File(fileUriString.replaceFirst(IoCommon.FileUriScheme + "://", ""));

						if (!systemFile.exists()) return null;

						if (!isSilent) {
							final Intent broadcastIntent = new Intent(mediaFileFoundEvent);
							broadcastIntent.putExtra(mediaFileFoundPath, systemFile.getPath());
							try {
								broadcastIntent.putExtra(mediaFileFoundMediaId, cursor.getInt(cursor.getColumnIndexOrThrow(audioIdKey)));
							} catch (IllegalArgumentException ie) {
								logger.info("Illegal column name.", ie);
							}
							broadcastIntent.putExtra(mediaFileFoundFileKey, serviceFile.getKey());
							broadcastIntent.putExtra(mediaFileFoundLibraryId, library.getId());
							LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);
						}

						logger.info("Returning serviceFile URI from local disk.");
						return Uri.fromFile(systemFile);
					} finally {
						cursor.close();
					}
				});
	}
}
