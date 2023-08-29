package com.hartwig.platinum.kubernetes;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class KubernetesUtilTest {
    @Test
    public void testValidInput() {
        String input = "this_is_a-valid-input string";
        String expectedLabel = "this-is-a-valid-input-string";
        String actualLabel = KubernetesUtil.toValidRFC1123Label(input);
        assertEquals(expectedLabel, actualLabel);
    }

    @Test
    public void testValidInput2Strings() {
        String input = "this_is_a-valid";
        String input2 = "input string";
        String expectedLabel = "this-is-a-valid-input-string";
        String actualLabel = KubernetesUtil.toValidRFC1123Label(input, input2);
        assertEquals(expectedLabel, actualLabel);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidInput() {
        String input = "This is an invalid input string! It's too long and has spaces!";
        KubernetesUtil.toValidRFC1123Label(input);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidUnicode() {
        String input = "this-is-an-íñválid-input-stríng";
        KubernetesUtil.toValidRFC1123Label(input);
    }
}