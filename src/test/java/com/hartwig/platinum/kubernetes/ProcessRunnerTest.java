package com.hartwig.platinum.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

public class ProcessRunnerTest {
    @Test
    public void shouldReturnTrueWhenProcessSucceeds() {
        ProcessRunner victim = new ProcessRunner();
        assertThat(victim.execute(List.of("/usr/bin/true"))).isTrue();
    }

    @Test
    public void shouldReturnFalseWhenProcessFails() {
        ProcessRunner victim = new ProcessRunner();
        assertThat(victim.execute(List.of("/usr/bin/false"))).isFalse();
    }

    @Test
    public void shouldReturnFalseWhenProcessBuilderThrows() {
        ProcessRunner victim = new ProcessRunner();
        assertThat(victim.execute(List.of("/a/b/c/not_a_real_program"))).isFalse();
    }
}