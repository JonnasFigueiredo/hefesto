package com.hefesto.llm.process;

import java.time.Duration;

/**
 * Abstração sobre {@link ProcessBuilder} para permitir mocks em testes.
 * Adapters CLI dependem desta interface, não de Process diretamente.
 */
public interface ProcessLauncher {

    /**
     * Executa um comando, opcionalmente com input no stdin, e retorna o resultado.
     * Bloqueia até o processo terminar ou o timeout estourar.
     *
     * @param stdinInput texto a enviar pra stdin do processo (pode ser null).
     * @param timeout    duração máxima de espera; se null, default de 60s.
     * @param command    binário e argumentos.
     * @return resultado capturado (exit code, stdout, stderr).
     * @throws ProcessLaunchException se houver erro de I/O ou o processo não
     *         terminar dentro do timeout.
     */
    ProcessResult run(String stdinInput, Duration timeout, String... command);

    /**
     * Atalho para execução rápida sem stdin e com timeout default.
     */
    default ProcessResult run(String... command) {
        return run(null, null, command);
    }
}
