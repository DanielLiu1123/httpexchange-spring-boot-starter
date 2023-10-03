package com.freemanan.starter;

import java.net.ServerSocket;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

/**
 * @author Freeman
 */
@UtilityClass
public final class PortGetter {

    /**
     * Get an available port.
     *
     * @return port
     */
    @SneakyThrows
    public static int availablePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
