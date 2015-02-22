package com.lasthopesoftware.bluewater.servers.library.items.media.files.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;

import com.lasthopesoftware.bluewater.disk.cache.DiskFileCache;
import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.disk.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.File;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FileProperties;
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
	
	public static ImageAccess getImage(final Context context, final int fileKey, final OnCompleteListener<Void, Void, Bitmap> onGetBitmapComplete) {
		return getImage(context, new File(fileKey), onGetBitmapComplete);
	}
	
	public static ImageAccess getImage(final Context context, final IFile file, final OnCompleteListener<Void, Void, Bitmap> onGetBitmapComplete) {
		final ImageAccess imageAccessTask = new ImageAccess(context, file, onGetBitmapComplete);
		imageAccessTask.execute();
		return imageAccessTask;
	}

	private SimpleTask<Void, Void, Bitmap> mImageAccessTask;
	
	private ImageAccess(final Context context, final IFile file, final OnCompleteListener<Void, Void, Bitmap> onGetBitmapComplete) {
		super();
		
		mImageAccessTask = new SimpleTask<Void, Void, Bitmap>(new GetFileImageOnExecute(context, file));
		mImageAccessTask.addOnCompleteListener(onGetBitmapComplete);
	}
	
	private void execute() {
		mImageAccessTask.execute(imageAccessExecutor);
	}
		
	public void cancel() {
		mImageAccessTask.cancel(false);
	}
	
	private static class GetFileImageOnExecute implements OnExecuteListener<Void, Void, Bitmap> {
		
		private static final Logger mLogger = LoggerFactory.getLogger(GetFileImageOnExecute.class);
		
		private static final int MAX_DISK_CACHE_SIZE = 100 * 1024 * 1024; // 100 * 1024 * 1024 for 100MB of cache
		private static final int MAX_MEMORY_CACHE_SIZE = 5;
		private static final int MAX_DAYS_IN_CACHE = 30;
		private static final String IMAGES_CACHE_NAME = "images";
		
		private static final Bitmap mFillerBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
		private static final LruCache<String, Byte[]> mImageMemoryCache = new LruCache<String, Byte[]>(MAX_MEMORY_CACHE_SIZE);
		
		private final Context mContext;
		private final IFile mFile;
		
		public GetFileImageOnExecute(final Context context, final IFile file) {
			mContext = context;
			mFile = file;
		}
		
		@Override
		public Bitmap onExecute(ISimpleTask<Void, Void, Bitmap> owner, Void... params) throws Exception {
			final Library library = LibrarySession.GetLibrary(mContext);
			final DiskFileCache imageDiskCache = new DiskFileCache(mContext, library, IMAGES_CACHE_NAME, MAX_DAYS_IN_CACHE, MAX_DISK_CACHE_SIZE);
			
			if (owner.isCancelled()) return getFillerBitmap();
			
			String uniqueKey = null;
			try {
				// First try storing by the album artist, which can cover the artist for the entire album (i.e. an album with various
				// artists), and then by artist if that field is empty
				String artist = mFile.getProperty(FileProperties.ALBUM_ARTIST);
				if (artist == null || artist.isEmpty())
					artist = mFile.getProperty(FileProperties.ARTIST);
				
				uniqueKey = artist + ":" + mFile.getProperty(FileProperties.ALBUM);
			} catch (IOException ioE) {
				mLogger.error("Error getting file properties.");
				return getFillerBitmap();
			}
			
			byte[] imageBytes = getBitmapBytesFromMemory(uniqueKey);
			if (imageBytes.length > 0) return getBitmapFromBytes(imageBytes);
			
			final java.io.File imageCacheFile = imageDiskCache.get(uniqueKey);
			if (imageCacheFile != null) {
				imageBytes = putBitmapIntoMemory(uniqueKey, imageCacheFile);
				if (imageBytes.length > 0)
					return getBitmapFromBytes(imageBytes);
			}
			
			try {
				final HttpURLConnection conn = ConnectionProvider.getConnection(
											"File/GetImage", 
											"File=" + String.valueOf(mFile.getKey()), 
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
					mLogger.warn("Image not found!");
					return getFillerBitmap();
				} finally {
					conn.disconnect();
				}
				
				final java.io.File cacheDir = DiskFileCache.getDiskCacheDir(mContext, IMAGES_CACHE_NAME);
				if (!cacheDir.exists())
					cacheDir.mkdirs();
				final java.io.File file = java.io.File.createTempFile(String.valueOf(library.getId()) + "-" + IMAGES_CACHE_NAME, "." + IMAGE_FORMAT, cacheDir);

				imageDiskCache.put(uniqueKey, file, imageBytes);
				putBitmapIntoMemory(uniqueKey, imageBytes);
					
				return getBitmapFromBytes(imageBytes);
			} catch (Exception e) {
				mLogger.error(e.toString(), e);
			}
			
			return null;
		}
		
		private static final byte[] getBitmapBytesFromMemory(final String uniqueKey) {
			final Byte[] memoryImageBytes = mImageMemoryCache.get(uniqueKey);
			
			if (memoryImageBytes == null) return new byte[0];
			
			final byte[] imageBytes = new byte[memoryImageBytes.length];
			for (int i = 0; i < memoryImageBytes.length; i++)
				imageBytes[i] = memoryImageBytes[i].byteValue();
			
			return imageBytes;
		}
		
		private static final byte[] putBitmapIntoMemory(final String uniqueKey, final java.io.File file) {
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
		    	mLogger.error("Could not find file.", e);
		    	return new byte[0];
		    } catch (IOException e) {
		    	mLogger.error("Error reading file.", e);
		    	return new byte[0];
		    }
		    
		    putBitmapIntoMemory(uniqueKey, bytes);
		    return bytes;
		}
		
		private static final void putBitmapIntoMemory(final String uniqueKey, final byte[] imageBytes) {
			final Byte[] memoryImageBytes = new Byte[imageBytes.length];
			
			for (int i = 0; i < imageBytes.length; i++)
				memoryImageBytes[i] = Byte.valueOf(imageBytes[i]);
			
			mImageMemoryCache.put(uniqueKey, memoryImageBytes);
		}

		private static final Bitmap getBitmapFromBytes(final byte[] imageBytes) {
			return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
		}
		
		private static final Bitmap getBitmapCopy(final Bitmap src) {
			return src.copy(src.getConfig(), false);
		}
		
		private static final Bitmap getFillerBitmap() {
			return getBitmapCopy(mFillerBitmap);
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
