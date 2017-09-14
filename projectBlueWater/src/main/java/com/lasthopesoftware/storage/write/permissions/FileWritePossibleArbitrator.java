package com.lasthopesoftware.storage.write.permissions;

import com.lasthopesoftware.storage.RecursiveFileAssertionTester;
import com.namehillsoftware.lazyj.Lazy;

import java.io.File;

public final class FileWritePossibleArbitrator implements IFileWritePossibleArbitrator {

	private static final Lazy<FileWritePossibleArbitrator> lazyInstance = new Lazy<>(FileWritePossibleArbitrator::new);

	private FileWritePossibleArbitrator() {}

	@Override
	public boolean isFileWritePossible(File file) {
		return RecursiveFileAssertionTester.recursivelyTestAssertion(file, File::canWrite);
	}

	public static FileWritePossibleArbitrator getInstance() {
		return lazyInstance.getObject();
	}
}
