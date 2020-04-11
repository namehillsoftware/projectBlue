package com.lasthopesoftware.bluewater.client.browsing.items.media.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.collection.LruCache;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.ICache;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.IProvideCaches;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.CachedSessionFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.servers.selection.ISelectedLibraryIdentifierProvider;
import com.lasthopesoftware.resources.executors.CachedSingleThreadExecutor;
import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.MessengerOperator;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy;
import com.namehillsoftware.handoff.promises.propagation.PromiseProxy;
import com.namehillsoftware.handoff.promises.queued.MessageWriter;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter;
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;

import okhttp3.ResponseBody;

public class ImageProvider {

	private static final String IMAGE_FORMAT = "jpg";

	private static final CreateAndHold<ExecutorService> imageAccessExecutor = new Lazy<>(CachedSingleThreadExecutor::new);

	private static final Logger logger = LoggerFactory.getLogger(ImageProvider.class);

	private static final int MAX_MEMORY_CACHE_SIZE = 10;

	private static final LruCache<String, Byte[]> imageMemoryCache = new LruCache<>(MAX_MEMORY_CACHE_SIZE);

	private static final String cancellationMessage = "The image task was cancelled";

	private final ISelectedLibraryIdentifierProvider selectedLibraryIdentifierProvider;
	private final IConnectionProvider connectionProvider;
	private final CachedSessionFilePropertiesProvider cachedSessionFilePropertiesProvider;
	private final IProvideCaches caches;

	public ImageProvider(ISelectedLibraryIdentifierProvider selectedLibraryIdentifierProvider, IConnectionProvider connectionProvider, CachedSessionFilePropertiesProvider cachedSessionFilePropertiesProvider, IProvideCaches caches) {
		this.selectedLibraryIdentifierProvider = selectedLibraryIdentifierProvider;
		this.connectionProvider = connectionProvider;
		this.cachedSessionFilePropertiesProvider = cachedSessionFilePropertiesProvider;
		this.caches = caches;
	}

	public Promise<Bitmap> promiseFileBitmap(ServiceFile serviceFile) {
		return new Promise<>(new ImageOperator(serviceFile));
	}

	private class ImageOperator implements MessengerOperator<Bitmap> {

		private final ServiceFile serviceFile;

		ImageOperator(ServiceFile serviceFile) {
			this.serviceFile = serviceFile;
		}

		@Override
		public void send(Messenger<Bitmap> messenger) {
			final Promise<Map<String, String>> promisedFileProperties = cachedSessionFilePropertiesProvider.promiseFileProperties(serviceFile);

			final CancellationProxy cancellationProxy = new CancellationProxy();
			messenger.cancellationRequested(cancellationProxy);
			cancellationProxy.doCancel(promisedFileProperties);

			final PromiseProxy<Bitmap> promiseProxy = new PromiseProxy<>(messenger);
			final Promise<Bitmap> promisedBitmap =
				promisedFileProperties
					.then(fileProperties -> {
						// First try storing by the album artist, which can cover the artist for the entire album (i.e. an album with various
						// artists), and then by artist if that field is empty
						String artist = fileProperties.get(KnownFileProperties.ALBUM_ARTIST);
						if (artist == null || artist.isEmpty())
							artist = fileProperties.get(KnownFileProperties.ARTIST);

						String albumOrTrackName = fileProperties.get(KnownFileProperties.ALBUM);
						if (albumOrTrackName == null)
							albumOrTrackName = fileProperties.get(KnownFileProperties.NAME);

						return artist + ":" + albumOrTrackName;
					})
					.eventually(uniqueKey -> {
						final Promise<Bitmap> memoryTask = new QueuedPromise<>(new ImageMemoryWriter(uniqueKey), imageAccessExecutor.getObject());

						return memoryTask.eventually(bitmap -> {
							if (bitmap != null) return new Promise<>(bitmap);

							return caches.promiseCache(selectedLibraryIdentifierProvider.getSelectedLibraryId())
								.eventually(cache -> cache.promiseCachedFile(uniqueKey)
									.eventually(imageFile -> new QueuedPromise<>(new ImageDiskCacheWriter(uniqueKey, imageFile), imageAccessExecutor.getObject()))
									.eventually(imageBitmap -> imageBitmap != null
											? new Promise<>(imageBitmap)
											: promiseImage(connectionProvider, uniqueKey, cache, serviceFile.getKey()),
										error -> {
											logger.warn("There was an error getting the file from the cache!", error);
											return promiseImage(connectionProvider, uniqueKey, cache, serviceFile.getKey());
										}));
							});
					});

			promiseProxy.proxy(promisedBitmap);
		}
	}

	private static Promise<Bitmap> promiseImage(IConnectionProvider connectionProvider, String uniqueKey, ICache imageDiskCache, int fileKey) {
		return connectionProvider.promiseResponse("File/GetImage", "File=" + fileKey, "Type=Full", "Pad=1", "Format=" + IMAGE_FORMAT, "FillTransparency=ffffff")
			.eventually(response -> {
				final ResponseBody body = response.body();
				if (body == null) return Promise.empty();

				try {
					return new QueuedPromise<>(
						new RemoteImageAccessWriter(uniqueKey, imageDiskCache, body.bytes()),
						imageAccessExecutor.getObject());
				} finally {
					body.close();
				}
			}, e -> {
				if (e instanceof FileNotFoundException) {
					logger.warn("Image not found!");
					return Promise.empty();
				}

				if (e instanceof IOException) {
					logger.error("There was an error getting the connection for images", e);
					return Promise.empty();
				}

				logger.error(e.toString(), e);
				return new Promise<>(e);
			});
	}

	private static class ImageMemoryWriter implements CancellableMessageWriter<Bitmap> {
		private final String uniqueKey;

		ImageMemoryWriter(String uniqueKey) {
			this.uniqueKey = uniqueKey;
		}


		@Override
		public Bitmap prepareMessage(CancellationToken cancellationToken) {
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
		private final ICache imageDiskCache;
		private final byte[] imageBytes;

		private RemoteImageAccessWriter(String uniqueKey, ICache imageDiskCache, byte[] imageBytes) {
			this.uniqueKey = uniqueKey;
			this.imageDiskCache = imageDiskCache;
			this.imageBytes = imageBytes;
		}

		@Override
		public Bitmap prepareMessage(CancellationToken cancellationToken) {
			imageDiskCache
				.put(uniqueKey, imageBytes)
				.excuse(ioe -> {
					logger.error("Error writing cached file!", ioe);
					return null;
				});

			putBitmapIntoMemory(uniqueKey, imageBytes);

			if (cancellationToken.isCancelled())
				throw new CancellationException(cancellationMessage);

			return getBitmapFromBytes(imageBytes);
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
		try {
			try (FileInputStream fis = new FileInputStream(file); ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
				IOUtils.copy(fis, buffer);

				final byte[] bytes = buffer.toByteArray();
				putBitmapIntoMemory(uniqueKey, bytes);
				return bytes;
			}
		} catch (FileNotFoundException e) {
			logger.error("Could not find cached file.", e);
			return new byte[0];
		} catch (IOException e) {
			logger.error("Error reading cached file.", e);
			return new byte[0];
		}
	}
}
