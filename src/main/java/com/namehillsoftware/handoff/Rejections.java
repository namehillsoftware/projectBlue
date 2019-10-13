package com.namehillsoftware.handoff;

import java.util.Objects;

public class Rejections {

    public interface ReceiveUnhandledRejections {
        void newUnhandledRejection(Throwable rejection);
    }

    private static final ReceiveUnhandledRejections defaultReject = rejection -> {};

    private static volatile ReceiveUnhandledRejections unhandledRejections = defaultReject;

    public static void setUnhandledRejectionsReceiver(ReceiveUnhandledRejections unhandledRejections) {
        Objects.requireNonNull(unhandledRejections);
        Rejections.unhandledRejections = unhandledRejections;
    }

    public static void useDefaultUnhandledRejectionsReceiver() {
        setUnhandledRejectionsReceiver(defaultReject);
    }

    static ReceiveUnhandledRejections getUnhandledRejectionsHandler() {
        return unhandledRejections;
    }
}
