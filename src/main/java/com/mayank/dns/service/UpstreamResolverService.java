package com.mayank.dns.service;

import com.mayank.dns.config.DnsServerConfig;
import com.mayank.dns.protocol.DnsMessage;
import com.mayank.dns.protocol.DnsMessageDecoder;
import com.mayank.dns.protocol.DnsMessageEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

/**
 * Service that forwards DNS queries to an upstream DNS server (e.g., 8.8.8.8)
 * and returns the parsed response.
 * Handles timeout scenarios gracefully.
 */
@Service
public class UpstreamResolverService {

    private static final Logger log = LoggerFactory.getLogger(UpstreamResolverService.class);

    private final DnsServerConfig config;
    private final DnsMessageEncoder encoder;

    public UpstreamResolverService(DnsServerConfig config) {
        this.config = config;
        this.encoder = new DnsMessageEncoder();
    }

    /**
     * Forwards the given DNS query to the upstream server and returns the response.
     *
     * @param query the DNS query message to forward
     * @return the parsed DNS response from upstream
     * @throws UpstreamTimeoutException if the upstream server does not respond within the timeout
     * @throws UpstreamResolveException if any other error occurs during resolution
     */
    public DnsMessage resolve(DnsMessage query) {
        byte[] queryBytes = encoder.encode(query);

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(config.getServer().getTimeoutMs());

            InetAddress upstreamAddress = InetAddress.getByName(config.getServer().getUpstream());
            int upstreamPort = config.getServer().getUpstreamPort();

            // Send query to upstream
            DatagramPacket outPacket = new DatagramPacket(
                    queryBytes, queryBytes.length, upstreamAddress, upstreamPort
            );
            socket.send(outPacket);

            log.debug("Forwarded query to {}:{} ({} bytes)",
                    config.getServer().getUpstream(), upstreamPort, queryBytes.length);

            // Receive response
            byte[] responseBuffer = new byte[512]; // standard DNS UDP max
            DatagramPacket inPacket = new DatagramPacket(responseBuffer, responseBuffer.length);
            socket.receive(inPacket);

            log.debug("Received response from upstream ({} bytes)", inPacket.getLength());

            // Parse the response
            byte[] responseData = new byte[inPacket.getLength()];
            System.arraycopy(inPacket.getData(), 0, responseData, 0, inPacket.getLength());

            DnsMessageDecoder decoder = new DnsMessageDecoder(responseData);
            return decoder.decode();

        } catch (SocketTimeoutException e) {
            log.warn("Upstream DNS server {}:{} timed out after {}ms",
                    config.getServer().getUpstream(),
                    config.getServer().getUpstreamPort(),
                    config.getServer().getTimeoutMs());
            throw new UpstreamTimeoutException("Upstream DNS server timed out", e);

        } catch (Exception e) {
            log.error("Error resolving query via upstream: {}", e.getMessage(), e);
            throw new UpstreamResolveException("Failed to resolve via upstream", e);
        }
    }

    /**
     * Thrown when the upstream DNS server does not respond in time.
     */
    public static class UpstreamTimeoutException extends RuntimeException {
        public UpstreamTimeoutException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Thrown when an unexpected error occurs during upstream resolution.
     */
    public static class UpstreamResolveException extends RuntimeException {
        public UpstreamResolveException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
