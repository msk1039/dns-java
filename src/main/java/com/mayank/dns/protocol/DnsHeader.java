package com.mayank.dns.protocol;

/**
 * Represents the 12-byte DNS message header (RFC 1035 Section 4.1.1).
 *
 * Layout (each row = 16 bits):
 *   +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *   |                      ID                         |
 *   +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *   |QR| Opcode  |AA|TC|RD|RA| Z      |   RCODE      |
 *   +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *   |                    QDCOUNT                       |
 *   +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *   |                    ANCOUNT                       |
 *   +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *   |                    NSCOUNT                       |
 *   +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *   |                    ARCOUNT                       |
 *   +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 */
public class DnsHeader {

    private int id;           // 16-bit identifier
    private int qr;           // 1-bit: 0=query, 1=response
    private int opcode;       // 4-bit: kind of query
    private boolean aa;       // Authoritative Answer
    private boolean tc;       // Truncation
    private boolean rd;       // Recursion Desired
    private boolean ra;       // Recursion Available
    private int rcode;        // 4-bit response code
    private int qdCount;      // Number of questions
    private int anCount;      // Number of answer records
    private int nsCount;      // Number of authority records
    private int arCount;      // Number of additional records

    public DnsHeader() {
    }

    // --- Getters and Setters ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getQr() {
        return qr;
    }

    public void setQr(int qr) {
        this.qr = qr;
    }

    public int getOpcode() {
        return opcode;
    }

    public void setOpcode(int opcode) {
        this.opcode = opcode;
    }

    public boolean isAa() {
        return aa;
    }

    public void setAa(boolean aa) {
        this.aa = aa;
    }

    public boolean isTc() {
        return tc;
    }

    public void setTc(boolean tc) {
        this.tc = tc;
    }

    public boolean isRd() {
        return rd;
    }

    public void setRd(boolean rd) {
        this.rd = rd;
    }

    public boolean isRa() {
        return ra;
    }

    public void setRa(boolean ra) {
        this.ra = ra;
    }

    public int getRcode() {
        return rcode;
    }

    public void setRcode(int rcode) {
        this.rcode = rcode;
    }

    public int getQdCount() {
        return qdCount;
    }

    public void setQdCount(int qdCount) {
        this.qdCount = qdCount;
    }

    public int getAnCount() {
        return anCount;
    }

    public void setAnCount(int anCount) {
        this.anCount = anCount;
    }

    public int getNsCount() {
        return nsCount;
    }

    public void setNsCount(int nsCount) {
        this.nsCount = nsCount;
    }

    public int getArCount() {
        return arCount;
    }

    public void setArCount(int arCount) {
        this.arCount = arCount;
    }

    @Override
    public String toString() {
        return "DnsHeader{" +
                "id=" + id +
                ", qr=" + qr +
                ", opcode=" + opcode +
                ", aa=" + aa +
                ", tc=" + tc +
                ", rd=" + rd +
                ", ra=" + ra +
                ", rcode=" + rcode +
                ", qdCount=" + qdCount +
                ", anCount=" + anCount +
                ", nsCount=" + nsCount +
                ", arCount=" + arCount +
                '}';
    }
}
