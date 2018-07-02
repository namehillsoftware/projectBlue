package com.lasthopesoftware.storage.directories;

import com.annimon.stream.Stream;
import com.namehillsoftware.handoff.promises.Promise;

import java.io.File;

public interface GetPublicDirectories {
	Promise<Stream<File>> promisePublicDrives();
}
