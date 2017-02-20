package com.lasthopesoftware.bluewater.client.library.items.media.image;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;
import android.util.DisplayMetrics;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.DiskFileCache;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.servers.selection.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.QueuedPromise;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

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

import static com.vedsoft.futures.callables.VoidFunc.runningCarelessly;

public class ImageProvider extends QueuedPromise<Bitmap> {
	
	private static final String IMAGE_FORMAT = "jpg";
	
	private static final ExecutorService imageAccessExecutor = Executors.newSingleThreadExecutor();
	
	private static final Logger logger = LoggerFactory.getLogger(ImageProvider.class);

	private static final int MAX_DISK_CACHE_SIZE = 50 * 1024 * 1024; // 100 * 1024 * 1024 for 100MB of cache
	private static final int MAX_MEMORY_CACHE_SIZE = 10;
	private static final int MAX_DAYS_IN_CACHE = 30;
	private static final String IMAGES_CACHE_NAME = "images";

	private static final LruCache<String, Byte[]> imageMemoryCache = new LruCache<>(MAX_MEMORY_CACHE_SIZE);

	public static ImageProvider getImage(final Context context, ConnectionProvider connectionProvider, final int fileKey) {
		return new ImageProvider(context, connectionProvider, fileKey);
	}

	private ImageProvider(final Context context, final ConnectionProvider connectionProvider, final int fileKey) {
		super(new ImageDiscTask(context, connectionProvider, new FillerBitmap(context), fileKey), imageAccessExecutor);

	}

	private static class ImageDiscTask implements ThreeParameterAction<IResolvedPromise<Bitmap>, IRejectedPromise, OneParameterAction<Runnable>> {
		private final Context context;
		private final ConnectionProvider connectionProvider;
		private final FillerBitmap fillerBitmap;
		private final int fileKey;
		private final ILibraryProvider libraryProvider;
		private final SelectedBrowserLibraryIdentifierProvider selectedLibraryIdentifierProvider;

		private volatile boolean isCancelled;

		ImageDiscTask(Context context, ConnectionProvider connectionProvider, FillerBitmap fillerBitmap, int fileKey) {
			this.context = context;
			this.connectionProvider = connectionProvider;
			this.fillerBitmap = fillerBitmap;
			this.fileKey = fileKey;
			this.libraryProvider = new LibraryRepository(context);
			this.selectedLibraryIdentifierProvider = new SelectedBrowserLibraryIdentifierProvider(context);
		}

		@Override
		public void runWith(IResolvedPromise<Bitmap> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
			onCancelled.runWith(() -> {
				isCancelled = true;
				resolve.withResult(fillerBitmap.getFillerBitmap());
			});

			if (isCancelled) return;

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
				resolve.withResult(fillerBitmap.getFillerBitmap());
				return;
			}

			final byte[] imageBytes = getBitmapBytesFromMemory(uniqueKey);
			if (imageBytes.length > 0) {
				resolve.withResult(getBitmapFromBytes(imageBytes));
				return;
			}

			libraryProvider
				.getLibrary(selectedLibraryIdentifierProvider.getSelectedLibraryId())
				.thenPromise(library -> {
					final IPromise<Bitmap> httpAccessPromise =
						new QueuedPromise<>(new ImageHttpTask(uniqueKey, context, library, connectionProvider, fillerBitmap, fileKey), imageAccessExecutor);

					onCancelled.runWith(httpAccessPromise::cancel);

					return httpAccessPromise;
				})
				.then(runningCarelessly(resolve::withResult))
				.error(runningCarelessly(reject::withError));
		}

		private static byte[] getBitmapBytesFromMemory(final String uniqueKey) {
			final Byte[] memoryImageBytes = imageMemoryCache.get(uniqueKey);

			if (memoryImageBytes == null) return new byte[0];

			final byte[] imageBytes = new byte[memoryImageBytes.length];
			for (int i = 0; i < memoryImageBytes.length; i++)
				imageBytes[i] = memoryImageBytes[i];

			return imageBytes;
		}

		private static Bitmap getBitmapFromBytes(final byte[] imageBytes) {
			return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
		}
	}

	private static class ImageHttpTask implements ThreeParameterAction<IResolvedPromise<Bitmap>, IRejectedPromise, OneParameterAction<Runnable>> {

		private final String uniqueKey;
		private final Context context;
		private final Library library;
		private final IConnectionProvider connectionProvider;
		private final FillerBitmap fillerBitmap;
		private final int fileKey;

		private volatile boolean isCancelled;

		ImageHttpTask(String uniqueKey, Context context, Library library, IConnectionProvider connectionProvider, FillerBitmap fillerBitmap, int fileKey) {
			this.uniqueKey = uniqueKey;
			this.context = context;
			this.library = library;
			this.connectionProvider = connectionProvider;
			this.fillerBitmap = fillerBitmap;
			this.fileKey = fileKey;
		}

		@SuppressLint("NewApi")
		@Override
		public void runWith(IResolvedPromise<Bitmap> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
			if (library == null) {
				resolve.withResult(fillerBitmap.getFillerBitmap());
				return;
			}

			onCancelled.runWith(() -> {
				isCancelled = true;
				resolve.withResult(fillerBitmap.getFillerBitmap());
			});

			final DiskFileCache imageDiskCache = new DiskFileCache(context, library, IMAGES_CACHE_NAME, MAX_DAYS_IN_CACHE, MAX_DISK_CACHE_SIZE);

			byte[] imageBytes;
			try {
				final java.io.File imageCacheFile = imageDiskCache.get(uniqueKey);
				if (imageCacheFile != null) {
					imageBytes = putBitmapIntoMemory(uniqueKey, imageCacheFile);
					if (imageBytes.length > 0) {
						resolve.withResult(getBitmapFromBytes(imageBytes));
						return;
					}
				}
			} catch (IOException e) {
				logger.error("There was an error getting the cached file", e);
			}

			try {
				final HttpURLConnection connection = connectionProvider.getConnection("File/GetImage", "File=" + String.valueOf(fileKey), "Type=Full", "Pad=1", "Format=" + IMAGE_FORMAT, "FillTransparency=ffffff");
				try {
					// Connection failed to build
					if (connection == null) {
						resolve.withResult(fillerBitmap.getFillerBitmap());
						return;
					}

					try {
						//isCancelled was called, return an empty bitmap but do not put it into the cache
						if (isCancelled) {
							return;
						}

						try (InputStream is = connection.getInputStream()) {
							imageBytes = IOUtils.toByteArray(is);
						} catch (InterruptedIOException interruptedIoException) {
							logger.warn("Copying the input stream to a byte array was interrupted", interruptedIoException);
							resolve.withResult(fillerBitmap.getFillerBitmap());
							return;
						}

						if (imageBytes.length == 0) {
							resolve.withResult(fillerBitmap.getFillerBitmap());
							return;
						}
					} catch (FileNotFoundException fe) {
						logger.warn("Image not found!");
						resolve.withResult(fillerBitmap.getFillerBitmap());
						return;
					}

					try {
						imageDiskCache.put(uniqueKey, imageBytes);
					} catch (IOException ioe) {
						logger.error("Error writing file!", ioe);
					}

					putBitmapIntoMemory(uniqueKey, imageBytes);
					resolve.withResult(getBitmapFromBytes(imageBytes));
				} catch (Exception e) {
					logger.error(e.toString(), e);
				} finally {
					if (connection != null)
						connection.disconnect();
				}
			} catch (IOException e) {
				logger.error("There was an error getting the connection for images", e);
			}
		}

		private static Bitmap getBitmapFromBytes(final byte[] imageBytes) {
			return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
		}

		private static void putBitmapIntoMemory(final String uniqueKey, final byte[] imageBytes) {
			final Byte[] memoryImageBytes = new Byte[imageBytes.length];

			for (int i = 0; i < imageBytes.length; i++)
				memoryImageBytes[i] = imageBytes[i];

			imageMemoryCache.put(uniqueKey, memoryImageBytes);
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
	}

	private static class FillerBitmap {

		private static Bitmap fillerBitmap;
		private static final Object fillerBitmapSyncObj = new Object();

		private Context context;

		private FillerBitmap(Context context) {
			this.context = context;
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

		private static Bitmap getBitmapCopy(final Bitmap src) {
			return src.copy(src.getConfig(), false);
		}
	}
}
