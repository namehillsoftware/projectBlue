package com.lasthopesoftware.bluewater.servers.library.items.media.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;
import android.util.DisplayMetrics;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.cached.DiskFileCache;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.bluewater.servers.library.repository.LibrarySession;
import com.vedsoft.fluent.FluentTask;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageProvider extends FluentTask<Void, Void, Bitmap> {
	
	private static final String IMAGE_FORMAT = "jpg";
	
	private static final ExecutorService imageAccessExecutor = Executors.newSingleThreadExecutor();
	
	private static final Logger logger = LoggerFactory.getLogger(ImageProvider.class);

	private static final int MAX_DISK_CACHE_SIZE = 35 * 1024 * 1024; // 100 * 1024 * 1024 for 100MB of cache
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
	protected Bitmap executeInBackground(Void[] params) {
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

			uniqueKey = artist + ":" + fileProperties.get(FilePropertiesProvider.ALBUM);
		} catch (InterruptedException | ExecutionException e) {
			logger.error("Error getting file properties.", e);
			return getFillerBitmap();
		}

		byte[] imageBytes = getBitmapBytesFromMemory(uniqueKey);
		if (imageBytes.length > 0) return getBitmapFromBytes(imageBytes);

        final Library library = LibrarySession.GetActiveLibrary(context);
		if (library == null) return getFillerBitmap();


        final DiskFileCache imageDiskCache = new DiskFileCache(context, library, IMAGES_CACHE_NAME, MAX_DAYS_IN_CACHE, MAX_DISK_CACHE_SIZE);

		final java.io.File imageCacheFile = imageDiskCache.get(uniqueKey);
		if (imageCacheFile != null) {
			imageBytes = putBitmapIntoMemory(uniqueKey, imageCacheFile);
			if (imageBytes.length > 0)
				return getBitmapFromBytes(imageBytes);
		}

		try {
			final HttpURLConnection connection = connectionProvider.getConnection("File/GetImage", "File=" + String.valueOf(fileKey), "Type=Full", "Pad=1", "Format=" + IMAGE_FORMAT, "FillTransparency=ffffff");
			try {
				// Connection failed to build
				if (connection == null) return getFillerBitmap();

				try {
					//isCancelled was called, return an empty bitmap but do not put it into the cache
					if (isCancelled()) return getFillerBitmap();

					final InputStream is = connection.getInputStream();
					try {
						imageBytes = IOUtils.toByteArray(is);
					} finally {
						is.close();
					}

					if (imageBytes.length == 0)
						return getFillerBitmap();
				} catch (FileNotFoundException fe) {
					logger.warn("Image not found!");
					return getFillerBitmap();
				}

				try {
					final java.io.File cacheDir = DiskFileCache.getDiskCacheDir(context, IMAGES_CACHE_NAME);
					if (!cacheDir.exists() && !cacheDir.mkdirs()) return getFillerBitmap();

					final java.io.File file = java.io.File.createTempFile(String.valueOf(library.getId()) + "-" + IMAGES_CACHE_NAME, "." + IMAGE_FORMAT, cacheDir);
					imageDiskCache.put(uniqueKey, file, imageBytes);
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
