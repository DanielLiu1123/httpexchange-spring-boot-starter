package com.freemanan.starter;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * @author Freeman
 */
public final class PortGetter {

    private PortGetter() {}

    /**
     * Get an available port.
     *
     * @return port
     */
    public static int availablePort() {
        try {
            ServerSocket socket = new ServerSocket(0);
            int port = socket.getLocalPort();
            socket.close();
            return port;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
