package com.hefesto.llm.process;

/**
 * Resultado da execução de um subprocesso síncrono.
 *
 * @param exitCode  código de saída do processo. 0 = sucesso por convenção.
 * @param stdout    saída padrão capturada como String (UTF-8).
 * @param stderr    saída de erro capturada como String (UTF-8).
 */
public record ProcessResult(
    int exitCode,
    String stdout,
    String stderr
) {

    public boolean success() {
        return exitCode == 0;
    }
}
