package com.lasthopesoftware.bluewater.client.library.items.media.image;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;
import android.util.DisplayMetrics;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.DiskFileCache;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.LibrarySession;
import com.vedsoft.fluent.FluentCallable;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageProvider extends FluentCallable<Bitmap> {
	
	private static final String IMAGE_FORMAT = "jpg";
	
	private static final ExecutorService imageAccessExecutor = Executors.newSingleThreadExecutor();
	
	private static final Logger logger = LoggerFactory.getLogger(ImageProvider.class);

	private static final int MAX_DISK_CACHE_SIZE = 50 * 1024 * 1024; // 100 * 1024 * 1024 for 100MB of cache
	private static final int MAX_MEMORY_CACHE_SIZE = 10;
	private static final int MAX_DAYS_IN_CACHE = 30;
	private static final String IMAGES_CACHE_NAME = "images";

	private static Bitmap fillerBitmap;
	private static final Object fillerBitmapSyncObj = new Object();
	private static final LruCache<String, Byte[]> imageMemoryCache = new LruCache<>(MAX_MEMORY_CACHE_SIZE);

	private final Context context;
	private final ConnectionProvider connectionProvider;
	private final int fileKey;

	public static ImageProvider getImage(final Context context, ConnectionProvider connectionProvider, final int fileKey) {
		return new ImageProvider(context, connectionProvider, fileKey);
	}

	private ImageProvider(final Context context, final ConnectionProvider connectionProvider, final int fileKey) {
		super(imageAccessExecutor);

		this.context = context;
		this.connectionProvider = connectionProvider;
		this.fileKey = fileKey;
	}

	@Override
	@SuppressLint("NewApi")
	protected Bitmap executeInBackground() {
		if (isCancelled()) return getFillerBitmap();

		String uniqueKey;
		try {
			final CachedFilePropertiesProvider filePropertiesProvider = new CachedFilePropertiesProvider(connectionProvider, fileKey);
			final Map<String, String> fileProperties = filePropertiesProvider.get();
			// First try storing by the album artist, which can cover the artist for the entire album (i.e. an album with various
			// artists), and then by artist if that field is empty
			String artist = fileProperties.get(FilePropertiesProvider.ALBUM_ARTIST);
			if (artist == null || artist.isEmpty())
				artist = fileProperties.get(FilePropertiesProvider.ARTIST);

			String albumOrTrackName = fileProperties.get(FilePropertiesProvider.ALBUM);
			if (albumOrTrackName == null)
				albumOrTrackName = fileProperties.get(FilePropertiesProvider.NAME);

			uniqueKey = artist + ":" + albumOrTrackName;
		} catch (InterruptedException | ExecutionException e) {
			logger.error("Error getting file properties.", e);
			return getFillerBitmap();
		}

		byte[] imageBytes = getBitmapBytesFromMemory(uniqueKey);
		if (imageBytes.length > 0) return getBitmapFromBytes(imageBytes);

        final Library library = LibrarySession.GetActiveLibraryInternal(context);
		if (library == null) return getFillerBitmap();


        final DiskFileCache imageDiskCache = new DiskFileCache(context, library, IMAGES_CACHE_NAME, MAX_DAYS_IN_CACHE, MAX_DISK_CACHE_SIZE);

		try {
			final java.io.File imageCacheFile = imageDiskCache.get(uniqueKey);
			if (imageCacheFile != null) {
				imageBytes = putBitmapIntoMemory(uniqueKey, imageCacheFile);
				if (imageBytes.length > 0)
					return getBitmapFromBytes(imageBytes);
			}
		} catch (IOException e) {
			logger.error("There was an error getting the cached file", e);
		}

		try {
			final HttpURLConnection connection = connectionProvider.getConnection("File/GetImage", "File=" + String.valueOf(fileKey), "Type=Full", "Pad=1", "Format=" + IMAGE_FORMAT, "FillTransparency=ffffff");
			try {
				// Connection failed to build
				if (connection == null) return getFillerBitmap();

				try {
					//isCancelled was called, return an empty bitmap but do not put it into the cache
					if (isCancelled()) return getFillerBitmap();

					try (InputStream is = connection.getInputStream()) {
						imageBytes = IOUtils.toByteArray(is);
					} catch (InterruptedIOException interruptedIoException) {
						logger.warn("Copying the input stream to a byte array was interrupted", interruptedIoException);
						return getFillerBitmap();
					}

					if (imageBytes.length == 0)
						return getFillerBitmap();
				} catch (FileNotFoundException fe) {
					logger.warn("Image not found!");
					return getFillerBitmap();
				}

				try {
					imageDiskCache.put(uniqueKey, imageBytes);
				} catch (IOException ioe) {
					logger.error("Error writing file!", ioe);
				}

				putBitmapIntoMemory(uniqueKey, imageBytes);
				return getBitmapFromBytes(imageBytes);
			} catch (Exception e) {
				logger.error(e.toString(), e);
			} finally {
				if (connection != null)
					connection.disconnect();
			}
		} catch (IOException e) {
			logger.error("There was an error getting the connection for images", e);
		}

		return null;
	}

	private static byte[] getBitmapBytesFromMemory(final String uniqueKey) {
		final Byte[] memoryImageBytes = imageMemoryCache.get(uniqueKey);

		if (memoryImageBytes == null) return new byte[0];

		final byte[] imageBytes = new byte[memoryImageBytes.length];
		for (int i = 0; i < memoryImageBytes.length; i++)
			imageBytes[i] = memoryImageBytes[i];

		return imageBytes;
	}

	private static byte[] putBitmapIntoMemory(final String uniqueKey, final java.io.File file) {
		final int size = (int) file.length();
	    final byte[] bytes = new byte[size];

	    try {
	        final FileInputStream fis = new FileInputStream(file);
	        final BufferedInputStream buffer = new BufferedInputStream(fis);
	        try {
	            buffer.read(bytes, 0, bytes.length);
	        } finally {
	            buffer.close();
	            fis.close();
	        }
	    } catch (FileNotFoundException e) {
	        logger.error("Could not find file.", e);
	        return new byte[0];
	    } catch (IOException e) {
	        logger.error("Error reading file.", e);
	        return new byte[0];
	    }

	    putBitmapIntoMemory(uniqueKey, bytes);
	    return bytes;
	}

	private static void putBitmapIntoMemory(final String uniqueKey, final byte[] imageBytes) {
		final Byte[] memoryImageBytes = new Byte[imageBytes.length];

		for (int i = 0; i < imageBytes.length; i++)
			memoryImageBytes[i] = imageBytes[i];

		imageMemoryCache.put(uniqueKey, memoryImageBytes);
	}

	private static Bitmap getBitmapFromBytes(final byte[] imageBytes) {
		return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
	}

	private static Bitmap getBitmapCopy(final Bitmap src) {
		return src.copy(src.getConfig(), false);
	}

	private Bitmap getFillerBitmap() {
		synchronized (fillerBitmapSyncObj) {
			if (fillerBitmap != null) return getBitmapCopy(fillerBitmap);

			fillerBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.wave_background);

			final DisplayMetrics dm = context.getResources().getDisplayMetrics();
			int maxSize = Math.max(dm.heightPixels, dm.widthPixels);

			fillerBitmap = Bitmap.createScaledBitmap(fillerBitmap, maxSize, maxSize, false);

			return getBitmapCopy(fillerBitmap);
		}
	}
}
