package com.lasthopesoftware.bluewater.data.service.access;

import java.io.FileNotFoundException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.LoggerFactory;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.lasthopesoftware.bluewater.data.service.access.connection.JrConnection;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTask;

public class JrImageTask extends SimpleTask<Void, Void, Bitmap> {

	private static final int maxSize = (Runtime.getRuntime().maxMemory() / 32768) > 100 ? 100 : (int) (Runtime.getRuntime().maxMemory() / 32768);
	private static ConcurrentLinkedQueue<String> imageQueue = new ConcurrentLinkedQueue<String>();
	private static ConcurrentHashMap<String, Bitmap> imageCache = new ConcurrentHashMap<String, Bitmap>();
	private static Bitmap emptyBitmap;
	
	public JrImageTask(String uniqueId, int fileKey) {
		super();
		
		final String _uniqueId = uniqueId;
		final int _fileKey = fileKey;
		
		super.addOnExecuteListener(new OnExecuteListener<Void, Void, Bitmap>() {
			
			@Override
			public void onExecute(ISimpleTask<Void, Void, Bitmap> owner, Void... params) throws Exception {
				if (imageCache.containsKey(_uniqueId)) {
					owner.setResult(imageCache.get(_uniqueId));
					return;
				}
				
				Bitmap returnBmp = null;
				
				try {
					JrConnection conn = new JrConnection(
												"File/GetImage", 
												"File=" + String.valueOf(_fileKey), 
												"Type=Full", 
												"Pad=1",
												"Format=jpg",
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
				
				owner.setResult(returnBmp);
				imageCache.put(_uniqueId, returnBmp);
				
				if (imageQueue.size() <= maxSize) return;
				
				if (!imageCache.contains(imageQueue.peek())) return;
				
				Bitmap oldBmp = imageCache.remove(imageQueue.poll());
				if (oldBmp != emptyBitmap) oldBmp.recycle();
			}
		});
	}
}
