package com.rookies.log2doc.entity;

public enum DocumentClassification {

    PUBLIC(1),
    INTERNAL(2),
    CONFIDENTIAL(4),
    SECRET(5),
    TOP_SECRET(7);

    private final int minRoleId;

    DocumentClassification(int minRoleId) {
        this.minRoleId = minRoleId;
    }

    public int getMinRoleId() {
        return minRoleId;
    }
}
