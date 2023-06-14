package com.hartwig.platinum.pdl;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import com.hartwig.pdl.generator.PdlGenerator;
import com.hartwig.platinum.ApiRerun;
import com.hartwig.platinum.config.PlatinumConfiguration;

import org.junit.Test;
import org.mockito.InOrder;

public class ConvertForRerunTest {
    @Test
    public void shouldProcessInputsInAlphabeticalOrder() throws Exception {
        PlatinumConfiguration configuration = PlatinumConfiguration.builder().sampleIds(List.of("z", "a", "l", "b")).build();
        PdlGenerator generator = mock(PdlGenerator.class);
        ApiRerun apiRerun = mock(ApiRerun.class);

        when(apiRerun.create("a")).thenReturn(1L);
        when(apiRerun.create("b")).thenReturn(2L);
        when(apiRerun.create("l")).thenReturn(3L);
        when(apiRerun.create("z")).thenReturn(4L);

        InOrder inOrder = inOrder(generator);
        new ConvertForRerun(generator, apiRerun).apply(configuration);

        Thread.sleep(250);

        inOrder.verify(generator).generate(1L);
        inOrder.verify(generator).generate(2L);
        inOrder.verify(generator).generate(3L);
        inOrder.verify(generator).generate(4L);
        inOrder.verifyNoMoreInteractions();
    }
}