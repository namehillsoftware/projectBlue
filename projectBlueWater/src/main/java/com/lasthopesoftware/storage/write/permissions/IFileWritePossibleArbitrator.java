package com.lasthopesoftware.storage.write.permissions;

import java.io.File;

/**
 * Created by david on 7/17/16.
 */
public interface IFileWritePossibleArbitrator {
	boolean isFileWritePossible(File file);
}
