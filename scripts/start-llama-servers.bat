@echo off
REM ===========================================================================
REM  Sobe um llama-server (llama.cpp) por modelo .gguf de E:\LLM, cada um numa
REM  porta. As portas batem com hefesto.local-models no application.yml.
REM
REM  Pre-requisito: o binario llama-server.exe (do llama.cpp) em E:\LLM.
REM  Baixe em: https://github.com/ggml-org/llama.cpp/releases
REM    - CPU:    llama-<build>-bin-win-cpu-x64.zip
REM    - NVIDIA: llama-<build>-bin-win-cuda-x64.zip (muito mais rapido)
REM  Extraia o llama-server.exe (e DLLs) para E:\LLM.
REM
REM  -ngl 99  = offload de todas as camadas pra GPU (ignorado no build CPU).
REM  -c 8192  = janela de contexto. Ajuste conforme RAM/VRAM.
REM ===========================================================================

set LLM_DIR=E:\LLM
set SERVER=%LLM_DIR%\llama-server.exe

if not exist "%SERVER%" (
  echo [ERRO] Nao encontrei %SERVER%
  echo Baixe o llama-server do llama.cpp e coloque em %LLM_DIR%.
  pause
  exit /b 1
)

echo Subindo llama-3.1-8b na porta 8081...
start "llama-3.1-8b" "%SERVER%" -m "%LLM_DIR%\llama-3.1-8b-Q4_K_M.gguf" --alias llama-3.1-8b --host 127.0.0.1 --port 8081 -c 8192 -ngl 99

echo Subindo mistral-7b na porta 8082...
start "mistral-7b" "%SERVER%" -m "%LLM_DIR%\mistral-7b-v0.3-Q4_K_M.gguf" --alias mistral-7b --host 127.0.0.1 --port 8082 -c 8192 -ngl 99

echo Subindo qwen3-8b na porta 8083...
start "qwen3-8b" "%SERVER%" -m "%LLM_DIR%\qwen3-8b-Q4_K_M.gguf" --alias qwen3-8b --host 127.0.0.1 --port 8083 -c 8192 -ngl 99

echo.
echo Tres janelas de llama-server foram abertas (8081/8082/8083).
echo Cada uma carrega ~5GB; aguarde "server is listening" em cada janela.
echo Rodar os 3 ao mesmo tempo exige bastante RAM/VRAM. Se faltar memoria,
echo suba so o modelo que for usar (comente as linhas dos outros).
