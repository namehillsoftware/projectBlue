package com.lasthopesoftware.bluewater.data.service.access;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;

import com.lasthopesoftware.bluewater.data.service.access.connection.ConnectionManager;
import com.lasthopesoftware.bluewater.data.service.helpers.FileCache;
import com.lasthopesoftware.bluewater.data.service.objects.File;
import com.lasthopesoftware.bluewater.data.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.data.sqlite.objects.Library;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTask;

public class ImageAccess extends SimpleTask<Void, Void, Bitmap> {
	
	public static final String IMAGE_FORMAT = "jpg";

	
	public ImageAccess(final Context context, final int fileKey) {
		this(context, new File(fileKey));
	}
	
	public ImageAccess(final Context context, final File file) {
		super();
		
		super.setOnExecuteListener(new GetFileImageOnExecute(context, file));
	}
	

	@Override
	public final void setOnExecuteListener(OnExecuteListener<Void, Void, Bitmap> listener) {
		throw new UnsupportedOperationException("The on execute listener cannot be set for an ImageTask. It is already set in the constructor.");
	}
		
	private static class GetFileImageOnExecute implements OnExecuteListener<Void, Void, Bitmap> {
		
		private static final Logger mLogger = LoggerFactory.getLogger(GetFileImageOnExecute.class);
		
		private static final int MAX_DISK_CACHE_SIZE = 100 * 1024 * 1024; // 100 * 1024 * 1024 for 100MB of cache
		private static final int MAX_MEMORY_CACHE_SIZE = 5;
		private static final String IMAGES_CACHE_NAME = "images";
		
		private static final Bitmap mFillerBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
		private static final LruCache<String, Byte[]> mImageMemoryCache = new LruCache<String, Byte[]>(MAX_MEMORY_CACHE_SIZE);
		
		private final Context mContext;
		private final File mFile;
		
		public GetFileImageOnExecute(final Context context, final File file) {
			mContext = context;
			mFile = file;
		}
		
		@Override
		public Bitmap onExecute(ISimpleTask<Void, Void, Bitmap> owner, Void... params) throws Exception {
			final Library library = LibrarySession.GetLibrary(mContext);
			final FileCache imageDiskCache = new FileCache(mContext, library, IMAGES_CACHE_NAME, MAX_DISK_CACHE_SIZE);
			
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
				final HttpURLConnection conn = ConnectionManager.getConnection(
											"File/GetImage", 
											"File=" + String.valueOf(mFile.getKey()), 
											"Type=Full", 
											"Pad=1",
											"Format=" + IMAGE_FORMAT,
											"FillTransparency=ffffff");
				
				// Connection failed to build or isCancelled was called, return an empty bitmap
				// but do not put it into the cache
				if (conn == null || owner.isCancelled()) return getFillerBitmap();
				
				try {
					imageBytes = IOUtils.toByteArray(conn.getInputStream());
					if (imageBytes.length == 0)
						return getFillerBitmap();
				} catch (FileNotFoundException fe) {
					mLogger.warn("Image not found!");
					return getFillerBitmap();
				} finally {
					conn.disconnect();
				}
				
				final java.io.File cacheDir = FileCache.getDiskCacheDir(mContext, IMAGES_CACHE_NAME);
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
			Byte[] memoryImageBytes = null;
			synchronized(mImageMemoryCache) {
				memoryImageBytes = mImageMemoryCache.get(uniqueKey);
			}
			
			if (memoryImageBytes == null || memoryImageBytes.length == 0) return new byte[0];
			
			final byte[] imageBytes = new byte[memoryImageBytes.length];
			for (int i = 0; i < memoryImageBytes.length; i++)
				imageBytes[i] = memoryImageBytes[i].byteValue();
			
			return imageBytes;
		}
		
		private static final byte[] putBitmapIntoMemory(final String uniqueKey, final java.io.File file) {
			final int size = (int) file.length();
		    final byte[] bytes = new byte[size];
		    try {
		        final BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
		        buf.read(bytes, 0, bytes.length);
		        buf.close();
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
			
			synchronized(mImageMemoryCache) {
				mImageMemoryCache.put(uniqueKey, memoryImageBytes);
			}
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
}
