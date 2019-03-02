package com.lasthopesoftware.storage.directories;

import android.content.Context;
import android.os.Environment;
import com.annimon.stream.Stream;
import com.namehillsoftware.handoff.promises.Promise;

import java.io.File;

public class PrivateDirectoryLookup implements GetPrivateDirectories {
	private final Context context;

	public PrivateDirectoryLookup(Context context) {
		this.context = context;
	}

	@Override
	public Promise<Stream<File>> promisePrivateDrives() {
		return new Promise<>(Stream.of(context.getExternalFilesDirs(Environment.DIRECTORY_MUSIC)));
	}
}
