package com.lasthopesoftware.storage.read.permissions;

import com.lasthopesoftware.storage.RecursiveFileAssertionTester;

import java.io.File;

/**
 * Created by david on 7/17/16.
 */
public class FileReadPossibleArbitrator implements IFileReadPossibleArbitrator {

	@Override
	public boolean isFileReadPossible(File file) {
		return RecursiveFileAssertionTester.recursivelyTestAssertion(file, File::canRead);
	}
}
