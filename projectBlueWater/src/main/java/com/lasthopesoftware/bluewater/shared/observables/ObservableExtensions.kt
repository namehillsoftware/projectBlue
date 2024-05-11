package com.lasthopesoftware.bluewater.shared.observables

import io.reactivex.rxjava3.disposables.Disposable

fun Disposable.toCloseable() = AutoCloseable { dispose() }
