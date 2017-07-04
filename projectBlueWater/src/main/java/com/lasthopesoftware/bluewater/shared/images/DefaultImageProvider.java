package com.lasthopesoftware.bluewater.shared.images;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.queued.QueuedPromise;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DefaultImageProvider {

	private static final ExecutorService defaultImageAccessExecutor = Executors.newSingleThreadExecutor();

	private static Bitmap fillerBitmap;
	private static final Object fillerBitmapSyncObj = new Object();

	private Context context;

	public DefaultImageProvider(Context context) {
		this.context = context;
	}

	public Promise<Bitmap> promiseFileBitmap() {
		return new QueuedPromise<>(this::getFillerBitmap, defaultImageAccessExecutor);
	}

	private Bitmap getFillerBitmap() {
		synchronized (fillerBitmapSyncObj) {
			if (fillerBitmap != null) return getBitmapCopy(fillerBitmap);

			fillerBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.wave_background);

			final DisplayMetrics dm = context.getResources().getDisplayMetrics();
			int maxSize = Math.max(dm.heightPixels, dm.widthPixels);

			fillerBitmap = Bitmap.createScaledBitmap(fillerBitmap, maxSize, maxSize, false);

			return getBitmapCopy(fillerBitmap);
		}
	}

	private static Bitmap getBitmapCopy(final Bitmap src) {
		return src.copy(src.getConfig(), false);
	}
}
