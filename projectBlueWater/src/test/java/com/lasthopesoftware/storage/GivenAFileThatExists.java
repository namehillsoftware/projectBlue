package com.lasthopesoftware.storage;

import com.vedsoft.futures.callables.OneParameterFunction;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Created by david on 7/17/16.
 */
public class GivenAFileThatExists {
	@RunWith(MockitoJUnitRunner.class)
	public static class WhenTestingTheAssertion {
		private File mockFile;

		@Mock
		private OneParameterFunction<File, Boolean> mockFileAssertionTester;

		@Before
		public void setUp() {
			mockFile = mock(File.class);
			when(mockFile.exists()).thenReturn(true);

			when(mockFileAssertionTester.resultFrom(any())).thenReturn(true);

			RecursiveFileAssertionTester.recursivelyTestAssertion(mockFile, mockFileAssertionTester);
		}

		@Test
		public void ThenTheAssertionIsTestedOnThatFile() {
			verify(mockFileAssertionTester, times(1)).resultFrom(mockFile);
		}
	}
}
