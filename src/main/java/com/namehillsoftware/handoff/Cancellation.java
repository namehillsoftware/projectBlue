package com.namehillsoftware.handoff;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

class Cancellation {
    private static final Runnable emptyRunnable = () -> {};

    private final AtomicReference<Runnable> reaction = new AtomicReference<>();
    private final AtomicBoolean isCancelled = new AtomicBoolean();

    public void cancel() {
        final Runnable reactionSnapshot = reaction.getAndSet(emptyRunnable);

        if (reactionSnapshot != null && reactionSnapshot != emptyRunnable && !isCancelled.getAndSet(true))
            reactionSnapshot.run();
    }

    public final void respondToCancellation(Runnable reaction) {
        this.reaction.compareAndSet(null, reaction);
    }

    public final void clearCancellation() {
        this.reaction.set(emptyRunnable);
    }
}
