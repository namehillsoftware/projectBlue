FROM gradle:8.2.1-jdk17

# Install system dependencies
RUN apt-get update -qq && apt-get install -qq -y --no-install-recommends \
        apt-transport-https \
        curl \
        build-essential \
        file \
        git \
        gnupg2 \
    && rm -rf /var/lib/apt/lists/*;

# set default environment variables
ENV ADB_INSTALL_TIMEOUT=10
ENV ANDROID_HOME=/opt/android
ENV ANDROID_SDK_HOME=${ANDROID_HOME}

ENV PATH ${ANDROID_HOME}/cmdline-tools/cmdline-tools/bin:${ANDROID_HOME}/tools:${ANDROID_HOME}/tools/bin:${ANDROID_HOME}/platform-tools:${PATH}

# set default build arguments
ARG SDK_VERSION=commandlinetools-linux-10406996_latest.zip

RUN curl -sSL https://dl.google.com/android/repository/${SDK_VERSION} -o /tmp/sdk.zip \
    && mkdir ${ANDROID_HOME} \
    && unzip -q -d ${ANDROID_HOME}/cmdline-tools /tmp/sdk.zip \
    && rm /tmp/sdk.zip \
    && yes | sdkmanager --licenses

# Set these to the same versions as in build.gradle to avoid downloading updated tools
ARG ANDROID_BUILD_VERSION=34
ARG ANDROID_TOOLS_VERSION=33.0.1

# Full reference at https://dl.google.com/android/repository/repository2-1.xml
# download and unpack android
RUN yes | sdkmanager "platform-tools" \
#        "emulator" \ # keeping just in case it is needed
        "platforms;android-${ANDROID_BUILD_VERSION}" \
        "build-tools;${ANDROID_TOOLS_VERSION}" \
#        "add-ons;addon-google_apis-google-23" \ # keeping in case addons are needed
        "extras;android;m2repository"

WORKDIR /src

ENTRYPOINT [ "gradle", "-PdisablePreDex" ]

# Usage:
#  Build Image: docker build . -t android-build
#  Build app: docker run --rm android-build :projectBlueWater:assembleRelease
#  Test app: docker run --rm android-build test
