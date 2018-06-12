package com.lasthopesoftware.storage.directories;

import android.os.Environment;

import com.annimon.stream.Stream;
import com.namehillsoftware.handoff.promises.Promise;

import java.io.File;

public class PublicDriveLookup implements GetPublicDrives {
	@Override
	public Promise<Stream<File>> promisePublicDrives() {
		return new Promise<>(Stream
			.of(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)));
	}
}
