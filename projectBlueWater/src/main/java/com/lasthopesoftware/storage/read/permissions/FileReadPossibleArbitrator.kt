package com.lasthopesoftware.storage.read.permissions;

import com.lasthopesoftware.storage.RecursiveFileAssertionTester;

import java.io.File;

public final class FileReadPossibleArbitrator implements IFileReadPossibleArbitrator {

	@Override
	public boolean isFileReadPossible(File file) {
		return RecursiveFileAssertionTester.recursivelyTestAssertion(file, File::canRead);
	}
}
