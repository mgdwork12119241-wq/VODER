cmake_minimum_required(VERSION 3.22.1)
project(voder_voice_clone)

add_library(voder_voice_clone SHARED native-lib.cpp)

target_compile_features(voder_voice_clone PRIVATE cxx_std_17)
target_compile_options(voder_voice_clone PRIVATE -O3 -fvisibility=hidden)

find_library(log-lib log)
target_link_libraries(voder_voice_clone ${log-lib})
