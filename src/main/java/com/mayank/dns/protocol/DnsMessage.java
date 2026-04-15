package com.mayank.dns.protocol;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a complete DNS message (RFC 1035 Section 4.1).
 * Contains the header, question section, and resource record sections
 * (answers, authority, additional).
 */
public class DnsMessage {

    private DnsHeader header;
    private List<DnsQuestion> questions;
    private List<DnsRecord> answers;
    private List<DnsRecord> authorities;
    private List<DnsRecord> additionals;

    public DnsMessage() {
        this.header = new DnsHeader();
        this.questions = new ArrayList<>();
        this.answers = new ArrayList<>();
        this.authorities = new ArrayList<>();
        this.additionals = new ArrayList<>();
    }

    // --- Convenience factory methods ---

    /**
     * Creates a response message for the given query, copying the ID and question.
     */
    public static DnsMessage buildResponse(DnsMessage query, DnsConstants.ResponseCode rcode, List<DnsRecord> answers) {
        DnsMessage response = new DnsMessage();
        DnsHeader header = response.getHeader();

        header.setId(query.getHeader().getId());
        header.setQr(DnsConstants.QR_RESPONSE);
        header.setOpcode(DnsConstants.OPCODE_QUERY);
        header.setAa(false);
        header.setTc(false);
        header.setRd(query.getHeader().isRd());
        header.setRa(true);
        header.setRcode(rcode.getValue());
        header.setQdCount(query.getQuestions().size());
        header.setAnCount(answers != null ? answers.size() : 0);
        header.setNsCount(0);
        header.setArCount(0);

        response.setQuestions(new ArrayList<>(query.getQuestions()));
        response.setAnswers(answers != null ? answers : new ArrayList<>());

        return response;
    }

    /**
     * Creates an NXDOMAIN response for the given query.
     */
    public static DnsMessage buildNxdomainResponse(DnsMessage query) {
        return buildResponse(query, DnsConstants.ResponseCode.NXDOMAIN, new ArrayList<>());
    }

    /**
     * Creates a SERVFAIL response for the given query.
     */
    public static DnsMessage buildServfailResponse(DnsMessage query) {
        return buildResponse(query, DnsConstants.ResponseCode.SERVFAIL, new ArrayList<>());
    }

    // --- Getters and Setters ---

    public DnsHeader getHeader() {
        return header;
    }

    public void setHeader(DnsHeader header) {
        this.header = header;
    }

    public List<DnsQuestion> getQuestions() {
        return questions;
    }

    public void setQuestions(List<DnsQuestion> questions) {
        this.questions = questions;
    }

    public List<DnsRecord> getAnswers() {
        return answers;
    }

    public void setAnswers(List<DnsRecord> answers) {
        this.answers = answers;
    }

    public List<DnsRecord> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(List<DnsRecord> authorities) {
        this.authorities = authorities;
    }

    public List<DnsRecord> getAdditionals() {
        return additionals;
    }

    public void setAdditionals(List<DnsRecord> additionals) {
        this.additionals = additionals;
    }

    /**
     * Checks if this message is an NXDOMAIN response.
     */
    public boolean isNxdomain() {
        return header.getRcode() == DnsConstants.ResponseCode.NXDOMAIN.getValue();
    }

    /**
     * Gets the response code as an enum.
     */
    public DnsConstants.ResponseCode getResponseCode() {
        return DnsConstants.ResponseCode.fromValue(header.getRcode());
    }

    @Override
    public String toString() {
        return "DnsMessage{" +
                "header=" + header +
                ", questions=" + questions +
                ", answers=" + answers +
                ", authorities=" + authorities +
                ", additionals=" + additionals +
                '}';
    }
}
