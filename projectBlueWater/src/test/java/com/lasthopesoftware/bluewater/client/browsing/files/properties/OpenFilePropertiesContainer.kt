package com.lasthopesoftware.bluewater.client.browsing.files.properties

import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.ContainVersionedFileProperties

open class OpenFilePropertiesContainer(private val inner: ContainVersionedFileProperties) : ContainVersionedFileProperties by inner
