package com.namehillsoftware.handoff.errors;

public interface ReceiveUnhandledRejections {
    void newUnhandledRejection(Throwable rejection);
}
