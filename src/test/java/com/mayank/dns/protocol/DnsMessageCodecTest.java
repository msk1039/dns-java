package com.mayank.dns.protocol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DnsMessageEncoder and DnsMessageDecoder.
 * Verifies encode → decode roundtrip, and correct byte-level encoding per RFC 1035.
 */
class DnsMessageCodecTest {

    private final DnsMessageEncoder encoder = new DnsMessageEncoder();

    /**
     * Helper to create a standard A-record query.
     */
    private DnsMessage createQuery(String domain) {
        DnsMessage message = new DnsMessage();
        DnsHeader header = message.getHeader();
        header.setId(0xABCD);
        header.setQr(DnsConstants.QR_QUERY);
        header.setOpcode(DnsConstants.OPCODE_QUERY);
        header.setRd(true);
        header.setQdCount(1);

        message.getQuestions().add(new DnsQuestion(domain, DnsConstants.RecordType.A.getValue(), DnsConstants.CLASS_IN));
        return message;
    }

    /**
     * Helper to create a response with answer records.
     */
    private DnsMessage createResponseWithAnswers(String domain, byte[]... ips) {
        DnsMessage query = createQuery(domain);
        List<DnsRecord> answers = new java.util.ArrayList<>();
        for (byte[] ip : ips) {
            answers.add(new DnsRecord(domain, DnsConstants.RecordType.A.getValue(), DnsConstants.CLASS_IN, 300, ip));
        }
        return DnsMessage.buildResponse(query, DnsConstants.ResponseCode.NOERROR, answers);
    }

    @Nested
    @DisplayName("Encode → Decode Roundtrip")
    class RoundtripTests {

        @Test
        @DisplayName("Simple query roundtrip preserves all fields")
        void simpleQueryRoundtrip() {
            DnsMessage original = createQuery("google.com");

            byte[] encoded = encoder.encode(original);
            DnsMessage decoded = new DnsMessageDecoder(encoded).decode();

            assertEquals(original.getHeader().getId(), decoded.getHeader().getId());
            assertEquals(original.getHeader().getQr(), decoded.getHeader().getQr());
            assertEquals(original.getHeader().isRd(), decoded.getHeader().isRd());
            assertEquals(original.getHeader().getQdCount(), decoded.getHeader().getQdCount());
            assertEquals(1, decoded.getQuestions().size());
            assertEquals("google.com", decoded.getQuestions().getFirst().getName());
            assertEquals(DnsConstants.RecordType.A.getValue(), decoded.getQuestions().getFirst().getType());
            assertEquals(DnsConstants.CLASS_IN, decoded.getQuestions().getFirst().getDnsClass());
        }

        @Test
        @DisplayName("Response with single A record roundtrip")
        void singleAnswerRoundtrip() {
            DnsMessage original = createResponseWithAnswers("example.com",
                    new byte[]{(byte) 93, (byte) 184, (byte) 216, (byte) 34});

            byte[] encoded = encoder.encode(original);
            DnsMessage decoded = new DnsMessageDecoder(encoded).decode();

            assertEquals(1, decoded.getAnswers().size());
            DnsRecord answer = decoded.getAnswers().getFirst();
            assertEquals("example.com", answer.getName());
            assertEquals(DnsConstants.RecordType.A.getValue(), answer.getType());
            assertEquals(300, answer.getTtl());
            assertEquals("93.184.216.34", answer.getRdataAsString());
        }

        @Test
        @DisplayName("Response with multiple A records roundtrip")
        void multipleAnswersRoundtrip() {
            DnsMessage original = createResponseWithAnswers("amazon.com",
                    new byte[]{(byte) 98, (byte) 87, (byte) 170, (byte) 74},
                    new byte[]{(byte) 98, (byte) 87, (byte) 170, (byte) 71},
                    new byte[]{(byte) 98, (byte) 82, (byte) 161, (byte) 185});

            byte[] encoded = encoder.encode(original);
            DnsMessage decoded = new DnsMessageDecoder(encoded).decode();

            assertEquals(3, decoded.getAnswers().size());
            assertEquals("98.87.170.74", decoded.getAnswers().get(0).getRdataAsString());
            assertEquals("98.87.170.71", decoded.getAnswers().get(1).getRdataAsString());
            assertEquals("98.82.161.185", decoded.getAnswers().get(2).getRdataAsString());
        }

        @Test
        @DisplayName("NXDOMAIN response roundtrip preserves rcode")
        void nxdomainRoundtrip() {
            DnsMessage query = createQuery("nonexistent.com");
            DnsMessage original = DnsMessage.buildNxdomainResponse(query);

            byte[] encoded = encoder.encode(original);
            DnsMessage decoded = new DnsMessageDecoder(encoded).decode();

            assertEquals(DnsConstants.ResponseCode.NXDOMAIN.getValue(), decoded.getHeader().getRcode());
            assertEquals(DnsConstants.QR_RESPONSE, decoded.getHeader().getQr());
            assertTrue(decoded.getAnswers().isEmpty());
        }

        @Test
        @DisplayName("SERVFAIL response roundtrip preserves rcode")
        void servfailRoundtrip() {
            DnsMessage query = createQuery("timeout.com");
            DnsMessage original = DnsMessage.buildServfailResponse(query);

            byte[] encoded = encoder.encode(original);
            DnsMessage decoded = new DnsMessageDecoder(encoded).decode();

            assertEquals(DnsConstants.ResponseCode.SERVFAIL.getValue(), decoded.getHeader().getRcode());
        }

        @Test
        @DisplayName("Subdomain roundtrip (www.sub.example.com)")
        void subdomainRoundtrip() {
            DnsMessage original = createQuery("www.sub.example.com");

            byte[] encoded = encoder.encode(original);
            DnsMessage decoded = new DnsMessageDecoder(encoded).decode();

            assertEquals("www.sub.example.com", decoded.getQuestions().getFirst().getName());
        }

        @Test
        @DisplayName("Single-label domain roundtrip (localhost)")
        void singleLabelRoundtrip() {
            DnsMessage original = createQuery("localhost");

            byte[] encoded = encoder.encode(original);
            DnsMessage decoded = new DnsMessageDecoder(encoded).decode();

            assertEquals("localhost", decoded.getQuestions().getFirst().getName());
        }
    }

    @Nested
    @DisplayName("Encoder specifics")
    class EncoderTests {

        @Test
        @DisplayName("Encoded header is exactly 12 bytes when no questions/answers")
        void headerIs12Bytes() {
            DnsMessage message = new DnsMessage();
            byte[] encoded = encoder.encode(message);

            assertEquals(DnsConstants.HEADER_SIZE, encoded.length);
        }

        @Test
        @DisplayName("Encoded query has correct total size")
        void queryHasCorrectSize() {
            // "google.com" = [6]google[3]com[0] = 12 bytes
            // + QTYPE(2) + QCLASS(2) = 16 bytes for question
            // + 12 bytes header = 28 bytes total
            DnsMessage query = createQuery("google.com");
            byte[] encoded = encoder.encode(query);

            assertEquals(28, encoded.length);
        }

        @Test
        @DisplayName("Transaction ID is encoded correctly in first 2 bytes")
        void transactionIdEncoding() {
            DnsMessage message = new DnsMessage();
            message.getHeader().setId(0x1234);

            byte[] encoded = encoder.encode(message);

            assertEquals(0x12, encoded[0] & 0xFF);
            assertEquals(0x34, encoded[1] & 0xFF);
        }

        @Test
        @DisplayName("RD flag is encoded in correct bit position")
        void rdFlagEncoding() {
            DnsMessage message = new DnsMessage();
            message.getHeader().setRd(true);

            byte[] encoded = encoder.encode(message);

            // Flags are bytes 2-3, RD is bit 8 (byte 2, bit 0)
            assertEquals(0x01, encoded[2] & 0x01);
        }

        @Test
        @DisplayName("QR response flag is encoded in correct bit position")
        void qrFlagEncoding() {
            DnsMessage message = new DnsMessage();
            message.getHeader().setQr(DnsConstants.QR_RESPONSE);

            byte[] encoded = encoder.encode(message);

            // QR is bit 15 (byte 2, bit 7)
            assertEquals(0x80, encoded[2] & 0x80);
        }
    }

    @Nested
    @DisplayName("Decoder specifics")
    class DecoderTests {

        @Test
        @DisplayName("Decodes header flags correctly")
        void decodesHeaderFlags() {
            // Manually construct bytes: ID=0x1234, flags=0x8180 (QR=1,RD=1,RA=1), QD=1, AN=0, NS=0, AR=0
            byte[] data = new byte[]{
                    0x12, 0x34,             // ID
                    (byte) 0x81, (byte) 0x80, // Flags: QR=1, RD=1, RA=1
                    0x00, 0x01,             // QDCOUNT = 1
                    0x00, 0x00,             // ANCOUNT = 0
                    0x00, 0x00,             // NSCOUNT = 0
                    0x00, 0x00,             // ARCOUNT = 0
                    // Question: "a.com" type=A class=IN
                    0x01, 'a', 0x03, 'c', 'o', 'm', 0x00,  // [1]a[3]com[0]
                    0x00, 0x01,             // QTYPE = A
                    0x00, 0x01              // QCLASS = IN
            };

            DnsMessage message = new DnsMessageDecoder(data).decode();

            assertEquals(0x1234, message.getHeader().getId());
            assertEquals(DnsConstants.QR_RESPONSE, message.getHeader().getQr());
            assertTrue(message.getHeader().isRd());
            assertTrue(message.getHeader().isRa());
            assertFalse(message.getHeader().isAa());
            assertFalse(message.getHeader().isTc());
            assertEquals(0, message.getHeader().getRcode());
            assertEquals(1, message.getQuestions().size());
            assertEquals("a.com", message.getQuestions().getFirst().getName());
        }

        @Test
        @DisplayName("Handles DNS label compression (pointer)")
        void handlesLabelCompression() {
            // Build a response where the answer name uses a pointer to the question name
            byte[] data = new byte[]{
                    // Header
                    0x00, 0x01,             // ID = 1
                    (byte) 0x81, (byte) 0x80, // Flags: QR=1, RD=1, RA=1
                    0x00, 0x01,             // QDCOUNT = 1
                    0x00, 0x01,             // ANCOUNT = 1
                    0x00, 0x00,             // NSCOUNT = 0
                    0x00, 0x00,             // ARCOUNT = 0
                    // Question: "a.b" at offset 12
                    0x01, 'a', 0x01, 'b', 0x00, // [1]a[1]b[0] — 5 bytes, starts at offset 12
                    0x00, 0x01,             // QTYPE = A
                    0x00, 0x01,             // QCLASS = IN
                    // Answer: name is pointer to offset 12 (0xC00C)
                    (byte) 0xC0, 0x0C,      // Pointer to offset 12 → "a.b"
                    0x00, 0x01,             // TYPE = A
                    0x00, 0x01,             // CLASS = IN
                    0x00, 0x00, 0x01, 0x2C, // TTL = 300
                    0x00, 0x04,             // RDLENGTH = 4
                    0x01, 0x02, 0x03, 0x04  // RDATA = 1.2.3.4
            };

            DnsMessage message = new DnsMessageDecoder(data).decode();

            assertEquals("a.b", message.getQuestions().getFirst().getName());
            assertEquals(1, message.getAnswers().size());
            assertEquals("a.b", message.getAnswers().getFirst().getName());
            assertEquals(300, message.getAnswers().getFirst().getTtl());
            assertEquals("1.2.3.4", message.getAnswers().getFirst().getRdataAsString());
        }

        @Test
        @DisplayName("Decodes NXDOMAIN rcode correctly")
        void decodesNxdomainRcode() {
            // Header with RCODE=3 (NXDOMAIN)
            byte[] data = new byte[]{
                    0x00, 0x01,             // ID
                    (byte) 0x81, (byte) 0x83, // Flags: QR=1, RD=1, RA=1, RCODE=3
                    0x00, 0x01,             // QDCOUNT = 1
                    0x00, 0x00,             // ANCOUNT = 0
                    0x00, 0x00,             // NSCOUNT = 0
                    0x00, 0x00,             // ARCOUNT = 0
                    // Question: "x.y"
                    0x01, 'x', 0x01, 'y', 0x00,
                    0x00, 0x01,             // QTYPE = A
                    0x00, 0x01              // QCLASS = IN
            };

            DnsMessage message = new DnsMessageDecoder(data).decode();

            assertEquals(DnsConstants.ResponseCode.NXDOMAIN.getValue(), message.getHeader().getRcode());
            assertTrue(message.isNxdomain());
            assertTrue(message.getAnswers().isEmpty());
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Empty domain name encodes and decodes to empty string")
        void emptyDomainName() {
            DnsMessage message = new DnsMessage();
            message.getHeader().setQdCount(1);
            message.getQuestions().add(new DnsQuestion("", DnsConstants.RecordType.A.getValue(), DnsConstants.CLASS_IN));

            byte[] encoded = encoder.encode(message);
            DnsMessage decoded = new DnsMessageDecoder(encoded).decode();

            assertEquals("", decoded.getQuestions().getFirst().getName());
        }

        @Test
        @DisplayName("Large TTL value roundtrips correctly")
        void largeTtlRoundtrip() {
            DnsMessage query = createQuery("ttl.test");
            DnsRecord record = new DnsRecord("ttl.test", 1, 1, 86400, new byte[]{10, 0, 0, 1}); // 24 hours
            DnsMessage response = DnsMessage.buildResponse(query, DnsConstants.ResponseCode.NOERROR, List.of(record));

            byte[] encoded = encoder.encode(response);
            DnsMessage decoded = new DnsMessageDecoder(encoded).decode();

            assertEquals(86400, decoded.getAnswers().getFirst().getTtl());
        }

        @Test
        @DisplayName("Zero TTL roundtrips correctly")
        void zeroTtlRoundtrip() {
            DnsMessage query = createQuery("zero.ttl");
            DnsRecord record = new DnsRecord("zero.ttl", 1, 1, 0, new byte[]{10, 0, 0, 1});
            DnsMessage response = DnsMessage.buildResponse(query, DnsConstants.ResponseCode.NOERROR, List.of(record));

            byte[] encoded = encoder.encode(response);
            DnsMessage decoded = new DnsMessageDecoder(encoded).decode();

            assertEquals(0, decoded.getAnswers().getFirst().getTtl());
        }
    }
}
