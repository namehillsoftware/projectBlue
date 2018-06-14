package com.lasthopesoftware.storage.directories;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import com.annimon.stream.Stream;
import com.namehillsoftware.handoff.promises.Promise;

import java.io.File;

public class PublicDirectoryLookup implements GetPublicDirectories {

	private final Context context;

	public PublicDirectoryLookup(Context context) {
		this.context = context;
	}

	@Override
	public Promise<Stream<File>> promisePublicDrives() {
		final Stream<File> publicDirectory = Stream.of(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC));
		return Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
			? new Promise<>(publicDirectory)
			: new Promise<>(Stream.concat(
				publicDirectory,
				Stream.of(context.getExternalMediaDirs())));
	}
}
