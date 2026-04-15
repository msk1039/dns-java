package com.mayank.dns.protocol;

/**
 * DNS protocol constants as defined in RFC 1035.
 * Designed for extensibility — add new record types or response codes as needed.
 */
public final class DnsConstants {

    private DnsConstants() {
        // Utility class
    }

    // --- Header size ---
    public static final int HEADER_SIZE = 12;

    // --- Maximum UDP DNS message size ---
    public static final int MAX_UDP_SIZE = 512;

    // --- QR (Query/Response) flag ---
    public static final int QR_QUERY = 0;
    public static final int QR_RESPONSE = 1;

    // --- Opcode values ---
    public static final int OPCODE_QUERY = 0;

    // --- DNS Class ---
    public static final int CLASS_IN = 1; // Internet

    // --- Record Types ---
    public enum RecordType {
        A(1),
        NS(2),
        CNAME(5),
        SOA(6),
        MX(15),
        TXT(16),
        AAAA(28),
        UNKNOWN(-1);

        private final int value;

        RecordType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static RecordType fromValue(int value) {
            for (RecordType type : values()) {
                if (type.value == value) {
                    return type;
                }
            }
            return UNKNOWN;
        }
    }

    // --- Response Codes (RCODE) ---
    public enum ResponseCode {
        NOERROR(0),
        FORMERR(1),    // Format error
        SERVFAIL(2),   // Server failure
        NXDOMAIN(3),   // Non-existent domain
        NOTIMP(4),     // Not implemented
        REFUSED(5);    // Query refused

        private final int value;

        ResponseCode(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static ResponseCode fromValue(int value) {
            for (ResponseCode code : values()) {
                if (code.value == value) {
                    return code;
                }
            }
            return SERVFAIL;
        }
    }
}
