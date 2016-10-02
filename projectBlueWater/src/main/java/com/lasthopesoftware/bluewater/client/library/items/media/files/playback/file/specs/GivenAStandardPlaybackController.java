package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.specs;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

/**
 * Created by david on 9/19/16.
 */
public class GivenAStandardPlaybackController {

    public class WhenCallingPauseWithAPlaybackFileController {

        private IPlaybackHandler mockPlaybackController;

        @Before
        public void before() {
            mockPlaybackController = mock(IPlaybackHandler.class);
        }

        @Test
        public void thenPauseIsCalledOnThePlaybackController() {
            verify(mockPlaybackController).pause();
        }
    }
}
