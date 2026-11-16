#!/bin/sh
# دانلود موتور تونل (hev-socks5-tunnel) به همراه وابستگی‌هایش داخل پروژه
set -e
git clone --recursive https://github.com/heiher/hev-socks5-tunnel app/src/main/jni/hev-socks5-tunnel
echo "تمام. حالا پروژه را در Android Studio باز کنید (NDK r27 یا جدیدتر از SDK Manager نصب باشد)."
