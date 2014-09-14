package com.lasthopesoftware.bluewater.data.service.access;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;

import org.slf4j.LoggerFactory;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.lasthopesoftware.bluewater.data.service.access.connection.ConnectionManager;
import com.lasthopesoftware.bluewater.data.service.helpers.FileCache;
import com.lasthopesoftware.bluewater.data.service.objects.File;
import com.lasthopesoftware.bluewater.data.sqlite.access.LibrarySession;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTask;

public class ImageAccess extends SimpleTask<Void, Void, Bitmap> {
		
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
		private static final int maxSize = 1024 * 1024 * 1024;
		private static final Bitmap mEmptyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
		
		private final Context mContext;
		private final File mFile;
		
		private static final String IMAGES_CACHE_NAME = "images";
		
		public GetFileImageOnExecute(final Context context, final File file) {
			mContext = context;
			mFile = file;
		}
		
		@Override
		public Bitmap onExecute(ISimpleTask<Void, Void, Bitmap> owner, Void... params) throws Exception {
			final FileCache imageCache = new FileCache(mContext, LibrarySession.GetLibrary(mContext), IMAGES_CACHE_NAME, maxSize);
			
			String uniqueKey = null;
			try {
				uniqueKey = mFile.getProperty(FileProperties.ARTIST) + ":" + mFile.getProperty(FileProperties.ALBUM);
			} catch (IOException ioE) {
				LoggerFactory.getLogger(getClass()).error("Error getting file properties.", ioE);
				return getBitmapCopy(mEmptyBitmap);
			}
			
			
			if (imageCache.containsKey(uniqueKey)) {
				try {
					return BitmapFactory.decodeFile(imageCache.get(uniqueKey).getCanonicalPath());
				} catch (IOException ioE) {
					LoggerFactory.getLogger(getClass()).error("Error getting file path.", ioE);
				}
			}
			
			Bitmap returnBmp = null;
			try {
				HttpURLConnection conn = ConnectionManager.getConnection(
											"File/GetImage", 
											"File=" + String.valueOf(mFile.getKey()), 
											"Type=Full", 
											"Pad=1",
											"Format=jpg",
											"FillTransparency=ffffff");
				
				// Connection failed to build or isCancelled was called, return an empty bitmap
				// but do not put it into the cache
				if (conn == null || owner.isCancelled()) return getBitmapCopy(mEmptyBitmap);
				
				try {
					returnBmp = BitmapFactory.decodeStream(conn.getInputStream());
				} finally {
					conn.disconnect();
				}
				
				if (returnBmp == null)
					returnBmp = mEmptyBitmap;
				
				final java.io.File file = new java.io.File(FileCache.getDiskCacheDir(mContext, IMAGES_CACHE_NAME), uniqueKey + ".jpg");
				String fileName;
				try {
					fileName = file.getCanonicalPath();

					final ByteBuffer byteBuffer = ByteBuffer.allocate(returnBmp.getByteCount());
					returnBmp.copyPixelsToBuffer(byteBuffer);
					
					final FileOutputStream fos = mContext.openFileOutput(fileName, Context.MODE_PRIVATE);
					try {
						fos.write(byteBuffer.array());
					} finally {
						fos.close();
					}
				} catch (IOException e) {
					LoggerFactory.getLogger(getClass()).error("Unable to write to file!", e);
				}
				
				return getBitmapCopy(returnBmp);
			} catch (FileNotFoundException fe) {
				LoggerFactory.getLogger(getClass()).warn("Image not found!");
			} catch (Exception e) {
				LoggerFactory.getLogger(getClass()).error(e.toString(), e);
			} finally {
				if (returnBmp != null)
					returnBmp.recycle();
			}
			
			return null;
		}

		private static Bitmap getBitmapCopy(Bitmap src) {
			return src.copy(src.getConfig(), false);
		}
	}
}
