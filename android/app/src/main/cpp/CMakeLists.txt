cmake_minimum_required(VERSION 3.22)
project(voder_voice_clone LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

set(QWEN3_TTS_ROOT "${CMAKE_CURRENT_LIST_DIR}/../../../../external/qwen3-tts.cpp")
set(GGML_DIR "${QWEN3_TTS_ROOT}/ggml")

set(GGML_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(GGML_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(GGML_BACKEND_DL OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_VULKAN OFF CACHE BOOL "" FORCE)
set(GGML_OPENCL OFF CACHE BOOL "" FORCE)

add_subdirectory("${GGML_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/ggml")
find_package(Threads REQUIRED)

function(voder_link_ggml target_name)
    target_link_libraries(${target_name} PUBLIC ggml ggml-base ggml-cpu Threads::Threads)
endfunction()

add_library(text_tokenizer STATIC
    "${QWEN3_TTS_ROOT}/src/text_tokenizer.cpp"
    "${QWEN3_TTS_ROOT}/src/gguf_loader.cpp"
    "${QWEN3_TTS_ROOT}/src/tokenizer_unicode.cpp"
    "${QWEN3_TTS_ROOT}/src/tokenizer_unicode_data.cpp"
)
target_include_directories(text_tokenizer PUBLIC "${QWEN3_TTS_ROOT}/src" "${GGML_DIR}/include")
voder_link_ggml(text_tokenizer)

add_library(tts_transformer STATIC
    "${QWEN3_TTS_ROOT}/src/gguf_loader.cpp"
    "${QWEN3_TTS_ROOT}/src/tts_transformer.cpp"
    "${QWEN3_TTS_ROOT}/src/transformer/transformer_cache.cpp"
    "${QWEN3_TTS_ROOT}/src/transformer/transformer_debug.cpp"
    "${QWEN3_TTS_ROOT}/src/transformer/transformer_embeddings.cpp"
    "${QWEN3_TTS_ROOT}/src/transformer/transformer_graph_code_pred.cpp"
    "${QWEN3_TTS_ROOT}/src/transformer/transformer_graph_talker.cpp"
    "${QWEN3_TTS_ROOT}/src/transformer/transformer_loader.cpp"
    "${QWEN3_TTS_ROOT}/src/transformer/transformer_loader_metadata.cpp"
    "${QWEN3_TTS_ROOT}/src/transformer/transformer_loader_tensors.cpp"
    "${QWEN3_TTS_ROOT}/src/transformer/transformer_runtime.cpp"
    "${QWEN3_TTS_ROOT}/src/transformer/transformer_runtime_code_pred.cpp"
    "${QWEN3_TTS_ROOT}/src/transformer/transformer_generate.cpp"
    "${QWEN3_TTS_ROOT}/src/coreml_code_predictor_stub.cpp"
)
target_include_directories(tts_transformer PUBLIC "${QWEN3_TTS_ROOT}/src" "${GGML_DIR}/include")
voder_link_ggml(tts_transformer)

add_library(audio_tokenizer_encoder STATIC
    "${QWEN3_TTS_ROOT}/src/audio_tokenizer_encoder.cpp"
    "${QWEN3_TTS_ROOT}/src/encoder/encoder_frontend.cpp"
    "${QWEN3_TTS_ROOT}/src/encoder/encoder_graph.cpp"
    "${QWEN3_TTS_ROOT}/src/encoder/encoder_loader.cpp"
    "${QWEN3_TTS_ROOT}/src/encoder/encoder_runtime.cpp"
    "${QWEN3_TTS_ROOT}/src/gguf_loader.cpp"
)
target_include_directories(audio_tokenizer_encoder PUBLIC "${QWEN3_TTS_ROOT}/src" "${GGML_DIR}/include")
voder_link_ggml(audio_tokenizer_encoder)

add_library(speech_tokenizer_encoder STATIC
    "${QWEN3_TTS_ROOT}/src/speech_tokenizer_encoder.cpp"
    "${QWEN3_TTS_ROOT}/src/gguf_loader.cpp"
)
target_include_directories(speech_tokenizer_encoder PUBLIC "${QWEN3_TTS_ROOT}/src" "${GGML_DIR}/include")
voder_link_ggml(speech_tokenizer_encoder)

add_library(audio_tokenizer_decoder STATIC
    "${QWEN3_TTS_ROOT}/src/decoder/decoder_cache.cpp"
    "${QWEN3_TTS_ROOT}/src/decoder/decoder_graph.cpp"
    "${QWEN3_TTS_ROOT}/src/decoder/decoder_layers.cpp"
    "${QWEN3_TTS_ROOT}/src/decoder/decoder_loader.cpp"
    "${QWEN3_TTS_ROOT}/src/decoder/decoder_runtime.cpp"
    "${QWEN3_TTS_ROOT}/src/gguf_loader.cpp"
)
target_include_directories(audio_tokenizer_decoder PUBLIC "${QWEN3_TTS_ROOT}/src" "${GGML_DIR}/include")
voder_link_ggml(audio_tokenizer_decoder)

add_library(qwen3_tts STATIC
    "${QWEN3_TTS_ROOT}/src/common/audio_io.cpp"
    "${QWEN3_TTS_ROOT}/src/common/icl_prompt_io.cpp"
    "${QWEN3_TTS_ROOT}/src/common/speaker_embedding_io.cpp"
    "${QWEN3_TTS_ROOT}/src/pipeline/pipeline_models.cpp"
    "${QWEN3_TTS_ROOT}/src/pipeline/pipeline_runtime.cpp"
    "${QWEN3_TTS_ROOT}/src/pipeline/pipeline_synthesize.cpp"
    "${QWEN3_TTS_ROOT}/src/qwen3_tts.cpp"
    "${QWEN3_TTS_ROOT}/src/qwen3_tts_c.cpp"
)
target_include_directories(qwen3_tts PUBLIC "${QWEN3_TTS_ROOT}/src" "${GGML_DIR}/include")
target_link_libraries(qwen3_tts PUBLIC
    text_tokenizer
    tts_transformer
    audio_tokenizer_encoder
    speech_tokenizer_encoder
    audio_tokenizer_decoder
    Threads::Threads
)
target_compile_definitions(qwen3_tts PRIVATE QWEN3_TTS_EXPORT)

add_library(voder_voice_clone SHARED native-lib.cpp)
target_compile_features(voder_voice_clone PRIVATE cxx_std_17)
target_compile_options(voder_voice_clone PRIVATE -O3 -fvisibility=hidden)
target_include_directories(voder_voice_clone PRIVATE "${QWEN3_TTS_ROOT}/src")
target_link_libraries(voder_voice_clone PRIVATE qwen3_tts log)
