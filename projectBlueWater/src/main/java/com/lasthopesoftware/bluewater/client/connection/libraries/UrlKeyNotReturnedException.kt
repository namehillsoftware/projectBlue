package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.exceptions.DataNotReturnedException

class UrlKeyNotReturnedException(libraryId: LibraryId, key: Any?) : DataNotReturnedException("A UrlKeyHolder could not be provided for $libraryId and $key.")
