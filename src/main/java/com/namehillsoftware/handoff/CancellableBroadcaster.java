package com.namehillsoftware.handoff;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

abstract class CancellableBroadcaster<Resolution> {
    private final AtomicReference<Runnable> reaction = new AtomicReference<>();
    private final AtomicBoolean isCancellationClosed = new AtomicBoolean();

    protected final void reject(Throwable error) {
        resolve(null, error);
    }

    protected final void resolve(Resolution resolution) {
        resolve(resolution, null);
    }

    private void resolve(Resolution resolution, Throwable rejection) {
        isCancellationClosed.set(true);
        this.reaction.set(null);
        resolve(new Message<>(resolution, rejection));
    }

    protected abstract void resolve(Message<Resolution> message);

    public final void cancel() {
        if (isCancellationClosed.getAndSet(true)) return;

        final Runnable reactionSnapshot = reaction.getAndSet(null);
        if (reactionSnapshot != null)
            reactionSnapshot.run();
    }

    public final void respondToCancellation(Runnable reaction) {
        if (!isCancellationClosed.get())
            this.reaction.set(reaction);
    }
}
