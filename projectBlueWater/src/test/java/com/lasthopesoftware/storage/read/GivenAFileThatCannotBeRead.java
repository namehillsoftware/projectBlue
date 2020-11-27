package com.lasthopesoftware.storage.read;

import com.lasthopesoftware.storage.read.permissions.FileReadPossibleArbitrator;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GivenAFileThatCannotBeRead {
	public static class WhenCheckingForPermissions {

		private boolean fileReadIsPossible;

		@Before
		public void before() {
			final FileReadPossibleArbitrator fileReadPossibleArbitrator = new FileReadPossibleArbitrator();
			final File file = mock(File.class);
			when(file.canRead()).thenReturn(false);
			when(file.getParentFile()).thenReturn(null);

			fileReadIsPossible = fileReadPossibleArbitrator.isFileReadPossible(file);
		}

		@Test
		public void thenFileReadIsNotPossible() {
			Assert.assertFalse(this.fileReadIsPossible);
		}
	}
}
