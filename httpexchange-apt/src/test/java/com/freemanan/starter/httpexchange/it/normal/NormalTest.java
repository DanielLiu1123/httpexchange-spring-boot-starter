package com.freemanan.starter.httpexchange.it.normal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class NormalTest {

    @Test
    void testApi() {
        assertDoesNotThrow(() -> {
            Class.forName("com.freemanan.starter.httpexchange.it.normal.ApiBase")
                    .getDeclaredMethod("get");
        });
        assertThrows(NoSuchMethodException.class, () -> {
            Class.forName("com.freemanan.starter.httpexchange.it.normal.ApiBase")
                    .getDeclaredMethod("post");
        });

        assertThrows(ClassNotFoundException.class, () -> {
            Class.forName("com.freemanan.starter.httpexchange.it.normal.Api2Base");
        });

        assertThrows(NoSuchMethodException.class, () -> {
            Class.forName("com.freemanan.starter.httpexchange.it.normal.Api3Base")
                    .getDeclaredMethod("get");
        });

        assertDoesNotThrow(() -> {
            Class.forName("com.freemanan.starter.httpexchange.it.normal.Api4Base")
                    .getDeclaredMethod("get");
        });
        assertDoesNotThrow(() -> {
            Class.forName("com.freemanan.starter.httpexchange.it.normal.InnerApiBase")
                    .getDeclaredMethod("get");
        });
        assertThrows(ClassNotFoundException.class, () -> {
            Class.forName("com.freemanan.starter.httpexchange.it.normal.InnerApi2Base");
        });

        assertDoesNotThrow(() -> {
            Class.forName("com.freemanan.starter.httpexchange.it.normal.Api6Base")
                    .getDeclaredMethod("get");
        });
        assertThrows(NoSuchMethodException.class, () -> {
            Class.forName("com.freemanan.starter.httpexchange.it.normal.Api7Base")
                    .getDeclaredMethod("get");
        });
        assertThrows(ClassNotFoundException.class, () -> {
            Class.forName("com.freemanan.starter.httpexchange.it.normal.Api8Base");
        });
    }
}