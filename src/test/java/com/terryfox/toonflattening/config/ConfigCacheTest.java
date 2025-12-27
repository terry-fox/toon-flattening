package com.terryfox.toonflattening.config;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigCacheTest {

    @Mock
    private ModConfigEvent.Reloading mockReloadEvent;

    private ConfigCache configCache;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        configCache = new ConfigCache();
    }

    @Test
    void reload_cachesAllConfigValues() {
        configCache.reload();

        // Verify all getters return non-default values
        assertTrue(configCache.damageAmount() >= 0f && configCache.damageAmount() <= 20f);
        assertTrue(configCache.heightScale() >= 0.01f && configCache.heightScale() <= 1.0f);
        assertTrue(configCache.widthScale() >= 1.0f && configCache.widthScale() <= 6.0f);
        assertTrue(configCache.spreadIncrement() >= 0.1f && configCache.spreadIncrement() <= 2.0f);
        assertTrue(configCache.maxSpreadLimit() >= 1.0f && configCache.maxSpreadLimit() <= 6.0f);
        assertTrue(configCache.reformationTicks() >= 1 && configCache.reformationTicks() <= 100);
        assertTrue(configCache.fallbackTimeoutTicks() >= 0);
    }

    @Test
    void defaultValues_matchSpecification() {
        configCache.reload();

        // From spec: defaults
        assertEquals(4.0f, configCache.damageAmount(), 0.001f);
        assertEquals(0.05f, configCache.heightScale(), 0.001f);
        assertEquals(1.8f, configCache.widthScale(), 0.001f);
        assertEquals(0.8f, configCache.spreadIncrement(), 0.001f);
        assertEquals(6.0f, configCache.maxSpreadLimit(), 0.001f);
        assertEquals(5, configCache.reformationTicks());
        assertTrue(configCache.anvilBlockingEnabled());
        assertEquals(6000, configCache.fallbackTimeoutTicks()); // 300 seconds * 20 ticks
    }

    @Test
    void fallbackTimeoutTicks_convertsSecondsToTicks() {
        configCache.reload();

        // Default: 300 seconds = 6000 ticks
        assertEquals(6000, configCache.fallbackTimeoutTicks());
    }

    @Test
    void onConfigReload_reloadsWhenCorrectSpec() {
        ConfigCache spy = spy(configCache);
        ModConfigEvent.Reloading event = mock(ModConfigEvent.Reloading.class);
        net.neoforged.fml.config.ModConfig config = mock(net.neoforged.fml.config.ModConfig.class);

        when(event.getConfig()).thenReturn(config);
        when(config.getSpec()).thenReturn(ToonFlattenConfig.SPEC);

        spy.onConfigReload(event);

        verify(spy, times(1)).reload();
    }

    @Test
    void onConfigReload_ignoresOtherSpecs() {
        ConfigCache spy = spy(configCache);
        ModConfigEvent.Reloading event = mock(ModConfigEvent.Reloading.class);
        net.neoforged.fml.config.ModConfig config = mock(net.neoforged.fml.config.ModConfig.class);
        ModConfigSpec otherSpec = new ModConfigSpec.Builder().build();

        when(event.getConfig()).thenReturn(config);
        when(config.getSpec()).thenReturn(otherSpec);

        spy.onConfigReload(event);

        verify(spy, never()).reload();
    }

    @Test
    void get_returnsSingletonInstance() {
        ConfigCache instance1 = ConfigCache.get();
        ConfigCache instance2 = ConfigCache.get();

        assertNotNull(instance1);
        assertSame(instance1, instance2);
    }

    @Test
    void volatileFields_ensureThreadSafety() throws NoSuchFieldException {
        // Verify critical fields are volatile for hot-reload safety
        assertTrue(java.lang.reflect.Modifier.isVolatile(
            ConfigCache.class.getDeclaredField("damageAmount").getModifiers()
        ));
        assertTrue(java.lang.reflect.Modifier.isVolatile(
            ConfigCache.class.getDeclaredField("anvilBlockingEnabled").getModifiers()
        ));
    }
}
