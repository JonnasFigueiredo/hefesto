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
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementação padrão de {@link ProcessLauncher} usando {@link ProcessBuilder}.
 * Coleta stdout e stderr em paralelo para evitar deadlock em buffers cheios.
 */
@Component
public class DefaultProcessLauncher implements ProcessLauncher {

    private static final Logger log = LoggerFactory.getLogger(DefaultProcessLauncher.class);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    @Override
    public ProcessResult run(String stdinInput, Duration timeout, String... command) {
        if (command == null || command.length == 0) {
            throw new ProcessLaunchException("command must not be empty");
        }
        Duration effectiveTimeout = timeout != null ? timeout : DEFAULT_TIMEOUT;

        // Windows: .cmd/.bat precisam ser executados via cmd.exe /c por causa
        // das restrições do JDK 17+ contra argument injection (CVE-2024-...).
        String[] effectiveCommand = adaptForWindows(command);

        log.debug("Spawning process: {} (timeout={}s, hasStdin={})",
            String.join(" ", effectiveCommand), effectiveTimeout.toSeconds(), stdinInput != null);

        ProcessBuilder pb = new ProcessBuilder(effectiveCommand);
        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new ProcessLaunchException(
                "Failed to launch process: " + command[0] + " (" + e.getMessage() + ")", e);
        }

        ExecutorService io = Executors.newFixedThreadPool(2);
        try {
            Future<String> stdoutF = io.submit(() -> readFully(process.getInputStream()));
            Future<String> stderrF = io.submit(() -> readFully(process.getErrorStream()));

            if (stdinInput != null) {
                try (OutputStream os = process.getOutputStream()) {
                    os.write(stdinInput.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                } catch (IOException e) {
                    process.destroyForcibly();
                    throw new ProcessLaunchException("Failed to write to stdin", e);
                }
            }

            boolean finished = process.waitFor(effectiveTimeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new ProcessLaunchException(
                    "Process timed out after " + effectiveTimeout.toSeconds() + "s: " + command[0]);
            }

            String stdout = stdoutF.get(2, TimeUnit.SECONDS);
            String stderr = stderrF.get(2, TimeUnit.SECONDS);
            int exitCode = process.exitValue();

            log.debug("Process exited with code {} ({} bytes stdout, {} bytes stderr)",
                exitCode, stdout.length(), stderr.length());

            return new ProcessResult(exitCode, stdout, stderr);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new ProcessLaunchException("Interrupted while waiting for process", e);
        } catch (TimeoutException | java.util.concurrent.ExecutionException e) {
            process.destroyForcibly();
            throw new ProcessLaunchException("Failed reading process output", e);
        } finally {
            io.shutdownNow();
        }
    }

    /**
     * No Windows, envolve invocações de .cmd/.bat com {@code cmd.exe /c} para
     * evitar problemas de quoting do ProcessBuilder e restrições de segurança
     * do JDK 17+. Em outros sistemas, retorna o comando inalterado.
     */
    private static String[] adaptForWindows(String[] command) {
        if (!isWindows()) return command;
        String first = command[0].toLowerCase(java.util.Locale.ROOT);
        if (!first.endsWith(".cmd") && !first.endsWith(".bat")) return command;

        String[] adapted = new String[command.length + 2];
        adapted[0] = "cmd.exe";
        adapted[1] = "/c";
        System.arraycopy(command, 0, adapted, 2, command.length);
        return adapted;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");
    }

    private static String readFully(java.io.InputStream in) throws IOException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int read;
            while ((read = br.read(buf)) != -1) {
                sb.append(buf, 0, read);
            }
            return sb.toString();
        }
    }
}
