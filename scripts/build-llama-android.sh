#!/bin/sh
# Builds llama.cpp's llama-server as a static arm64 Android-compatible binary (musl, no NDK needed)
# and installs it as apkbuild/app/lib/arm64-v8a/libllamaserver.so (apps may only exec files in their native lib dir).
# Needs: pip install ziglang cmake ninja --break-system-packages ; git
set -e
W=${W:-/tmp/llama-android}; mkdir -p $W/zbin
for t in cc c++; do n=$([ $t = cc ] && echo zcc || echo zcxx); printf '#!/bin/sh\nexec python3 -m ziglang %s -target aarch64-linux-musl -mcpu=generic+v8_2a+dotprod+fullfp16 "$@"\n' $t > $W/zbin/$n; done
for t in ar ranlib; do printf '#!/bin/sh\nexec python3 -m ziglang %s "$@"\n' $t > $W/zbin/z$t; done; chmod +x $W/zbin/*
[ -d $W/llama.cpp ] || git clone --depth 1 https://github.com/ggml-org/llama.cpp.git $W/llama.cpp
cd $W/llama.cpp
cmake -S . -B build-and -G Ninja -DCMAKE_BUILD_TYPE=Release -DCMAKE_SYSTEM_NAME=Linux -DCMAKE_SYSTEM_PROCESSOR=aarch64 \
 -DCMAKE_C_COMPILER=$W/zbin/zcc -DCMAKE_CXX_COMPILER=$W/zbin/zcxx -DCMAKE_AR=$W/zbin/zar -DCMAKE_RANLIB=$W/zbin/zranlib \
 -DBUILD_SHARED_LIBS=OFF -DGGML_NATIVE=OFF -DGGML_OPENMP=OFF -DLLAMA_OPENSSL=OFF -DLLAMA_BUILD_TESTS=OFF -DLLAMA_BUILD_EXAMPLES=OFF \
 -DLLAMA_BUILD_APP=OFF -DLLAMA_BUILD_UI=OFF -DLLAMA_USE_PREBUILT_UI=OFF -DLLAMA_SUBPROCESS=OFF -DCMAKE_EXE_LINKER_FLAGS="-static"
ninja -C build-and llama-server || true   # zig's linker crashes on the --dependency-file flag; relink without it:
cd build-and && ninja -t commands bin/llama-server | tail -1 | sed 's| -Xlinker --dependency-file=[^ ]*||; s|-static |-static -s |' > relink.sh && sh relink.sh
cp bin/llama-server /home/claude/apkbuild/app/lib/arm64-v8a/libllamaserver.so && git -C .. rev-parse HEAD > /home/claude/apkbuild/native/llama.cpp-commit.txt
echo "built $(ls -la bin/llama-server)"
