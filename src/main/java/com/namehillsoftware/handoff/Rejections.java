package com.namehillsoftware.handoff;

public class Rejections {

    public interface ReceiveUnhandledRejections {
        void newUnhandledRejection(Throwable rejection);
    }

    private static final ReceiveUnhandledRejections defaultReceiver = rejection -> {};

    private static volatile ReceiveUnhandledRejections receiver = defaultReceiver;

    public static void setUnhandledRejectionsReceiver(ReceiveUnhandledRejections receiver) {
        Rejections.receiver = receiver;
    }

    static ReceiveUnhandledRejections getUnhandledRejectionsHandler() {
        return receiver;
    }
}
