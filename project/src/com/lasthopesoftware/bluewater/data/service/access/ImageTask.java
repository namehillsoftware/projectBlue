package com.lasthopesoftware.bluewater.data.service.access;

import java.io.FileNotFoundException;
import java.net.HttpURLConnection;

import org.slf4j.LoggerFactory;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.lasthopesoftware.bluewater.data.service.access.connection.ConnectionManager;
import com.lasthopesoftware.bluewater.data.service.objects.File;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTask;

public class ImageTask extends SimpleTask<Void, Void, Bitmap> {

	private static final int maxSize = (Runtime.getRuntime().maxMemory() / 32768) > 50 ? 50 : (int) (Runtime.getRuntime().maxMemory() / 32768);
	private static final ConcurrentLinkedHashMap<String, Bitmap> imageCache = new ConcurrentLinkedHashMap.Builder<String, Bitmap>().maximumWeightedCapacity(maxSize).build();
	private static final Bitmap mEmptyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
	
	private final Context mContext;
	
	public ImageTask(final Context context, final int fileKey) {
		this(context, new File(fileKey));
	}
	
	public ImageTask(final Context context, final File file) {
		super();
		
		mContext = context;
		
		super.setOnExecuteListener(new OnExecuteListener<Void, Void, Bitmap>() {
			
			@Override
			public Bitmap onExecute(ISimpleTask<Void, Void, Bitmap> owner, Void... params) throws Exception {
				final String uniqueId = file.getProperty(FileProperties.ARTIST) + ":" + file.getProperty(FileProperties.ALBUM);
				
				if (imageCache.containsKey(uniqueId))
					return getBitmapCopy(imageCache.get(uniqueId));
				
				Bitmap returnBmp = null;
				try {
					HttpURLConnection conn = ConnectionManager.getConnection(
												"File/GetImage", 
												"File=" + String.valueOf(file.getKey()), 
												"Type=Full", 
												"Pad=1",
												"Format=jpg",
												"FillTransparency=ffffff");
					
					// Connection failed to build or isCancelled was called, return an empty bitmap
					// but do not put it into the cache
					if (conn == null || isCancelled()) return getBitmapCopy(mEmptyBitmap);
					
					try {
						returnBmp = BitmapFactory.decodeStream(conn.getInputStream());
					} finally {
						conn.disconnect();
					}
				} catch (FileNotFoundException fe) {
					LoggerFactory.getLogger(getClass()).warn("Image not found!");
				} catch (Exception e) {
					LoggerFactory.getLogger(getClass()).error(e.toString(), e);
				}
				
				if (returnBmp == null)
					returnBmp = mEmptyBitmap;
				
				imageCache.put(uniqueId, returnBmp);
				
				return getBitmapCopy(returnBmp);
			}
		});
	}
	
	@Override
	public final void setOnExecuteListener(OnExecuteListener<Void, Void, Bitmap> listener) {
		throw new UnsupportedOperationException("The on execute listener cannot be set for an ImageTask. It is already set in the constructor.");
	}
	
	private Bitmap getBitmapCopy(Bitmap src) {
		return src.copy(src.getConfig(), false);
	}
	
	// Creates a unique subdirectory of the designated app cache directory. Tries to use external
	// but if not mounted, falls back on internal storage.
	private final static java.io.File getDiskCacheDir(final Context context, final String uniqueName) {
	    // Check if media is mounted or storage is built-in, if so, try and use external cache dir
	    // otherwise use internal cache dir
	    final String cachePath =
	            Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ?
            		context.getExternalCacheDir().getPath() :
                    context.getCacheDir().getPath();

	    return new java.io.File(cachePath + java.io.File.separator + uniqueName);
	}
}
