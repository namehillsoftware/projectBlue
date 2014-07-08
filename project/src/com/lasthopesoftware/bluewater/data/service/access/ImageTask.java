package com.lasthopesoftware.bluewater.data.service.access;

import java.io.FileNotFoundException;
import java.net.HttpURLConnection;

import org.slf4j.LoggerFactory;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.lasthopesoftware.bluewater.data.service.access.connection.ConnectionManager;
import com.lasthopesoftware.bluewater.data.service.objects.File;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTask;

public class ImageTask extends SimpleTask<Void, Void, Bitmap> {

	private static final int maxSize = (Runtime.getRuntime().maxMemory() / 32768) > 100 ? 100 : (int) (Runtime.getRuntime().maxMemory() / 32768);
	private static final ConcurrentLinkedHashMap<String, Bitmap> imageCache = new ConcurrentLinkedHashMap.Builder<String, Bitmap>().maximumWeightedCapacity(maxSize).build();
	private static final Bitmap mEmptyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
	
	public ImageTask(int fileKey) {
		this(new File(fileKey));
	}
	
	public ImageTask(File file) {
		super();
		
		final File _file = file;
		
		super.setOnExecuteListener(new OnExecuteListener<Void, Void, Bitmap>() {
			
			@Override
			public Bitmap onExecute(ISimpleTask<Void, Void, Bitmap> owner, Void... params) throws Exception {
				final String uniqueId = _file.getProperty(FileProperties.ARTIST) + ":" + _file.getProperty(FileProperties.ALBUM);
				
				if (imageCache.containsKey(uniqueId))
					return getBitmapCopy(imageCache.get(uniqueId));
				
				Bitmap returnBmp = null;
				try {
					HttpURLConnection conn = ConnectionManager.getConnection(
												"File/GetImage", 
												"File=" + String.valueOf(_file.getKey()), 
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
	
	private Bitmap getBitmapCopy(Bitmap src) {
		return src.copy(src.getConfig(), false);
	}
}
