package com.mayank.dns.protocol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DnsQuestion.
 */
class DnsQuestionTest {

    @Test
    @DisplayName("Constructor with arguments sets all fields")
    void constructorSetsFields() {
        DnsQuestion question = new DnsQuestion("google.com", 1, 1);

        assertEquals("google.com", question.getName());
        assertEquals(1, question.getType());
        assertEquals(1, question.getDnsClass());
    }

    @Test
    @DisplayName("Default constructor creates empty question")
    void defaultConstructor() {
        DnsQuestion question = new DnsQuestion();
        assertNull(question.getName());
        assertEquals(0, question.getType());
        assertEquals(0, question.getDnsClass());
    }

    @Test
    @DisplayName("getRecordType returns A for type 1")
    void getRecordTypeReturnsA() {
        DnsQuestion question = new DnsQuestion("example.com", 1, 1);
        assertEquals(DnsConstants.RecordType.A, question.getRecordType());
    }

    @Test
    @DisplayName("getRecordType returns AAAA for type 28")
    void getRecordTypeReturnsAAAA() {
        DnsQuestion question = new DnsQuestion("example.com", 28, 1);
        assertEquals(DnsConstants.RecordType.AAAA, question.getRecordType());
    }

    @Test
    @DisplayName("toString contains domain name and type")
    void toStringContainsInfo() {
        DnsQuestion question = new DnsQuestion("test.com", 1, 1);
        String str = question.toString();

        assertTrue(str.contains("test.com"));
        assertTrue(str.contains("A"));
    }
}
