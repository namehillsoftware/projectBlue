FROM gradle:7.1.1-jdk11

WORKDIR /src

COPY . .

ENTRYPOINT [ "gradle" ]

# Usage:
#  Build Image: docker build . -t handoff-build
#  Build app: docker run --rm handoff-build build
#  Test app: docker run --rm handoff-build test
