package com.mayank.dns.protocol;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Represents a DNS resource record (RFC 1035 Section 4.1.3).
 *
 *   +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *   |                      NAME                        |  (variable)
 *   +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *   |                      TYPE                        |  (16 bits)
 *   +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *   |                      CLASS                       |  (16 bits)
 *   +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *   |                      TTL                         |  (32 bits)
 *   +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *   |                    RDLENGTH                      |  (16 bits)
 *   +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *   |                      RDATA                       |  (variable)
 *   +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *
 * rdata is stored as raw bytes — interpretation depends on the record type.
 * For A records, rdata is 4 bytes representing an IPv4 address.
 */
public class DnsRecord {

    private String name;       // Domain name
    private int type;          // Record type (A=1, AAAA=28, etc.)
    private int dnsClass;      // Class (IN=1)
    private long ttl;          // Time to live in seconds
    private byte[] rdata;      // Raw resource data

    public DnsRecord() {
    }

    public DnsRecord(String name, int type, int dnsClass, long ttl, byte[] rdata) {
        this.name = name;
        this.type = type;
        this.dnsClass = dnsClass;
        this.ttl = ttl;
        this.rdata = rdata;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getDnsClass() {
        return dnsClass;
    }

    public void setDnsClass(int dnsClass) {
        this.dnsClass = dnsClass;
    }

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public byte[] getRdata() {
        return rdata;
    }

    public void setRdata(byte[] rdata) {
        this.rdata = rdata;
    }

    /**
     * Returns the RecordType enum for this record.
     */
    public DnsConstants.RecordType getRecordType() {
        return DnsConstants.RecordType.fromValue(type);
    }

    /**
     * For A records, returns the IPv4 address as a string (e.g., "93.184.216.34").
     * For other record types, returns a hex representation.
     */
    public String getRdataAsString() {
        if (rdata == null) {
            return "null";
        }
        if (type == DnsConstants.RecordType.A.getValue() && rdata.length == 4) {
            try {
                return InetAddress.getByAddress(rdata).getHostAddress();
            } catch (UnknownHostException e) {
                return Arrays.toString(rdata);
            }
        }
        // Fallback: hex representation
        StringBuilder sb = new StringBuilder();
        for (byte b : rdata) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "DnsRecord{" +
                "name='" + name + '\'' +
                ", type=" + DnsConstants.RecordType.fromValue(type) +
                ", class=" + dnsClass +
                ", ttl=" + ttl +
                ", rdata=" + getRdataAsString() +
                '}';
    }
}
