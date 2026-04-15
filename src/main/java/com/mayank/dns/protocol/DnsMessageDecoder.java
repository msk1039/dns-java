package com.mayank.dns.protocol;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Decodes raw DNS bytes into a DnsMessage object.
 * Handles DNS label compression (pointers) as per RFC 1035 Section 4.1.4.
 */
public class DnsMessageDecoder {

    private final byte[] data;
    private final ByteBuffer buffer;

    public DnsMessageDecoder(byte[] data) {
        this.data = data;
        this.buffer = ByteBuffer.wrap(data);
    }

    /**
     * Decodes the complete DNS message from the raw bytes.
     */
    public DnsMessage decode() {
        DnsMessage message = new DnsMessage();

        // Decode header (12 bytes)
        DnsHeader header = decodeHeader();
        message.setHeader(header);

        // Decode questions
        List<DnsQuestion> questions = new ArrayList<>();
        for (int i = 0; i < header.getQdCount(); i++) {
            questions.add(decodeQuestion());
        }
        message.setQuestions(questions);

        // Decode answer records
        List<DnsRecord> answers = new ArrayList<>();
        for (int i = 0; i < header.getAnCount(); i++) {
            answers.add(decodeRecord());
        }
        message.setAnswers(answers);

        // Decode authority records
        List<DnsRecord> authorities = new ArrayList<>();
        for (int i = 0; i < header.getNsCount(); i++) {
            authorities.add(decodeRecord());
        }
        message.setAuthorities(authorities);

        // Decode additional records
        List<DnsRecord> additionals = new ArrayList<>();
        for (int i = 0; i < header.getArCount(); i++) {
            additionals.add(decodeRecord());
        }
        message.setAdditionals(additionals);

        return message;
    }

    /**
     * Decodes the 12-byte DNS header.
     */
    private DnsHeader decodeHeader() {
        DnsHeader header = new DnsHeader();

        header.setId(readUnsignedShort());

        int flags = readUnsignedShort();
        header.setQr((flags >> 15) & 0x1);
        header.setOpcode((flags >> 11) & 0xF);
        header.setAa(((flags >> 10) & 0x1) == 1);
        header.setTc(((flags >> 9) & 0x1) == 1);
        header.setRd(((flags >> 8) & 0x1) == 1);
        header.setRa(((flags >> 7) & 0x1) == 1);
        header.setRcode(flags & 0xF);

        header.setQdCount(readUnsignedShort());
        header.setAnCount(readUnsignedShort());
        header.setNsCount(readUnsignedShort());
        header.setArCount(readUnsignedShort());

        return header;
    }

    /**
     * Decodes a question section entry.
     */
    private DnsQuestion decodeQuestion() {
        DnsQuestion question = new DnsQuestion();
        question.setName(decodeDomainName());
        question.setType(readUnsignedShort());
        question.setDnsClass(readUnsignedShort());
        return question;
    }

    /**
     * Decodes a resource record.
     */
    private DnsRecord decodeRecord() {
        DnsRecord record = new DnsRecord();
        record.setName(decodeDomainName());
        record.setType(readUnsignedShort());
        record.setDnsClass(readUnsignedShort());
        record.setTtl(readUnsignedInt());

        int rdLength = readUnsignedShort();
        byte[] rdata = new byte[rdLength];
        buffer.get(rdata);
        record.setRdata(rdata);

        return record;
    }

    /**
     * Decodes a domain name, handling DNS label compression (pointers).
     * 
     * Domain names are encoded as a sequence of labels:
     *   - Each label starts with a length byte, followed by that many characters.
     *   - A zero-length label marks the end.
     *   - If the two high bits of the length byte are set (0xC0), it's a pointer
     *     to another location in the message.
     */
    private String decodeDomainName() {
        StringBuilder name = new StringBuilder();
        boolean jumped = false;
        int savedPosition = -1;
        int maxJumps = 10; // safety limit to prevent infinite loops
        int jumpsPerformed = 0;

        while (true) {
            if (jumpsPerformed > maxJumps) {
                throw new IllegalStateException("Too many DNS label compression jumps — possible loop");
            }

            int length = Byte.toUnsignedInt(data[buffer.position()]);

            // Check for pointer (compression): top 2 bits are 11
            if ((length & 0xC0) == 0xC0) {
                if (!jumped) {
                    savedPosition = buffer.position() + 2; // save position after the 2-byte pointer
                }
                int pointer = ((length & 0x3F) << 8) | Byte.toUnsignedInt(data[buffer.position() + 1]);
                buffer.position(pointer);
                jumped = true;
                jumpsPerformed++;
                continue;
            }

            buffer.get(); // consume the length byte

            if (length == 0) {
                break; // end of domain name
            }

            if (!name.isEmpty()) {
                name.append('.');
            }

            byte[] label = new byte[length];
            buffer.get(label);
            name.append(new String(label));
        }

        if (jumped) {
            buffer.position(savedPosition); // restore position after pointer
        }

        return name.toString();
    }

    /**
     * Reads an unsigned 16-bit integer from the buffer.
     */
    private int readUnsignedShort() {
        return Short.toUnsignedInt(buffer.getShort());
    }

    /**
     * Reads an unsigned 32-bit integer from the buffer.
     */
    private long readUnsignedInt() {
        return Integer.toUnsignedLong(buffer.getInt());
    }
}
