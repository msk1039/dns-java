package com.mayank.dns.protocol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DnsConstants enums and static values.
 */
class DnsConstantsTest {

    @Nested
    @DisplayName("RecordType enum")
    class RecordTypeTests {

        @Test
        @DisplayName("A record type has value 1")
        void aRecordHasValue1() {
            assertEquals(1, DnsConstants.RecordType.A.getValue());
        }

        @Test
        @DisplayName("AAAA record type has value 28")
        void aaaaRecordHasValue28() {
            assertEquals(28, DnsConstants.RecordType.AAAA.getValue());
        }

        @Test
        @DisplayName("fromValue returns correct RecordType for known values")
        void fromValueReturnsCorrectType() {
            assertEquals(DnsConstants.RecordType.A, DnsConstants.RecordType.fromValue(1));
            assertEquals(DnsConstants.RecordType.NS, DnsConstants.RecordType.fromValue(2));
            assertEquals(DnsConstants.RecordType.CNAME, DnsConstants.RecordType.fromValue(5));
            assertEquals(DnsConstants.RecordType.SOA, DnsConstants.RecordType.fromValue(6));
            assertEquals(DnsConstants.RecordType.MX, DnsConstants.RecordType.fromValue(15));
            assertEquals(DnsConstants.RecordType.TXT, DnsConstants.RecordType.fromValue(16));
            assertEquals(DnsConstants.RecordType.AAAA, DnsConstants.RecordType.fromValue(28));
        }

        @Test
        @DisplayName("fromValue returns UNKNOWN for unrecognized values")
        void fromValueReturnsUnknownForInvalidValue() {
            assertEquals(DnsConstants.RecordType.UNKNOWN, DnsConstants.RecordType.fromValue(999));
            assertEquals(DnsConstants.RecordType.UNKNOWN, DnsConstants.RecordType.fromValue(0));
            assertEquals(DnsConstants.RecordType.UNKNOWN, DnsConstants.RecordType.fromValue(-5));
        }
    }

    @Nested
    @DisplayName("ResponseCode enum")
    class ResponseCodeTests {

        @Test
        @DisplayName("NOERROR has value 0")
        void noerrorHasValue0() {
            assertEquals(0, DnsConstants.ResponseCode.NOERROR.getValue());
        }

        @Test
        @DisplayName("NXDOMAIN has value 3")
        void nxdomainHasValue3() {
            assertEquals(3, DnsConstants.ResponseCode.NXDOMAIN.getValue());
        }

        @Test
        @DisplayName("SERVFAIL has value 2")
        void servfailHasValue2() {
            assertEquals(2, DnsConstants.ResponseCode.SERVFAIL.getValue());
        }

        @Test
        @DisplayName("fromValue returns correct ResponseCode for known values")
        void fromValueReturnsCorrectCode() {
            assertEquals(DnsConstants.ResponseCode.NOERROR, DnsConstants.ResponseCode.fromValue(0));
            assertEquals(DnsConstants.ResponseCode.FORMERR, DnsConstants.ResponseCode.fromValue(1));
            assertEquals(DnsConstants.ResponseCode.SERVFAIL, DnsConstants.ResponseCode.fromValue(2));
            assertEquals(DnsConstants.ResponseCode.NXDOMAIN, DnsConstants.ResponseCode.fromValue(3));
            assertEquals(DnsConstants.ResponseCode.NOTIMP, DnsConstants.ResponseCode.fromValue(4));
            assertEquals(DnsConstants.ResponseCode.REFUSED, DnsConstants.ResponseCode.fromValue(5));
        }

        @Test
        @DisplayName("fromValue returns SERVFAIL for unrecognized values")
        void fromValueReturnsServfailForUnknown() {
            assertEquals(DnsConstants.ResponseCode.SERVFAIL, DnsConstants.ResponseCode.fromValue(99));
            assertEquals(DnsConstants.ResponseCode.SERVFAIL, DnsConstants.ResponseCode.fromValue(-1));
        }
    }

    @Nested
    @DisplayName("Static constants")
    class StaticConstantsTests {

        @Test
        @DisplayName("Header size is 12 bytes")
        void headerSizeIs12() {
            assertEquals(12, DnsConstants.HEADER_SIZE);
        }

        @Test
        @DisplayName("Max UDP size is 512 bytes")
        void maxUdpSizeIs512() {
            assertEquals(512, DnsConstants.MAX_UDP_SIZE);
        }

        @Test
        @DisplayName("QR_QUERY is 0 and QR_RESPONSE is 1")
        void qrFlagValues() {
            assertEquals(0, DnsConstants.QR_QUERY);
            assertEquals(1, DnsConstants.QR_RESPONSE);
        }

        @Test
        @DisplayName("CLASS_IN is 1")
        void classInIs1() {
            assertEquals(1, DnsConstants.CLASS_IN);
        }
    }
}
