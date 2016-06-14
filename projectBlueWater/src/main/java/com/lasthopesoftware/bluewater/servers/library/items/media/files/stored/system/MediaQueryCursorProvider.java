package com.lasthopesoftware.bluewater.servers.library.items.media.files.stored.system;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import com.lasthopesoftware.bluewater.servers.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FilePropertiesProvider;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by david on 6/13/16.
 */
public class MediaQueryCursorProvider implements IMediaQueryCursorProvider {

	private static final String mediaDataQuery = MediaStore.Audio.Media.DATA + " LIKE '%' || ? || '%' ";
	private static final String[] mediaQueryProjection = { MediaStore.Audio.Media.DATA };

	private final Context context;
	private final IConnectionProvider connectionProvider;

	public MediaQueryCursorProvider(Context context, IConnectionProvider connectionProvider) {
		this.context = context;
		this.connectionProvider = connectionProvider;
	}

	@Override
	public Cursor getMediaQueryCursor(IFile file) throws IOException {
		if (context == null)
			throw new NullPointerException("The file player's context cannot be null");

		final CachedFilePropertiesProvider filePropertiesProvider = new CachedFilePropertiesProvider(connectionProvider, file.getKey());

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
