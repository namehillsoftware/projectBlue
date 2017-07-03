package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.system;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.messenger.promise.Promise;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by david on 6/13/16.
 */
public class MediaQueryCursorProvider implements IMediaQueryCursorProvider, CarelessOneParameterFunction<Map<String, String>, Cursor> {

	private static final String mediaDataQuery = MediaStore.Audio.Media.DATA + " LIKE '%' || ? || '%' ";
	private static final String[] mediaQueryProjection = { MediaStore.Audio.Media.DATA };

	private final Context context;
	private final CachedFilePropertiesProvider cachedFilePropertiesProvider;

	public MediaQueryCursorProvider(Context context, CachedFilePropertiesProvider cachedFilePropertiesProvider) {
		this.context = context;
		this.cachedFilePropertiesProvider = cachedFilePropertiesProvider;
	}

	@Override
	public Promise<Cursor> getMediaQueryCursor(ServiceFile serviceFile) {
		if (context == null)
			throw new IllegalArgumentException("The serviceFile player's context cannot be null");

		return
			cachedFilePropertiesProvider
				.promiseFileProperties(serviceFile.getKey())
				.next(this);
	}

	@Override
	public Cursor resultFrom(Map<String, String> fileProperties) throws Exception {
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
