package com.namehillsoftware.handoff.promises.response;

import com.namehillsoftware.handoff.promises.Promise;

public interface EventualAction {
    Promise<?> respond();
}
