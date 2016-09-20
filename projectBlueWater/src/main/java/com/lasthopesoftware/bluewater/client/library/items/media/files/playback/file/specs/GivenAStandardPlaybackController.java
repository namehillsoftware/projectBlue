package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.specs;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackController;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

/**
 * Created by david on 9/19/16.
 */
public class GivenAStandardPlaybackController {

    public class WhenCallingPauseWithAPlaybackFileController {

        private IPlaybackController mockPlaybackController;

        @Before
        public void before() {
            mockPlaybackController = mock(IPlaybackController.class);
        }

        @Test
        public void thenPauseIsCalledOnThePlaybackController() {
            verify(mockPlaybackController).pause();
        }
    }
}
