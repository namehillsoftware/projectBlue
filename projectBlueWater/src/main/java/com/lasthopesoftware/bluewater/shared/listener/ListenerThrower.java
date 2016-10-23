package com.lasthopesoftware.bluewater.shared.listener;

import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Created by david on 3/8/15.
 */
public class ListenerThrower {

    /**
     * Will runWith all listeners once and only once, can handle listeners removed while listeners are being
     * ran. Not thread-safe.
     * @param listeners The listeners to loop through
     * @param callback The action to perform when a listener is found
     * @param <T> The type of the listeners
     */
    public static <T> void throwListeners(Collection<T> listeners, OneParameterAction<T> callback) {
        final HashSet<T> ranListeners = new HashSet<>();

        // Track the state of the iterator
        int listenersSize = listeners.size();
        Iterator<T> iterator = listeners.iterator();

        while (iterator.hasNext()) {
            final T listener = iterator.next();
            if (ranListeners.contains(listener)) continue;

            callback.runWith(listener);
            ranListeners.add(listener);

            if (listeners.size() == listenersSize) continue;

            // reset the size and iterator
            listenersSize = listeners.size();
            iterator = listeners.iterator();
        }
    }
}
