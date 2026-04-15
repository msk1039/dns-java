package com.mayank.dns.protocol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DnsRecord — especially rdata string conversion.
 */
class DnsRecordTest {

    @Test
    @DisplayName("getRdataAsString returns IPv4 address for A record with 4-byte rdata")
    void rdataAsStringReturnsIpv4ForARecord() {
        byte[] rdata = new byte[]{(byte) 93, (byte) 184, (byte) 216, (byte) 34};
        DnsRecord record = new DnsRecord("example.com", 1, 1, 300, rdata);

        assertEquals("93.184.216.34", record.getRdataAsString());
    }

    @Test
    @DisplayName("getRdataAsString returns hex for non-A record types")
    void rdataAsStringReturnsHexForNonARecord() {
        byte[] rdata = new byte[]{0x01, 0x02, (byte) 0xAB};
        DnsRecord record = new DnsRecord("example.com", 28, 1, 300, rdata); // AAAA type

        assertEquals("0102ab", record.getRdataAsString());
    }

    @Test
    @DisplayName("getRdataAsString returns 'null' when rdata is null")
    void rdataAsStringReturnsNullWhenRdataNull() {
        DnsRecord record = new DnsRecord("example.com", 1, 1, 300, null);
        assertEquals("null", record.getRdataAsString());
    }

    @Test
    @DisplayName("getRecordType returns correct enum for A record")
    void getRecordTypeReturnsAForType1() {
        DnsRecord record = new DnsRecord("example.com", 1, 1, 300, new byte[4]);
        assertEquals(DnsConstants.RecordType.A, record.getRecordType());
    }

    @Test
    @DisplayName("getRecordType returns UNKNOWN for unrecognized type")
    void getRecordTypeReturnsUnknownForInvalidType() {
        DnsRecord record = new DnsRecord("example.com", 999, 1, 300, new byte[4]);
        assertEquals(DnsConstants.RecordType.UNKNOWN, record.getRecordType());
    }

    @Test
    @DisplayName("Constructor and getters work correctly")
    void constructorAndGetters() {
        byte[] rdata = new byte[]{1, 2, 3, 4};
        DnsRecord record = new DnsRecord("test.com", 1, 1, 3600, rdata);

        assertEquals("test.com", record.getName());
        assertEquals(1, record.getType());
        assertEquals(1, record.getDnsClass());
        assertEquals(3600, record.getTtl());
        assertArrayEquals(rdata, record.getRdata());
    }

    @Test
    @DisplayName("Setters work correctly")
    void settersWork() {
        DnsRecord record = new DnsRecord();
        record.setName("updated.com");
        record.setType(28);
        record.setDnsClass(1);
        record.setTtl(7200);
        record.setRdata(new byte[]{10, 20});

        assertEquals("updated.com", record.getName());
        assertEquals(28, record.getType());
        assertEquals(7200, record.getTtl());
    }

    @Test
    @DisplayName("toString contains domain name and type")
    void toStringContainsRelevantInfo() {
        DnsRecord record = new DnsRecord("example.com", 1, 1, 300, new byte[]{1, 2, 3, 4});
        String str = record.toString();

        assertTrue(str.contains("example.com"));
        assertTrue(str.contains("A"));
    }

    @Test
    @DisplayName("getRdataAsString handles IP address 0.0.0.0")
    void rdataAsStringHandlesZeroIp() {
        byte[] rdata = new byte[]{0, 0, 0, 0};
        DnsRecord record = new DnsRecord("example.com", 1, 1, 300, rdata);
        assertEquals("0.0.0.0", record.getRdataAsString());
    }

    @Test
    @DisplayName("getRdataAsString handles IP address 255.255.255.255")
    void rdataAsStringHandlesMaxIp() {
        byte[] rdata = new byte[]{(byte) 255, (byte) 255, (byte) 255, (byte) 255};
        DnsRecord record = new DnsRecord("example.com", 1, 1, 300, rdata);
        assertEquals("255.255.255.255", record.getRdataAsString());
    }
}
