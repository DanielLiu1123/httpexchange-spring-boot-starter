package io.github.danielliu1123;

import java.net.ServerSocket;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

/**
 * @author Freeman
 */
@UtilityClass
public class PortGetter {

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
