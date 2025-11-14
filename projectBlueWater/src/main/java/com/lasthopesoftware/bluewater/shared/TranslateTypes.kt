package com.lasthopesoftware.bluewater.shared

import com.namehillsoftware.handoff.promises.Promise

interface TranslateTypes<From, To> {
	fun promiseTranslation(from: From): Promise<To>
}
