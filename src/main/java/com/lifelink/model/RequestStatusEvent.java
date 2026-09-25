package com.lifelink.model;

import java.time.LocalDateTime;

public class RequestStatusEvent {

    private final int requestId;
    private final RequestStatus status;
    private final String note;
    private final LocalDateTime timestamp;

    public RequestStatusEvent(int requestId, RequestStatus status, String note, LocalDateTime timestamp) {
        this.requestId = requestId;
        this.status = status;
        this.note = note;
        this.timestamp = timestamp;
    }

    public int getRequestId() {
        return requestId;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
