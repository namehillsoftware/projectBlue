## Expected Errors and Handling

- File exists, do not have permissions to read - return error, end immediately
- Do not have permissions to write - return error, end immediately
- Do not have permissions to make directory - return error, end immediately
- IO Exception occurs during download - log error, continue download
- Generic exception occurs during download - return error, end immediately