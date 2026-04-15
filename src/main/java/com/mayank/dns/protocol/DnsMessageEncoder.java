package com.mayank.dns.protocol;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Encodes a DnsMessage object into raw DNS bytes for transmission.
 * Does NOT use label compression for simplicity — domain names are written in full.
 */
public class DnsMessageEncoder {

    /**
     * Encodes a complete DNS message to bytes.
     */
    public byte[] encode(DnsMessage message) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);

            encodeHeader(out, message.getHeader());

            // Encode questions
            for (DnsQuestion question : message.getQuestions()) {
                encodeQuestion(out, question);
            }

            // Encode answer records
            for (DnsRecord record : message.getAnswers()) {
                encodeRecord(out, record);
            }

            // Encode authority records
            for (DnsRecord record : message.getAuthorities()) {
                encodeRecord(out, record);
            }

            // Encode additional records
            for (DnsRecord record : message.getAdditionals()) {
                encodeRecord(out, record);
            }

            out.flush();
            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to encode DNS message", e);
        }
    }

    /**
     * Encodes the 12-byte DNS header.
     */
    private void encodeHeader(DataOutputStream out, DnsHeader header) throws IOException {
        out.writeShort(header.getId());

        int flags = 0;
        flags |= (header.getQr() & 0x1) << 15;
        flags |= (header.getOpcode() & 0xF) << 11;
        flags |= (header.isAa() ? 1 : 0) << 10;
        flags |= (header.isTc() ? 1 : 0) << 9;
        flags |= (header.isRd() ? 1 : 0) << 8;
        flags |= (header.isRa() ? 1 : 0) << 7;
        flags |= (header.getRcode() & 0xF);
        out.writeShort(flags);

        out.writeShort(header.getQdCount());
        out.writeShort(header.getAnCount());
        out.writeShort(header.getNsCount());
        out.writeShort(header.getArCount());
    }

    /**
     * Encodes a DNS question section entry.
     */
    private void encodeQuestion(DataOutputStream out, DnsQuestion question) throws IOException {
        encodeDomainName(out, question.getName());
        out.writeShort(question.getType());
        out.writeShort(question.getDnsClass());
    }

    /**
     * Encodes a DNS resource record.
     */
    private void encodeRecord(DataOutputStream out, DnsRecord record) throws IOException {
        encodeDomainName(out, record.getName());
        out.writeShort(record.getType());
        out.writeShort(record.getDnsClass());
        out.writeInt((int) record.getTtl());
        out.writeShort(record.getRdata().length);
        out.write(record.getRdata());
    }

    /**
     * Encodes a domain name as a sequence of labels.
     * Each label is preceded by its length byte. The name ends with a zero byte.
     * 
     * Example: "www.example.com" → [3]www[7]example[3]com[0]
     */
    private void encodeDomainName(DataOutputStream out, String name) throws IOException {
        if (name == null || name.isEmpty()) {
            out.writeByte(0);
            return;
        }

        String[] labels = name.split("\\.");
        for (String label : labels) {
            byte[] labelBytes = label.getBytes();
            if (labelBytes.length > 63) {
                throw new IllegalArgumentException("DNS label exceeds 63 characters: " + label);
            }
            out.writeByte(labelBytes.length);
            out.write(labelBytes);
        }
        out.writeByte(0); // terminating zero-length label
    }
}
