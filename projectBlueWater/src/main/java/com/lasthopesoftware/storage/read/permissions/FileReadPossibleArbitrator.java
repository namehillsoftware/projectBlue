package com.lasthopesoftware.storage.read.permissions;

import com.lasthopesoftware.storage.RecursiveFileAssertionTester;
import com.namehillsoftware.lazyj.Lazy;

import java.io.File;

public final class FileReadPossibleArbitrator implements IFileReadPossibleArbitrator {

	private static final Lazy<FileReadPossibleArbitrator> lazyInstance = new Lazy<>(FileReadPossibleArbitrator::new);

	private FileReadPossibleArbitrator() {}

	@Override
	public boolean isFileReadPossible(File file) {
		return RecursiveFileAssertionTester.recursivelyTestAssertion(file, File::canRead);
	}

	public static FileReadPossibleArbitrator getInstance() {
		return lazyInstance.getObject();
	}
}
