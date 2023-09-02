package com.lasthopesoftware.bluewater

import com.lasthopesoftware.bluewater.repository.Entity

interface IdentifiableEntity : Entity {
	val id: Number
}
