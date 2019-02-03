package com.namehillsoftware.handoff;

public class Rejections {

    public interface ReceiveUnhandledRejections {
        void newUnhandledRejection(Throwable rejection);
    }

    private static volatile ReceiveUnhandledRejections unhandledRejections;

    public static void setUnhandledRejectionsReceiver(ReceiveUnhandledRejections unhandledRejections) {
        Rejections.unhandledRejections = unhandledRejections;
    }

    static ReceiveUnhandledRejections getUnhandledRejectionsHandler() {
        return unhandledRejections;
    }
}
