package com.smartlogistics.socket.server;

import com.smartlogistics.common.jndi.JndiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Serveur Socket TCP multithreadé recevant les mises à jour GPS
 * et changements de statut des livreurs en temps réel.
 */
public class DeliveryTcpServer {
    private static final Logger log = LoggerFactory.getLogger(DeliveryTcpServer.class);

    private final int port;
    private final Map<Long, DriverClientHandler> activeDrivers = new ConcurrentHashMap<>();
    private ExecutorService threadPool;
    private ServerSocket serverSocket;
    private volatile boolean running;

    public DeliveryTcpServer() {
        this(JndiConfig.getTcpServerPort());
    }

    public DeliveryTcpServer(int port) {
        this.port = port;
    }

    public synchronized void startAsync() throws IOException {
        if (running) {
            return;
        }
        serverSocket = new ServerSocket(port);
        threadPool = Executors.newCachedThreadPool();
        running = true;

        Thread acceptThread = new Thread(() -> {
            log.info("Serveur Socket TCP Delivery démarré sur le port {}", port);
            while (running) {
                try {
                    Socket client = serverSocket.accept();
                    threadPool.submit(new DriverClientHandler(client, activeDrivers));
                } catch (IOException ex) {
                    if (running) {
                        log.error("Erreur accept() sur DeliveryTcpServer", ex);
                    }
                }
            }
        }, "delivery-tcp-acceptor");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public synchronized void stop() {
        running = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
        if (threadPool != null) {
            threadPool.shutdownNow();
        }
        activeDrivers.clear();
        log.info("Serveur Socket TCP Delivery arrêté.");
    }

    public boolean isRunning() {
        return running;
    }

    public int getActiveDriverCount() {
        return activeDrivers.size();
    }

    public static boolean isReachable() {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(JndiConfig.getTcpServerHost(), JndiConfig.getTcpServerPort()), 1000);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }
}
