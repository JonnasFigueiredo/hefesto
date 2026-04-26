package com.hefesto.llm.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper sobre {@link Process} para uso em streaming. Permite ler stdout
 * linha a linha enquanto o processo ainda está rodando, coletar stderr em
 * background, e abortar/aguardar conclusão.
 *
 * <p>Não é thread-safe — uma única thread deve consumir {@link #readLine()}
 * em loop até retornar null.</p>
 */
public class StreamingProcess implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(StreamingProcess.class);

    private final Process process;
    private final BufferedReader stdoutReader;
    private final StringBuilder stderrBuffer = new StringBuilder();
    private final ExecutorService stderrPool;
    private final Future<?> stderrFuture;

    public StreamingProcess(Process process, String stdinInput) throws IOException {
        this.process = process;
        this.stdoutReader = new BufferedReader(
            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

        this.stderrPool = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "streaming-stderr-collector");
            t.setDaemon(true);
            return t;
        });
        this.stderrFuture = stderrPool.submit(this::collectStderr);

        if (stdinInput != null) {
            try (OutputStream os = process.getOutputStream()) {
                os.write(stdinInput.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
        }
    }

    /**
     * Lê a próxima linha de stdout. Bloqueia até linha disponível, EOF ou
     * processo morrer. Retorna {@code null} no fim do stream.
     */
    public String readLine() throws IOException {
        return stdoutReader.readLine();
    }

    /**
     * Mata o processo de forma forçada. Usado pra cancelamento.
     */
    public void abort() {
        if (process.isAlive()) {
            log.debug("Aborting streaming process pid={}", process.pid());
            process.destroyForcibly();
        }
    }

    /**
     * Aguarda o processo terminar e retorna o resultado final (com stderr
     * coletado e exit code). Stdout neste ponto deve ter sido totalmente
     * lido pelo chamador via {@link #readLine()}.
     */
    public ProcessResult awaitCompletion(Duration timeout) {
        try {
            boolean ok = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!ok) {
                log.warn("Streaming process timed out after {}s, force-killing", timeout.toSeconds());
                process.destroyForcibly();
            }
            stderrFuture.get(2, TimeUnit.SECONDS);
            int exit = process.isAlive() ? -1 : process.exitValue();
            return new ProcessResult(exit, "", stderrBuffer.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return new ProcessResult(-1, "", "interrupted");
        } catch (Exception e) {
            return new ProcessResult(-1, "", "error awaiting completion: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            stdoutReader.close();
        } catch (IOException ignored) {
        }
        if (process.isAlive()) {
            process.destroyForcibly();
        }
        stderrPool.shutdownNow();
    }

    private void collectStderr() {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            char[] buf = new char[2048];
            int read;
            while ((read = br.read(buf)) != -1) {
                synchronized (stderrBuffer) {
                    stderrBuffer.append(buf, 0, read);
                }
            }
        } catch (IOException e) {
            log.debug("Error reading stderr: {}", e.getMessage());
        }
    }
}
