## Expected Errors and Handling

- File exists, do not have permissions to read - goes into `Unreadable` state, continue download, because it shouldn't prevent other files from downloading
- Do not have permissions to write - return error for that library, stop downloading for that library, because it is likely other files in that library cannot be written
- Do not have permissions to make directory - return error, end immediately
- IO Exception occurs during download - log error, continue download, because it was likely just a network issue
- Generic exception occurs during download - return error, end immediately