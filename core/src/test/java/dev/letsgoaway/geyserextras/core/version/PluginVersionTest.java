package dev.letsgoaway.geyserextras.core.version;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PluginVersionTest {
    @Test
    void runtimeVersionMatchesTheCurrentBuild() {
        assertEquals("2.2.0.1", PluginVersion.GE_VERSION);
    }

    @Test
    void identifiesOnlyNumericallyNewerReleases() {
        assertTrue(PluginVersion.isNewerVersion("2.3.0", "2.2.0"));
        assertTrue(PluginVersion.isNewerVersion("2.10.0", "2.9.9"));
        assertTrue(PluginVersion.isNewerVersion("2.2.1", "2.2.0"));
        assertFalse(PluginVersion.isNewerVersion("2.1.0", "2.2.0"));
        assertFalse(PluginVersion.isNewerVersion("2.2.0", "2.2.0"));
        assertFalse(PluginVersion.isNewerVersion("2.2", "2.2.0"));
    }

    @Test
    void treatsTrailingZeroComponentsAsEquivalent() {
        assertFalse(PluginVersion.isNewerVersion("2.2", "2.2.0"));
        assertFalse(PluginVersion.isNewerVersion("2.2.0.0", "2.2"));
    }

    @Test
    void comparesFourComponentMaintenanceVersions() {
        assertTrue(PluginVersion.isNewerVersion("2.2.0.1", "2.2.0"));
        assertTrue(PluginVersion.isNewerVersion("2.2.0.2", "2.2.0.1"));
        assertFalse(PluginVersion.isNewerVersion("2.2.0", "2.2.0.1"));
        assertFalse(PluginVersion.isNewerVersion("2.2.0.1", "2.2.0.1"));
    }

    @Test
    void safelyRejectsMalformedVersions() {
        assertFalse(PluginVersion.isNewerVersion(null, "2.2.0"));
        assertFalse(PluginVersion.isNewerVersion("latest", "2.2.0"));
        assertFalse(PluginVersion.isNewerVersion("2.x.0", "2.2.0"));
    }
}
