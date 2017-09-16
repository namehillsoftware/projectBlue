package com.lasthopesoftware.storage.write.permissions.specs;

import com.lasthopesoftware.storage.write.permissions.FileWritePossibleArbitrator;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by david on 7/17/16.
 */
public class GivenAFileThatCannotBeWritten {
	public static class AndANullParentFile {
		public static class WhenCheckingForPermissions {

			private boolean fileWriteIsPossible;

			@Before
			public void before() {
				final FileWritePossibleArbitrator fileWritePossibleArbitrator = new FileWritePossibleArbitrator();
				final File file = mock(File.class);
				when(file.canWrite()).thenReturn(false);
				when(file.getParentFile()).thenReturn(null);

				fileWriteIsPossible = fileWritePossibleArbitrator.isFileWritePossible(file);
			}

			@Test
			public void thenFileWriteIsNotPossible() {
				Assert.assertFalse(this.fileWriteIsPossible);
			}
		}
	}

	public static class AndAParentFileExists {
		public static class AndCanBeWritten {
			public static class WhenCheckingForPermissions {

				private boolean fileWriteIsPossible;

				@Before
				public void before() {
					final FileWritePossibleArbitrator fileWritePossibleArbitrator = new FileWritePossibleArbitrator();
					final File file = mock(File.class);
					when(file.canWrite()).thenReturn(false);

					final File parentFile = mock(File.class);
					when(file.getParentFile()).thenReturn(parentFile);

					when(parentFile.canWrite()).thenReturn(true);
					when(parentFile.getParentFile()).thenReturn(null);
					when(parentFile.exists()).thenReturn(true);

					fileWriteIsPossible = fileWritePossibleArbitrator.isFileWritePossible(file);
				}

				@Test
				public void thenFileWriteIsPossible() {
					Assert.assertTrue(this.fileWriteIsPossible);
				}
			}
		}

		public static class AndCannotBeWritten {
			public static class WhenCheckingForPermissions {

				private boolean fileWriteIsPossible;

				@Before
				public void before() {
					final FileWritePossibleArbitrator fileWritePossibleArbitrator = new FileWritePossibleArbitrator();
					final File file = mock(File.class);
					when(file.canWrite()).thenReturn(false);
					when(file.getParentFile()).thenReturn(null);

					fileWriteIsPossible = fileWritePossibleArbitrator.isFileWritePossible(file);
				}

				@Test
				public void thenFileWriteIsNotPossible() {
					Assert.assertFalse(this.fileWriteIsPossible);
				}
			}
		}
	}


}
