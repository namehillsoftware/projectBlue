package com.lasthopesoftware.bluewater.data.service.access;

import java.io.FileNotFoundException;
import java.net.HttpURLConnection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.LoggerFactory;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.lasthopesoftware.bluewater.data.service.access.connection.ConnectionManager;
import com.lasthopesoftware.bluewater.data.service.objects.File;
import com.lasthopesoftware.bluewater.data.sqlite.access.DatabaseHandler;
import com.lasthopesoftware.bluewater.data.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.data.sqlite.objects.CachedFile;
import com.lasthopesoftware.bluewater.data.sqlite.objects.Library;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;
import com.lasthopesoftware.threading.SimpleTaskState;

public class ImageAccess {

	private static final int maxSize = (Runtime.getRuntime().maxMemory() / 32768) > 50 ? 50 : (int) (Runtime.getRuntime().maxMemory() / 32768);
	private static final ConcurrentLinkedHashMap<String, Bitmap> imageCache = new ConcurrentLinkedHashMap.Builder<String, Bitmap>().maximumWeightedCapacity(maxSize).build();
	private static final Bitmap mEmptyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
	
	private static final ExecutorService mImageAccessExecutor = Executors.newSingleThreadExecutor();
	
	private final Context mContext;
	
	private final SimpleTask<Void, Void, Bitmap> mGetImageTask;
	
	public ImageAccess(final Context context, final int fileKey) {
		this(context, new File(fileKey));
	}
	
	public ImageAccess(final Context context, final File file) {
		super();
		
		mContext = context;
		mGetImageTask = new SimpleTask<Void, Void, Bitmap>(new ISimpleTask.OnExecuteListener<Void, Void, Bitmap>() {

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
					if (conn == null || owner.isCancelled()) return getBitmapCopy(mEmptyBitmap);
					
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
	
	private void getCachedImage(final String uniqueKey, final OnCompleteListener<Void, Void, Bitmap> onGetImageComplete) {
		LibrarySession.GetLibrary(mContext, new OnCompleteListener<Integer, Void, Library>() {
			
			@Override
			public void onComplete(ISimpleTask<Integer, Void, Library> owner, final Library library) {
			
				final SimpleTask<Void, Void, Bitmap> getCachedImageTask = new SimpleTask<Void, Void, Bitmap>(new ISimpleTask.OnExecuteListener<Void, Void, Bitmap>() {

					@Override
					public Bitmap onExecute(ISimpleTask<Void, Void, Bitmap> owner, Void... params) throws Exception {
						final DatabaseHandler handler = new DatabaseHandler(mContext);
						try {
							final Dao<CachedFile, Integer> cachedFileAccess = handler.getAccessObject(CachedFile.class);
							
							final PreparedQuery<CachedFile> preparedQuery =
									cachedFileAccess.queryBuilder()
										.where()
										.eq("libraryId", library.getId())
										.and()
										.eq("uniqueKey", uniqueKey).prepare();
							
							final CachedFile cachedFile = cachedFileAccess.queryForFirst(preparedQuery);
							
							if (cachedFile == null) return null;
							
							final java.io.File file = new java.io.File(cachedFile.getFileName());
							if (file.exists())
								return BitmapFactory.decodeFile(cachedFile.getFileName());
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						finally {
							handler.close();
						}
						
						return null;
					}
				});
				
				getCachedImageTask.addOnCompleteListener(onGetImageComplete);
				
				getCachedImageTask.executeOnExecutor(mImageAccessExecutor, null);
			}
		});
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
	
	public void getImage(final OnCompleteListener<Void, Void, Bitmap> onGetBitmapComplete) {
		mGetImageTask.executeOnExecutor(mImageAccessExecutor);
	}
}
