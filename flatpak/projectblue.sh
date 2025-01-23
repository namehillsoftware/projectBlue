#!/bin/sh
export JAVA_HOME=/app/share/java_home
exec android-translation-layer --gapplication-app-id=net.newpipe.NewPipe /app/share/project-blue.apk $@
