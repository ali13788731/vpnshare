#!/bin/sh
# Downloads the tunnel engine (hev-socks5-tunnel, pinned release) into the project
set -e
rm -rf app/src/main/jni/hev-socks5-tunnel
git clone --branch 2.20.0 --depth 1 --recursive https://github.com/heiher/hev-socks5-tunnel app/src/main/jni/hev-socks5-tunnel
echo "Done. Open the project in Android Studio (NDK r27+ required)."
