package com.lasthopesoftware.storage.write.exceptions

import java.io.File
import java.io.IOException

class StorageCreatePathException(file: File) :
    IOException("There was an error creating the path $file.")
