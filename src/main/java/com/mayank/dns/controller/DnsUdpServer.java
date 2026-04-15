package com.mayank.dns.controller;

import com.mayank.dns.config.DnsServerConfig;
import com.mayank.dns.protocol.DnsMessage;
import com.mayank.dns.protocol.DnsMessageDecoder;
import com.mayank.dns.protocol.DnsMessageEncoder;
import com.mayank.dns.service.DnsQueryService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * UDP DNS server that listens for incoming DNS queries on the configured port.
 * Acts as the "controller" layer for DNS protocol — receives packets,
 * delegates to DnsQueryService, and sends responses back.
 *
 * Runs on a dedicated daemon thread; each incoming query is handled
 * on a virtual thread for high concurrency.
 */
@Component
public class DnsUdpServer {

    private static final Logger log = LoggerFactory.getLogger(DnsUdpServer.class);
    private static final int BUFFER_SIZE = 512; // Standard DNS UDP max

    private final DnsServerConfig config;
    private final DnsQueryService queryService;
    private final DnsMessageEncoder encoder;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private DatagramSocket socket;
    private Thread listenerThread;
    private ExecutorService workerPool;

    public DnsUdpServer(DnsServerConfig config, DnsQueryService queryService) {
        this.config = config;
        this.queryService = queryService;
        this.encoder = new DnsMessageEncoder();
    }

    @PostConstruct
    public void start() {
        running.set(true);
        workerPool = Executors.newVirtualThreadPerTaskExecutor();

        listenerThread = new Thread(this::listen, "dns-udp-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();

        log.info("DNS UDP server starting on port {}", config.getServer().getPort());
    }

    @PreDestroy
    public void stop() {
        running.set(false);

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        if (workerPool != null) {
            workerPool.shutdown();
        }

        if (listenerThread != null) {
            listenerThread.interrupt();
        }

        log.info("DNS UDP server stopped");
    }

    /**
     * Main listener loop. Receives UDP packets and dispatches them to worker threads.
     */
    private void listen() {
        try {
            socket = new DatagramSocket(config.getServer().getPort());
            log.info("DNS UDP server listening on port {}", config.getServer().getPort());

            while (running.get()) {
                try {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    DatagramPacket inPacket = new DatagramPacket(buffer, buffer.length);
                    socket.receive(inPacket);

                    // Copy packet data for processing on a worker thread
                    byte[] requestData = new byte[inPacket.getLength()];
                    System.arraycopy(inPacket.getData(), 0, requestData, 0, inPacket.getLength());
                    InetAddress clientAddress = inPacket.getAddress();
                    int clientPort = inPacket.getPort();

                    workerPool.submit(() -> handlePacket(requestData, clientAddress, clientPort));

                } catch (Exception e) {
                    if (running.get()) {
                        log.error("Error receiving DNS packet: {}", e.getMessage(), e);
                    }
                }
            }
        } catch (Exception e) {
            if (running.get()) {
                log.error("Failed to start DNS UDP server on port {}: {}",
                        config.getServer().getPort(), e.getMessage(), e);
            }
        }
    }

    /**
     * Handles a single DNS query packet: decode → process → encode → send response.
     */
    private void handlePacket(byte[] requestData, InetAddress clientAddress, int clientPort) {
        try {
            // Decode the incoming query
            DnsMessageDecoder decoder = new DnsMessageDecoder(requestData);
            DnsMessage query = decoder.decode();

            log.debug("Received query from {}:{} — {}", clientAddress, clientPort, query.getQuestions());

            // Process the query through the service layer
            DnsMessage response = queryService.handleQuery(query);

            // Encode and send the response
            byte[] responseBytes = encoder.encode(response);
            DatagramPacket outPacket = new DatagramPacket(
                    responseBytes, responseBytes.length, clientAddress, clientPort
            );

            socket.send(outPacket);

            log.debug("Sent response to {}:{} ({} bytes)", clientAddress, clientPort, responseBytes.length);

        } catch (Exception e) {
            log.error("Error handling DNS query from {}:{}: {}", clientAddress, clientPort, e.getMessage(), e);
        }
    }
}
