package com.mayank.dns.protocol;

/**
 * Represents a DNS question entry (RFC 1035 Section 4.1.2).
 *
 *   +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *   |                     QNAME                        |  (variable length)
 *   +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *   |                     QTYPE                        |  (16 bits)
 *   +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *   |                     QCLASS                       |  (16 bits)
 *   +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 */
public class DnsQuestion {

    private String name;       // Domain name (e.g., "www.example.com")
    private int type;          // Query type (e.g., A=1)
    private int dnsClass;      // Query class (usually IN=1)

    public DnsQuestion() {
    }

    public DnsQuestion(String name, int type, int dnsClass) {
        this.name = name;
        this.type = type;
        this.dnsClass = dnsClass;
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

    /**
     * Returns the RecordType enum for this question's type.
     */
    public DnsConstants.RecordType getRecordType() {
        return DnsConstants.RecordType.fromValue(type);
    }

    @Override
    public String toString() {
        return "DnsQuestion{" +
                "name='" + name + '\'' +
                ", type=" + DnsConstants.RecordType.fromValue(type) +
                ", class=" + dnsClass +
                '}';
    }
}
