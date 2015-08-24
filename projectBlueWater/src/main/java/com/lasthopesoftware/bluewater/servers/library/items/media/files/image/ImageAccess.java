package com.lasthopesoftware.bluewater.servers.library.items.media.files.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.support.v4.util.LruCache;

import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.File;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.cache.DiskFileCache;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.servers.repository.Library;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageAccess implements ISimpleTask<Void, Void, Bitmap> {
	
	public static final String IMAGE_FORMAT = "jpg";
	
	private static final ExecutorService imageAccessExecutor = Executors.newSingleThreadExecutor();
	
	public static ImageAccess getImage(final Context context, ConnectionProvider connectionProvider, final int fileKey, final OnCompleteListener<Void, Void, Bitmap> onGetBitmapComplete) {
		return getImage(context, connectionProvider, new File(connectionProvider, fileKey), onGetBitmapComplete);
	}
	
	public static ImageAccess getImage(final Context context, ConnectionProvider connectionProvider, final IFile file, final OnCompleteListener<Void, Void, Bitmap> onGetBitmapComplete) {
		final ImageAccess imageAccessTask = new ImageAccess(context, connectionProvider, file, onGetBitmapComplete);
		imageAccessTask.execute();
		return imageAccessTask;
	}

	private SimpleTask<Void, Void, Bitmap> mImageAccessTask;
	
	private ImageAccess(final Context context, final ConnectionProvider connectionProvider, final IFile file, final OnCompleteListener<Void, Void, Bitmap> onGetBitmapComplete) {
		super();
		
		mImageAccessTask = new SimpleTask<>(new GetFileImageOnExecute(context, connectionProvider, file));
		mImageAccessTask.addOnCompleteListener(onGetBitmapComplete);
	}
	
	private void execute() {
		mImageAccessTask.execute(imageAccessExecutor);
	}
		
	public void cancel() {
		mImageAccessTask.cancel(false);
	}
	
	private static class GetFileImageOnExecute implements OnExecuteListener<Void, Void, Bitmap> {
		
		private static final Logger logger = LoggerFactory.getLogger(GetFileImageOnExecute.class);
		
		private static final int MAX_DISK_CACHE_SIZE = 50 * 1024 * 1024; // 100 * 1024 * 1024 for 100MB of cache
		private static final int MAX_MEMORY_CACHE_SIZE = 5;
		private static final int MAX_DAYS_IN_CACHE = 30;
		private static final String IMAGES_CACHE_NAME = "images";
		
		private static Bitmap fillerBitmap;
		private static final LruCache<String, Byte[]> imageMemoryCache = new LruCache<>(MAX_MEMORY_CACHE_SIZE);
		
		private final Context context;
		private final ConnectionProvider connectionProvider;
		private final IFile file;
		
		public GetFileImageOnExecute(final Context context, ConnectionProvider connectionProvider, final IFile file) {
			this.context = context;
			this.connectionProvider = connectionProvider;
			this.file = file;
		}
		
		@Override
		public Bitmap onExecute(ISimpleTask<Void, Void, Bitmap> owner, Void... params) throws Exception {
			if (owner.isCancelled()) return getFillerBitmap();
			
			String uniqueKey;
			try {
				// First try storing by the album artist, which can cover the artist for the entire album (i.e. an album with various
				// artists), and then by artist if that field is empty
				String artist = file.getProperty(FilePropertiesProvider.ALBUM_ARTIST);
				if (artist == null || artist.isEmpty())
					artist = file.getProperty(FilePropertiesProvider.ARTIST);
				
				uniqueKey = artist + ":" + file.getProperty(FilePropertiesProvider.ALBUM);
			} catch (IOException ioE) {
				logger.error("Error getting file properties.");
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
				final HttpURLConnection conn = connectionProvider.getConnection(
						"File/GetImage",
						"File=" + String.valueOf(file.getKey()),
						"Type=Full",
						"Pad=1",
						"Format=" + IMAGE_FORMAT,
						"FillTransparency=ffffff");
				
				// Connection failed to build
				if (conn == null) return getFillerBitmap();
				
				try {
					//isCancelled was called, return an empty bitmap but do not put it into the cache
					if (owner.isCancelled()) return getFillerBitmap();
					
					final InputStream is = conn.getInputStream();
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
				} finally {
					conn.disconnect();
				}

				try {
					final java.io.File cacheDir = DiskFileCache.getDiskCacheDir(context, IMAGES_CACHE_NAME);
					if (!cacheDir.exists())
						cacheDir.mkdirs();
					final java.io.File file = java.io.File.createTempFile(String.valueOf(library.getId()) + "-" + IMAGES_CACHE_NAME, "." + IMAGE_FORMAT, cacheDir);

					imageDiskCache.put(uniqueKey, file, imageBytes);
				} catch (IOException ioe) {
					logger.error("Error writing file!", ioe);
				}

				putBitmapIntoMemory(uniqueKey, imageBytes);
				return getBitmapFromBytes(imageBytes);
			} catch (Exception e) {
				logger.error(e.toString(), e);
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
		
		private static Bitmap getFillerBitmap() {
			if (fillerBitmap != null) return getBitmapCopy(fillerBitmap);

			fillerBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
			// Make a canvas with which we can draw to the bitmap
			final Canvas canvas = new Canvas(fillerBitmap);

			// Fill with white
			canvas.drawColor(0xffffffff);
			return getBitmapCopy(fillerBitmap);
		}
	}

	@Override
	public Bitmap get() throws ExecutionException, InterruptedException {
		return mImageAccessTask.get();
	}

	@Override
	public Exception getException() {
		return mImageAccessTask.getException();
	}

	@Override
	public ISimpleTask<Void, Void, Bitmap> cancel(boolean interrupt) {
		mImageAccessTask.cancel(interrupt);
		return this;
	}

	@Override
	public SimpleTaskState getState() {
		return mImageAccessTask.getState();
	}

	@Override
	public ISimpleTask<Void, Void, Bitmap> addOnStartListener(com.lasthopesoftware.threading.ISimpleTask.OnStartListener<Void, Void, Bitmap> listener) {
		mImageAccessTask.addOnStartListener(listener);
		return this;
	}

	@Override
	public ISimpleTask<Void, Void, Bitmap> addOnProgressListener(com.lasthopesoftware.threading.ISimpleTask.OnProgressListener<Void, Void, Bitmap> listener) {
		mImageAccessTask.addOnProgressListener(listener);
		return this;
	}

	@Override
	public ISimpleTask<Void, Void, Bitmap> addOnCompleteListener(com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener<Void, Void, Bitmap> listener) {
		mImageAccessTask.addOnCompleteListener(listener);
		return this;
	}

	@Override
	public ISimpleTask<Void, Void, Bitmap> addOnCancelListener(com.lasthopesoftware.threading.ISimpleTask.OnCancelListener<Void, Void, Bitmap> listener) {
		mImageAccessTask.addOnCancelListener(listener);
		return this;
	}

	@Override
	public ISimpleTask<Void, Void, Bitmap> addOnErrorListener(com.lasthopesoftware.threading.ISimpleTask.OnErrorListener<Void, Void, Bitmap> listener) {
		mImageAccessTask.addOnErrorListener(listener);
		return this;
	}

	@Override
	public ISimpleTask<Void, Void, Bitmap> removeOnStartListener(com.lasthopesoftware.threading.ISimpleTask.OnStartListener<Void, Void, Bitmap> listener) {
		mImageAccessTask.removeOnStartListener(listener);
		return this;
	}

	@Override
	public ISimpleTask<Void, Void, Bitmap> removeOnProgressListener(com.lasthopesoftware.threading.ISimpleTask.OnProgressListener<Void, Void, Bitmap> listener) {
		mImageAccessTask.removeOnProgressListener(listener);
		return this;
	}

	@Override
	public ISimpleTask<Void, Void, Bitmap> removeOnCompleteListener(com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener<Void, Void, Bitmap> listener) {
		mImageAccessTask.removeOnCompleteListener(listener);
		return this;
	}

	@Override
	public ISimpleTask<Void, Void, Bitmap> removeOnCancelListener(com.lasthopesoftware.threading.ISimpleTask.OnCancelListener<Void, Void, Bitmap> listener) {
		mImageAccessTask.removeOnCancelListener(listener);
		return this;
	}

	@Override
	public ISimpleTask<Void, Void, Bitmap> removeOnErrorListener(com.lasthopesoftware.threading.ISimpleTask.OnErrorListener<Void, Void, Bitmap> listener) {
		mImageAccessTask.removeOnErrorListener(listener);
		return this;
	}

	@Override
	public boolean isCancelled() {
		return mImageAccessTask.isCancelled();
	}
}
