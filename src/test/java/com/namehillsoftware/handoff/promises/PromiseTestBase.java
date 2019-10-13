package com.namehillsoftware.handoff.promises;

import com.namehillsoftware.handoff.Rejections;
import org.junit.BeforeClass;

public abstract class PromiseTestBase {

    @BeforeClass
    public static void setupEnvironment() {
        Rejections.useDefaultUnhandledRejectionsReceiver();
    }
}
