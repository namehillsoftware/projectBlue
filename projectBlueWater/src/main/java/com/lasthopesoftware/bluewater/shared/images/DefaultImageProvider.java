package com.lasthopesoftware.bluewater.shared.images;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import com.lasthopesoftware.bluewater.R;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DefaultImageProvider {

	private static final ExecutorService defaultImageAccessExecutor = Executors.newSingleThreadExecutor();
	private static final Object fillerBitmapSyncObj = new Object();

	private static Bitmap fillerBitmap;

	private final Context context;

	public DefaultImageProvider(Context context) {
		this.context = context;
	}

	public Promise<Bitmap> promiseFileBitmap() {
		return fillerBitmap != null
			? new Promise<>(getBitmapCopy(fillerBitmap))
			: new QueuedPromise<>(this::getFillerBitmap, defaultImageAccessExecutor);
	}

	private Bitmap getFillerBitmap() {
		if (fillerBitmap != null) return getBitmapCopy(fillerBitmap);

		fillerBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.wave_background);

		final DisplayMetrics dm = context.getResources().getDisplayMetrics();
		int maxSize = Math.max(dm.heightPixels, dm.widthPixels);

		fillerBitmap = Bitmap.createScaledBitmap(fillerBitmap, maxSize, maxSize, false);

		return getBitmapCopy(fillerBitmap);
	}

	private static Bitmap getBitmapCopy(final Bitmap src) {
		return src.copy(src.getConfig(), false);
	}
}
