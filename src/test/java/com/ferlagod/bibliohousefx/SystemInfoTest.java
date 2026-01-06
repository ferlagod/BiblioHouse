package com.ferlagod.bibliohousefx;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SystemInfo.
 */
public class SystemInfoTest {

    @Test
    void testJavaVersion() {
        String javaVersion = SystemInfo.javaVersion();
        assertNotNull(javaVersion, "Java version should not be null");
        assertFalse(javaVersion.isEmpty(), "Java version should not be empty");
    }

    @Test
    void testJavaFxVersion() {
        // JavaFX version might be null strings logic...
        // We just want to ensure calling it doesn't throw an exception.
        assertDoesNotThrow(() -> SystemInfo.javafxVersion());
    }
}
