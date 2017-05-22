package org.lamerman.rosandroidbag;

import org.junit.Test;

import static org.junit.Assert.*;

public class GeneralUnitTest {

    @Test
    public void extractEnvironmentVariables_isCorrect() throws Exception {
        String[] pairs = RecordLauncher.extractEnvironmentVariables("A=B,C=D,E=comma\\,test");
        assertEquals(pairs.length, 3);
        assertEquals(pairs[0], "A=B");
        assertEquals(pairs[1], "C=D");
        assertEquals(pairs[2], "E=comma,test");
    }
}