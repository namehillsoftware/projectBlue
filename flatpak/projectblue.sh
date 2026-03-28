#!/bin/sh
#export JAVA_HOME=/app/share/java_home
export ATL_UGLY_ENABLE_WEBVIEW=
exec android-translation-layer /app/share/project-blue.apk $@
