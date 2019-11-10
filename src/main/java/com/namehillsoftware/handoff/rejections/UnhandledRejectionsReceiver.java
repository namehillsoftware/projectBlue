package com.namehillsoftware.handoff.rejections;

public interface UnhandledRejectionsReceiver {
    void newUnhandledRejection(Throwable rejection);
}
