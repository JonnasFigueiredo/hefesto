package com.hefesto.llm.adapters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hefesto.llm.ChatRequest;
import com.hefesto.llm.ChatResponse;
import com.hefesto.llm.LlmAdapterException;
import com.hefesto.llm.process.ProcessLaunchException;
import com.hefesto.llm.process.ProcessLauncher;
import com.hefesto.llm.process.ProcessResult;

@ExtendWith(MockitoExtension.class)
class ClaudeCodeCliAdapterTest {

    @Mock
    ProcessLauncher launcher;

    ClaudeCodeCliAdapter adapter;

    @BeforeEach
    void setup() {
        adapter = new ClaudeCodeCliAdapter("claude", 60, launcher);
    }

    @Test
    void id_and_displayName_are_stable() {
        assertThat(adapter.id()).isEqualTo("claude-code");
        assertThat(adapter.displayName()).isEqualTo("Claude Code");
    }

    @Test
    void isAvailable_returnsTrue_whenVersionExitsZero() {
        when(launcher.run(eq(null), any(Duration.class), eq("claude"), eq("--version")))
            .thenReturn(new ProcessResult(0, "1.2.3\n", ""));

        assertThat(adapter.isAvailable()).isTrue();
    }

    @Test
    void isAvailable_returnsFalse_whenVersionExitsNonZero() {
        when(launcher.run(eq(null), any(Duration.class), eq("claude"), eq("--version")))
            .thenReturn(new ProcessResult(127, "", "command not found"));

        assertThat(adapter.isAvailable()).isFalse();
    }

    @Test
    void isAvailable_returnsFalse_whenLaunchThrows() {
        when(launcher.run(eq(null), any(Duration.class), eq("claude"), eq("--version")))
            .thenThrow(new ProcessLaunchException("binary missing"));

        assertThat(adapter.isAvailable()).isFalse();
    }

    @Test
    void chat_returnsResponse_onSuccess() {
        when(launcher.run(any(String.class), any(Duration.class), eq("claude"), eq("-p")))
            .thenReturn(new ProcessResult(0, "Olá, mundo!\n", ""));

        ChatRequest req = new ChatRequest("conv-1", List.of(), "Oi", null);
        ChatResponse res = adapter.chat(req);

        assertThat(res.content()).isEqualTo("Olá, mundo!");
        assertThat(res.adapterId()).isEqualTo("claude-code");
        assertThat(res.latencyMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void chat_throwsAdapterException_whenExitNonZero() {
        when(launcher.run(any(String.class), any(Duration.class), eq("claude"), eq("-p")))
            .thenReturn(new ProcessResult(1, "", "API error: rate limited"));

        ChatRequest req = new ChatRequest("conv-1", List.of(), "Oi", null);

        assertThatThrownBy(() -> adapter.chat(req))
            .isInstanceOf(LlmAdapterException.class)
            .hasMessageContaining("exit=1")
            .hasMessageContaining("rate limited");
    }

    @Test
    void chat_throwsAdapterException_whenStdoutEmpty() {
        when(launcher.run(any(String.class), any(Duration.class), eq("claude"), eq("-p")))
            .thenReturn(new ProcessResult(0, "", ""));

        ChatRequest req = new ChatRequest("conv-1", List.of(), "Oi", null);

        assertThatThrownBy(() -> adapter.chat(req))
            .isInstanceOf(LlmAdapterException.class)
            .hasMessageContaining("vazio");
    }

    @Test
    void chat_wrapsProcessLaunchException() {
        when(launcher.run(any(String.class), any(Duration.class), eq("claude"), eq("-p")))
            .thenThrow(new ProcessLaunchException("timed out"));

        ChatRequest req = new ChatRequest("conv-1", List.of(), "Oi", null);

        assertThatThrownBy(() -> adapter.chat(req))
            .isInstanceOf(LlmAdapterException.class)
            .hasMessageContaining("timed out");
    }
}
