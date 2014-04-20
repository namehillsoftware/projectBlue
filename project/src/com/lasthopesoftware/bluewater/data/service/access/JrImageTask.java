package com.lasthopesoftware.bluewater.data.service.access;

import java.io.FileNotFoundException;

import org.slf4j.LoggerFactory;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;

import com.lasthopesoftware.bluewater.activities.ViewNowPlaying;
import com.lasthopesoftware.bluewater.data.service.access.connection.JrConnection;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTask;

public class JrImageTask extends SimpleTask<Void, Void, Bitmap> {

	private static LruCache<String, Bitmap> imageCache = new LruCache<String, Bitmap>(100);
	private static Bitmap emptyBitmap;
	
	public JrImageTask(String uniqueId, int fileKey) {
		super();
		
		final String _uniqueId = uniqueId;
		final int _fileKey = fileKey;
		
		super.addOnExecuteListener(new OnExecuteListener<Void, Void, Bitmap>() {
			
			@Override
			public void onExecute(ISimpleTask<Void, Void, Bitmap> owner, Void... params) throws Exception {
				
				synchronized (imageCache) {
					if (imageCache.get(_uniqueId) != null) {
						owner.setResult(imageCache.get(_uniqueId));
						return;
					}
				}
				
				Bitmap returnBmp = null;
				
				try {
					JrConnection conn = new JrConnection(
												"File/GetImage", 
												"File=" + String.valueOf(_fileKey), 
												"Type=Full", 
												"Pad=1",
												"Format=png",
												"FillTransparency=ffffff");
					
					if (isCancelled()) return;
					
					try {
						returnBmp = BitmapFactory.decodeStream(conn.getInputStream());
					} finally {
						conn.disconnect();
					}
				} catch (FileNotFoundException fe) {
					LoggerFactory.getLogger(JrImageTask.class).warn("Image not found!");
				} catch (Exception e) {
					LoggerFactory.getLogger(JrImageTask.class).error(e.toString(), e);
				}
				
				if (returnBmp == null) {
					if (emptyBitmap == null) {
						emptyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
					}
					
					returnBmp = emptyBitmap;
				}
				
				synchronized (imageCache) {
					imageCache.put(_uniqueId, returnBmp);				
				}
				
				owner.setResult(returnBmp);
			}
		});
	}
}
