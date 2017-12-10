package com.lasthopesoftware.bluewater.client.library.items.media.files.cached.disk;

import android.content.Context;
import android.os.Environment;

import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.configuration.IDiskFileCacheConfiguration;

import java.io.File;

public class AndroidDiskCacheDirectoryProvider implements IDiskCacheDirectoryProvider {
	private final Context context;

	public AndroidDiskCacheDirectoryProvider(Context context) {
		this.context = context;
	}

	@Override
	public File getDiskCacheDirectory(IDiskFileCacheConfiguration diskFileCacheConfiguration) {
		final File cacheDir = new File(getDiskCacheDir(context, diskFileCacheConfiguration.getCacheName()), String.valueOf(diskFileCacheConfiguration.getLibrary().getId()));
		return cacheDir.exists() || cacheDir.mkdirs()
			? cacheDir
			: null;
	}

	private static java.io.File getDiskCacheDir(final Context context, final String uniqueName) {
		// Check if media is mounted or storage is built-in, if so, try and use external cache dir
		// otherwise use internal cache dir
		final File cacheDir =
			Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ?
				context.getExternalCacheDir() :
				context.getCacheDir();

		return new File(cacheDir, uniqueName);
	}
}
