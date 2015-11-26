package com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.uri;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;

import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.logger.LoggerFactory;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.shared.IoCommon;
import com.lasthopesoftware.bluewater.shared.SpecialValueHelpers;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by david on 7/24/15.
 */
public class MediaFileUriProvider extends AbstractFileUriProvider {

	public static final String mediaFileFoundEvent = SpecialValueHelpers.buildMagicPropertyName(MediaFileUriProvider.class, "mediaFileFoundEvent");
	public static final String mediaFileFoundMediaId = SpecialValueHelpers.buildMagicPropertyName(MediaFileUriProvider.class, "mediaFileFoundMediaId");
	public static final String mediaFileFoundFileKey = SpecialValueHelpers.buildMagicPropertyName(MediaFileUriProvider.class, "mediaFileFoundFileKey");
	public static final String mediaFileFoundPath = SpecialValueHelpers.buildMagicPropertyName(MediaFileUriProvider.class, "mediaFileFoundPath");

	private static final String audioIdKey = MediaStore.Audio.keyFor("audio_id");
	private static final String mediaDataQuery = MediaStore.Audio.Media.DATA + " LIKE '%' || ? || '%' ";
	private static final String[] mediaQueryProjection = { MediaStore.Audio.Media.DATA };

	private static final Logger mLogger = LoggerFactory.getLogger(MediaFileUriProvider.class);

	private final Context mContext;
	private final boolean mIsSilent;

	public MediaFileUriProvider(Context context, IFile file) {
		this (context, file, false);
	}

	/**
	 *
	 * @param context the application context under which to operate
	 * @param file the file to provide a URI for
	 * @param isSilent if true, will not emit broadcast events when media files are found
	 */
	public MediaFileUriProvider(Context context, IFile file, boolean isSilent) {
		super(file);

		mContext = context;
		mIsSilent = isSilent;
	}

	@Override
	public Uri getFileUri() throws IOException {
		final Cursor cursor = getMediaQueryCursor();
		try {
			if (!cursor.moveToFirst()) return null;

			final String fileUriString = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
			if (fileUriString == null || fileUriString.isEmpty()) return null;

			// The file object will produce a properly escaped File URI, as opposed to what is stored in the DB
			final java.io.File file = new java.io.File(fileUriString.replaceFirst(IoCommon.FileUriScheme + "://", ""));

			if (!file.exists()) return null;

			if (!mIsSilent) {
				final Intent broadcastIntent = new Intent(mediaFileFoundEvent);
				broadcastIntent.putExtra(mediaFileFoundPath, file.getPath());
				try {
					broadcastIntent.putExtra(mediaFileFoundMediaId, cursor.getInt(cursor.getColumnIndexOrThrow(audioIdKey)));
				} catch (IllegalArgumentException ie) {
					mLogger.info("Illegal column name.", ie);
				}
				broadcastIntent.putExtra(mediaFileFoundFileKey, getFile().getKey());
				LocalBroadcastManager.getInstance(mContext).sendBroadcast(broadcastIntent);
			}

			mLogger.info("Returning file URI from local disk.");
			return Uri.fromFile(file);
		} finally {
			cursor.close();
		}
	}

	public int getMediaId() throws IOException {
		final Cursor cursor = getMediaQueryCursor();
		try {
			if (cursor.moveToFirst())
				return cursor.getInt(cursor.getColumnIndexOrThrow(audioIdKey));
		} catch (IllegalArgumentException ie) {
			mLogger.info("Illegal column name.", ie);
		} finally {
			cursor.close();
		}

		return -1;
	}

	private Cursor getMediaQueryCursor() throws IOException {
		if (mContext == null)
			throw new NullPointerException("The file player's context cannot be null");

		final String originalFilename = getFile().getProperty(FilePropertiesProvider.FILENAME);
		if (originalFilename == null)
			throw new IOException("The filename property was not retrieved. A connection needs to be re-established.");

		final String filename = originalFilename.substring(originalFilename.lastIndexOf('\\') + 1, originalFilename.lastIndexOf('.'));

		final StringBuilder querySb = new StringBuilder(mediaDataQuery);
		appendAnd(querySb);

		final ArrayList<String> params = new ArrayList<>(5);
		params.add(filename);

		appendPropertyFilter(querySb, params, MediaStore.Audio.Media.ARTIST, getFile().getProperty(FilePropertiesProvider.ARTIST));
		appendAnd(querySb);

		appendPropertyFilter(querySb, params, MediaStore.Audio.Media.ALBUM, getFile().getProperty(FilePropertiesProvider.ALBUM));
		appendAnd(querySb);

		appendPropertyFilter(querySb, params, MediaStore.Audio.Media.TITLE, getFile().getProperty(FilePropertiesProvider.NAME));
		appendAnd(querySb);

		appendPropertyFilter(querySb, params, MediaStore.Audio.Media.TRACK, getFile().getProperty(FilePropertiesProvider.TRACK));

		return mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaQueryProjection, querySb.toString(), params.toArray(new String[params.size()]), null);
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
