package com.mayank.dns.protocol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DnsMessage — factory methods, getters, and helpers.
 */
class DnsMessageTest {

    /**
     * Helper to create a minimal query message.
     */
    private DnsMessage createQuery(String domain, int type) {
        DnsMessage query = new DnsMessage();
        query.getHeader().setId(12345);
        query.getHeader().setQr(DnsConstants.QR_QUERY);
        query.getHeader().setRd(true);
        query.getHeader().setQdCount(1);
        query.getQuestions().add(new DnsQuestion(domain, type, DnsConstants.CLASS_IN));
        return query;
    }

    @Nested
    @DisplayName("Default constructor")
    class DefaultConstructorTests {

        @Test
        @DisplayName("Creates message with empty lists")
        void createsEmptyMessage() {
            DnsMessage message = new DnsMessage();

            assertNotNull(message.getHeader());
            assertNotNull(message.getQuestions());
            assertNotNull(message.getAnswers());
            assertNotNull(message.getAuthorities());
            assertNotNull(message.getAdditionals());
            assertTrue(message.getQuestions().isEmpty());
            assertTrue(message.getAnswers().isEmpty());
        }
    }

    @Nested
    @DisplayName("buildResponse")
    class BuildResponseTests {

        @Test
        @DisplayName("Copies ID from the query")
        void copiesIdFromQuery() {
            DnsMessage query = createQuery("example.com", 1);
            List<DnsRecord> answers = new ArrayList<>();

            DnsMessage response = DnsMessage.buildResponse(query, DnsConstants.ResponseCode.NOERROR, answers);

            assertEquals(12345, response.getHeader().getId());
        }

        @Test
        @DisplayName("Sets QR flag to response (1)")
        void setsQrToResponse() {
            DnsMessage query = createQuery("example.com", 1);

            DnsMessage response = DnsMessage.buildResponse(query, DnsConstants.ResponseCode.NOERROR, new ArrayList<>());

            assertEquals(DnsConstants.QR_RESPONSE, response.getHeader().getQr());
        }

        @Test
        @DisplayName("Preserves RD flag from query")
        void preservesRdFlag() {
            DnsMessage query = createQuery("example.com", 1);
            query.getHeader().setRd(true);

            DnsMessage response = DnsMessage.buildResponse(query, DnsConstants.ResponseCode.NOERROR, new ArrayList<>());

            assertTrue(response.getHeader().isRd());
        }

        @Test
        @DisplayName("Sets RA (recursion available) to true")
        void setsRaToTrue() {
            DnsMessage query = createQuery("example.com", 1);

            DnsMessage response = DnsMessage.buildResponse(query, DnsConstants.ResponseCode.NOERROR, new ArrayList<>());

            assertTrue(response.getHeader().isRa());
        }

        @Test
        @DisplayName("Sets correct RCODE")
        void setsCorrectRcode() {
            DnsMessage query = createQuery("example.com", 1);

            DnsMessage response = DnsMessage.buildResponse(query, DnsConstants.ResponseCode.NXDOMAIN, new ArrayList<>());

            assertEquals(DnsConstants.ResponseCode.NXDOMAIN.getValue(), response.getHeader().getRcode());
        }

        @Test
        @DisplayName("Sets ANCOUNT to number of answers")
        void setsAncount() {
            DnsMessage query = createQuery("example.com", 1);
            List<DnsRecord> answers = List.of(
                    new DnsRecord("example.com", 1, 1, 300, new byte[]{1, 2, 3, 4}),
                    new DnsRecord("example.com", 1, 1, 300, new byte[]{5, 6, 7, 8})
            );

            DnsMessage response = DnsMessage.buildResponse(query, DnsConstants.ResponseCode.NOERROR, answers);

            assertEquals(2, response.getHeader().getAnCount());
            assertEquals(2, response.getAnswers().size());
        }

        @Test
        @DisplayName("Copies question section from query")
        void copiesQuestionSection() {
            DnsMessage query = createQuery("example.com", 1);

            DnsMessage response = DnsMessage.buildResponse(query, DnsConstants.ResponseCode.NOERROR, new ArrayList<>());

            assertEquals(1, response.getQuestions().size());
            assertEquals("example.com", response.getQuestions().getFirst().getName());
        }

        @Test
        @DisplayName("Handles null answers list")
        void handlesNullAnswers() {
            DnsMessage query = createQuery("example.com", 1);

            DnsMessage response = DnsMessage.buildResponse(query, DnsConstants.ResponseCode.NOERROR, null);

            assertEquals(0, response.getHeader().getAnCount());
            assertTrue(response.getAnswers().isEmpty());
        }
    }

    @Nested
    @DisplayName("buildNxdomainResponse")
    class BuildNxdomainTests {

        @Test
        @DisplayName("Creates response with NXDOMAIN rcode")
        void createsNxdomainResponse() {
            DnsMessage query = createQuery("nonexistent.com", 1);

            DnsMessage response = DnsMessage.buildNxdomainResponse(query);

            assertEquals(DnsConstants.ResponseCode.NXDOMAIN.getValue(), response.getHeader().getRcode());
            assertTrue(response.getAnswers().isEmpty());
        }
    }

    @Nested
    @DisplayName("buildServfailResponse")
    class BuildServfailTests {

        @Test
        @DisplayName("Creates response with SERVFAIL rcode")
        void createsServfailResponse() {
            DnsMessage query = createQuery("timeout.com", 1);

            DnsMessage response = DnsMessage.buildServfailResponse(query);

            assertEquals(DnsConstants.ResponseCode.SERVFAIL.getValue(), response.getHeader().getRcode());
            assertTrue(response.getAnswers().isEmpty());
        }
    }

    @Nested
    @DisplayName("Helper methods")
    class HelperMethodTests {

        @Test
        @DisplayName("isNxdomain returns true for NXDOMAIN response")
        void isNxdomainReturnsTrueForNxdomain() {
            DnsMessage query = createQuery("bad.com", 1);
            DnsMessage response = DnsMessage.buildNxdomainResponse(query);

            assertTrue(response.isNxdomain());
        }

        @Test
        @DisplayName("isNxdomain returns false for NOERROR response")
        void isNxdomainReturnsFalseForNoerror() {
            DnsMessage query = createQuery("good.com", 1);
            DnsMessage response = DnsMessage.buildResponse(query, DnsConstants.ResponseCode.NOERROR, new ArrayList<>());

            assertFalse(response.isNxdomain());
        }

        @Test
        @DisplayName("getResponseCode returns correct enum")
        void getResponseCodeReturnsEnum() {
            DnsMessage query = createQuery("test.com", 1);
            DnsMessage response = DnsMessage.buildServfailResponse(query);

            assertEquals(DnsConstants.ResponseCode.SERVFAIL, response.getResponseCode());
        }
    }
}
