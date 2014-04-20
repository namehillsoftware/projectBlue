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

public class JrImageTask extends SimpleTask<String, Void, Bitmap> {

	private static LruCache<String, Bitmap> imageCache = new LruCache<String, Bitmap>(100);
	private static Bitmap emptyBitmap;
	
	public JrImageTask() {
		super();
		
		super.addOnExecuteListener(new OnExecuteListener<String, Void, Bitmap>() {
			
			@Override
			public void onExecute(ISimpleTask<String, Void, Bitmap> owner, String... params) throws Exception {
				String uId = params[0];
				String fileKey = params[1];
				
				synchronized (imageCache) {
					if (imageCache.get(uId) != null) {
						owner.setResult(imageCache.get(uId));
						return;
					}
				}
				
				Bitmap returnBmp = null;
				
				try {
					JrConnection conn = new JrConnection(
												"File/GetImage", 
												"File=" + fileKey, 
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
					LoggerFactory.getLogger(ViewNowPlaying.class).warn("Image not found!");
				} catch (Exception e) {
					LoggerFactory.getLogger(ViewNowPlaying.class).error(e.toString(), e);
				}
				
				if (returnBmp == null) {
					if (emptyBitmap == null) {
						emptyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
					}
					
					returnBmp = emptyBitmap;
				}
				
				synchronized (imageCache) {
					imageCache.put(uId, returnBmp);				
				}
				
				owner.setResult(returnBmp);
			}
		});
	}
}
