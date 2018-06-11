package com.lasthopesoftware.storage.directories;

import com.namehillsoftware.handoff.promises.Promise;

import java.io.File;
import java.util.Collection;

public interface GetPrivateDrives {
	Promise<Collection<File>> promisePrivateDrives();
}
