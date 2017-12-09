package com.lasthopesoftware.bluewater.client.library.items.media.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.DiskFileCache;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.access.CachedFilesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.disk.AndroidDiskCacheDirectoryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.disk.IDiskCacheDirectoryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.persistence.DiskFileAccessTimeUpdater;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.persistence.DiskFileCachePersistence;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.stream.supplier.DiskFileCacheStreamSupplier;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.servers.selection.SelectedBrowserLibraryIdentifierProvider;
import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.MessengerOperator;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy;
import com.namehillsoftware.handoff.promises.propagation.PromiseProxy;
import com.namehillsoftware.handoff.promises.queued.MessageWriter;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter;
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageProvider {
	
	private static final String IMAGE_FORMAT = "jpg";
	
	private static final ExecutorService imageAccessExecutor = Executors.newSingleThreadExecutor();
	
	private static final Logger logger = LoggerFactory.getLogger(ImageProvider.class);

	private static final int MAX_MEMORY_CACHE_SIZE = 10;

	private static final LruCache<String, Byte[]> imageMemoryCache = new LruCache<>(MAX_MEMORY_CACHE_SIZE);

	private static final String cancellationMessage = "The image task was cancelled";

	private final Context context;
	private final IConnectionProvider connectionProvider;
	private final IDiskCacheDirectoryProvider diskCacheDirectoryProvider;
	private final CachedFilePropertiesProvider cachedFilePropertiesProvider;

	public ImageProvider(final Context context, IConnectionProvider connectionProvider, IDiskCacheDirectoryProvider diskCacheDirectoryProvider, CachedFilePropertiesProvider cachedFilePropertiesProvider) {
		this.context = context;
		this.connectionProvider = connectionProvider;
		this.diskCacheDirectoryProvider = diskCacheDirectoryProvider;
		this.cachedFilePropertiesProvider = cachedFilePropertiesProvider;
	}

	public Promise<Bitmap> promiseFileBitmap(ServiceFile serviceFile) {
		return new Promise<>(new ImageOperator(context, connectionProvider, diskCacheDirectoryProvider, cachedFilePropertiesProvider, serviceFile));
	}

	private static class ImageOperator implements MessengerOperator<Bitmap> {

		private final Context context;
		private final IConnectionProvider connectionProvider;
		private final IDiskCacheDirectoryProvider diskCacheDirectoryProvider;
		private final CachedFilePropertiesProvider cachedFilePropertiesProvider;
		private final ServiceFile serviceFile;

		ImageOperator(Context context, IConnectionProvider connectionProvider, IDiskCacheDirectoryProvider diskCacheDirectoryProvider, CachedFilePropertiesProvider cachedFilePropertiesProvider, ServiceFile serviceFile) {
			this.context = context;
			this.connectionProvider = connectionProvider;
			this.diskCacheDirectoryProvider = diskCacheDirectoryProvider;
			this.cachedFilePropertiesProvider = cachedFilePropertiesProvider;
			this.serviceFile = serviceFile;
		}

		@Override
		public void send(Messenger<Bitmap> messenger) {
			final Promise<Map<String, String>> promisedFileProperties = cachedFilePropertiesProvider.promiseFileProperties(serviceFile.getKey());

			final CancellationProxy cancellationProxy = new CancellationProxy();
			messenger.cancellationRequested(cancellationProxy);
			cancellationProxy.doCancel(promisedFileProperties);

			final PromiseProxy<Bitmap> promiseProxy = new PromiseProxy<>(messenger);
			final Promise<Bitmap> promisedBitmap =
				promisedFileProperties
					.then(fileProperties -> {
						// First try storing by the album artist, which can cover the artist for the entire album (i.e. an album with various
						// artists), and then by artist if that field is empty
						String artist = fileProperties.get(FilePropertiesProvider.ALBUM_ARTIST);
						if (artist == null || artist.isEmpty())
							artist = fileProperties.get(FilePropertiesProvider.ARTIST);

						String albumOrTrackName = fileProperties.get(FilePropertiesProvider.ALBUM);
						if (albumOrTrackName == null)
							albumOrTrackName = fileProperties.get(FilePropertiesProvider.NAME);

						return artist + ":" + albumOrTrackName;
					})
					.eventually(uniqueKey -> {
						final Promise<Bitmap> memoryTask = new QueuedPromise<>(new ImageMemoryWriter(uniqueKey), imageAccessExecutor);

						return memoryTask.eventually(bitmap -> {
							if (bitmap != null) return new Promise<>(bitmap);

							final LibraryRepository libraryProvider = new LibraryRepository(context);
							final SelectedBrowserLibraryIdentifierProvider selectedLibraryIdentifierProvider = new SelectedBrowserLibraryIdentifierProvider(context);

							return
								libraryProvider
									.getLibrary(selectedLibraryIdentifierProvider.getSelectedLibraryId())
									.eventually(library -> {
										final ImageCacheConfiguration imageCacheConfiguration = new ImageCacheConfiguration(library);
										final CachedFilesProvider cachedFilesProvider = new CachedFilesProvider(context, imageCacheConfiguration);
										final DiskFileAccessTimeUpdater diskFileAccessTimeUpdater = new DiskFileAccessTimeUpdater(context);
										final DiskFileCache imageDiskCache =
											new DiskFileCache(
												context,
												diskCacheDirectoryProvider,
												imageCacheConfiguration,
												new DiskFileCacheStreamSupplier(
													diskCacheDirectoryProvider,
													imageCacheConfiguration,
													new DiskFileCachePersistence(
														context,
														diskCacheDirectoryProvider,
														imageCacheConfiguration,
														cachedFilesProvider,
														diskFileAccessTimeUpdater),
													cachedFilesProvider
												),
												cachedFilesProvider,
												diskFileAccessTimeUpdater);

										final Promise<File> cachedFilePromise = imageDiskCache.promiseCachedFile(uniqueKey);

										final Promise<Bitmap> cachedSuccessTask =
											cachedFilePromise
												.eventually(imageFile -> new QueuedPromise<>(new ImageDiskCacheWriter(uniqueKey, imageFile), imageAccessExecutor))
												.eventually(imageBitmap -> imageBitmap != null ? new Promise<>(imageBitmap) : new QueuedPromise<>(new RemoteImageAccessWriter(uniqueKey, imageDiskCache, connectionProvider, serviceFile.getKey()), imageAccessExecutor));

										final Promise<Bitmap> cachedErrorTask =
											cachedFilePromise
												.excuse(e -> {
													logger.warn("There was an error getting the file from the cache!", e);
													return e;
												})
												.eventually(e -> new QueuedPromise<>(new RemoteImageAccessWriter(uniqueKey, imageDiskCache, connectionProvider, serviceFile.getKey()), imageAccessExecutor));

										return Promise.whenAny(cachedSuccessTask, cachedErrorTask);
									});
							});
					});

			promiseProxy.proxy(promisedBitmap);
		}
	}

	private static class ImageMemoryWriter implements CancellableMessageWriter<Bitmap> {
		private final String uniqueKey;

		ImageMemoryWriter(String uniqueKey) {
			this.uniqueKey = uniqueKey;
		}


		@Override
		public Bitmap prepareMessage(CancellationToken cancellationToken) throws Throwable {
			if (cancellationToken.isCancelled())
				throw new CancellationException(cancellationMessage);

			final byte[] imageBytes = getBitmapBytesFromMemory(uniqueKey);
			return imageBytes.length > 0 ? getBitmapFromBytes(imageBytes) : null;
		}

		private static byte[] getBitmapBytesFromMemory(final String uniqueKey) {
			final Byte[] memoryImageBytes = imageMemoryCache.get(uniqueKey);

			if (memoryImageBytes == null) return new byte[0];

			final byte[] imageBytes = new byte[memoryImageBytes.length];
			for (int i = 0; i < memoryImageBytes.length; i++)
				imageBytes[i] = memoryImageBytes[i];

			return imageBytes;
		}
	}

	private static class ImageDiskCacheWriter implements MessageWriter<Bitmap> {

		private final String uniqueKey;
		private final File imageCacheFile;

		ImageDiskCacheWriter(String uniqueKey, File imageCacheFile) {
			this.uniqueKey = uniqueKey;

			this.imageCacheFile = imageCacheFile;
		}

		@Override
		public Bitmap prepareMessage() {
			if (imageCacheFile != null) {
				final byte[] imageBytes = putBitmapIntoMemory(uniqueKey, imageCacheFile);
				if (imageBytes.length > 0)
					return getBitmapFromBytes(imageBytes);
			}

			return null;
		}
	}

	private static class RemoteImageAccessWriter implements CancellableMessageWriter<Bitmap> {

		private final String uniqueKey;
		private final DiskFileCache imageDiskCache;
		private final IConnectionProvider connectionProvider;
		private final int fileKey;

		private RemoteImageAccessWriter(String uniqueKey, DiskFileCache imageDiskCache, IConnectionProvider connectionProvider, int fileKey) {
			this.uniqueKey = uniqueKey;
			this.imageDiskCache = imageDiskCache;
			this.connectionProvider = connectionProvider;
			this.fileKey = fileKey;
		}

		@Override
		public Bitmap prepareMessage(CancellationToken cancellationToken) throws Throwable {
			try {
				final HttpURLConnection connection = connectionProvider.getConnection("File/GetImage", "File=" + String.valueOf(fileKey), "Type=Full", "Pad=1", "Format=" + IMAGE_FORMAT, "FillTransparency=ffffff");
				try {
					// Connection failed to build
					if (connection == null)	return null;

					byte[] imageBytes;
					try {
						if (cancellationToken.isCancelled())
							throw new CancellationException(cancellationMessage);

						try (InputStream is = connection.getInputStream()) {
							imageBytes = IOUtils.toByteArray(is);
						} catch (InterruptedIOException interruptedIoException) {
							logger.warn("Copying the input stream to a byte array was interrupted", interruptedIoException);
							throw interruptedIoException;
						}

						if (imageBytes.length == 0) {
							return null;
						}
					} catch (FileNotFoundException fe) {
						logger.warn("Image not found!");
						return null;
					}

					imageDiskCache
						.put(uniqueKey, imageBytes)
						.excuse(ioe -> {
							logger.error("Error writing serviceFile!", ioe);
							return null;
						});

					putBitmapIntoMemory(uniqueKey, imageBytes);

					if (cancellationToken.isCancelled())
						throw new CancellationException(cancellationMessage);

					return getBitmapFromBytes(imageBytes);
				} catch (Exception e) {
					logger.error(e.toString(), e);
					throw e;
				} finally {
					if (connection != null)
						connection.disconnect();
				}
			} catch (IOException e) {
				logger.error("There was an error getting the connection for images", e);
				throw e;
			}
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
			logger.error("Could not find serviceFile.", e);
			return new byte[0];
		} catch (IOException e) {
			logger.error("Error reading serviceFile.", e);
			return new byte[0];
		}

		putBitmapIntoMemory(uniqueKey, bytes);
		return bytes;
	}

}
