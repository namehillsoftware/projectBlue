package com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.uri;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.servers.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.shared.IoCommon;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by david on 7/24/15.
 */
public class MediaFileUriProvider extends AbstractFileUriProvider {

	public static final String mediaFileFoundEvent = MagicPropertyBuilder.buildMagicPropertyName(MediaFileUriProvider.class, "mediaFileFoundEvent");
	public static final String mediaFileFoundMediaId = MagicPropertyBuilder.buildMagicPropertyName(MediaFileUriProvider.class, "mediaFileFoundMediaId");
	public static final String mediaFileFoundFileKey = MagicPropertyBuilder.buildMagicPropertyName(MediaFileUriProvider.class, "mediaFileFoundFileKey");
	public static final String mediaFileFoundPath = MagicPropertyBuilder.buildMagicPropertyName(MediaFileUriProvider.class, "mediaFileFoundPath");

	private static final String audioIdKey = MediaStore.Audio.keyFor("audio_id");
	private static final String mediaDataQuery = MediaStore.Audio.Media.DATA + " LIKE '%' || ? || '%' ";
	private static final String[] mediaQueryProjection = { MediaStore.Audio.Media.DATA };

	private static final Logger logger = LoggerFactory.getLogger(MediaFileUriProvider.class);

	private final Context context;
	private final boolean isSilent;
	private final IConnectionProvider connectionProvider;

	public MediaFileUriProvider(Context context, IConnectionProvider connectionProvider, IFile file) {
		this (context, connectionProvider, file, false);
	}

	/**
	 *
	 * @param context the application context under which to operate
	 * @param file the file to provide a URI for
	 * @param isSilent if true, will not emit broadcast events when media files are found
	 */
	public MediaFileUriProvider(Context context, IConnectionProvider connectionProvider, IFile file, boolean isSilent) {
		super(file);

		this.context = context;
		this.isSilent = isSilent;
		this.connectionProvider = connectionProvider;
	}

	@Override
	public Uri getFileUri() throws IOException {
		final Cursor cursor = getMediaQueryCursor();
		if (cursor == null) return null;

		try {
			if (!cursor.moveToFirst()) return null;

			final String fileUriString = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
			if (fileUriString == null || fileUriString.isEmpty()) return null;

			// The file object will produce a properly escaped File URI, as opposed to what is stored in the DB
			final java.io.File file = new java.io.File(fileUriString.replaceFirst(IoCommon.FileUriScheme + "://", ""));

			if (!file.exists()) return null;

			if (!isSilent) {
				final Intent broadcastIntent = new Intent(mediaFileFoundEvent);
				broadcastIntent.putExtra(mediaFileFoundPath, file.getPath());
				try {
					broadcastIntent.putExtra(mediaFileFoundMediaId, cursor.getInt(cursor.getColumnIndexOrThrow(audioIdKey)));
				} catch (IllegalArgumentException ie) {
					logger.info("Illegal column name.", ie);
				}
				broadcastIntent.putExtra(mediaFileFoundFileKey, getFile().getKey());
				LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);
			}

			logger.info("Returning file URI from local disk.");
			return Uri.fromFile(file);
		} finally {
			cursor.close();
		}
	}

	public int getMediaId() throws IOException {
		final Cursor cursor = getMediaQueryCursor();
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

	private Cursor getMediaQueryCursor() throws IOException {
		if (context == null)
			throw new NullPointerException("The file player's context cannot be null");

		if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
			return null;

		final CachedFilePropertiesProvider filePropertiesProvider = new CachedFilePropertiesProvider(connectionProvider, getFile().getKey());

		try {
			final Map<String, String> fileProperties = filePropertiesProvider.get();

			final String originalFilename = fileProperties.get(FilePropertiesProvider.FILENAME);
			if (originalFilename == null)
				throw new IOException("The filename property was not retrieved. A connection needs to be re-established.");

			final String filename = originalFilename.substring(originalFilename.lastIndexOf('\\') + 1, originalFilename.lastIndexOf('.'));

			final StringBuilder querySb = new StringBuilder(mediaDataQuery);
			appendAnd(querySb);

			final ArrayList<String> params = new ArrayList<>(5);
			params.add(filename);

			appendPropertyFilter(querySb, params, MediaStore.Audio.Media.ARTIST, fileProperties.get(FilePropertiesProvider.ARTIST));
			appendAnd(querySb);

			appendPropertyFilter(querySb, params, MediaStore.Audio.Media.ALBUM, fileProperties.get(FilePropertiesProvider.ALBUM));
			appendAnd(querySb);

			appendPropertyFilter(querySb, params, MediaStore.Audio.Media.TITLE, fileProperties.get(FilePropertiesProvider.NAME));
			appendAnd(querySb);

			appendPropertyFilter(querySb, params, MediaStore.Audio.Media.TRACK, fileProperties.get(FilePropertiesProvider.TRACK));

			return context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaQueryProjection, querySb.toString(), params.toArray(new String[params.size()]), null);
		} catch (InterruptedException e) {
			return null;
		} catch (ExecutionException e) {
			if (e.getCause() instanceof FileNotFoundException)
				throw (FileNotFoundException)e.getCause();

			if (e.getCause() instanceof IOException)
				throw new IOException("The filename property was not retrieved. A connection needs to be re-established.", e.getCause());

			return null;
		}
	}

	private static StringBuilder appendPropertyFilter(final StringBuilder querySb, final ArrayList<String> params, final String key, final String value) {
		querySb.append(' ').append(key).append(' ');

		if (value != null) {
			querySb.append(" = ? ");
			params.add(value);
		} else {
			querySb.append(" IS NULL ");
		}

		return querySb;
	}

	private static StringBuilder appendAnd(final StringBuilder querySb) {
		return querySb.append(" AND ");
	}
}
