package com.rookies.log2doc.entity;

/**
 * 문서 처리 상태를 나타내는 Enum.
 * - PROCESSING: 처리 중
 * - COMPLETED: 처리 완료
 * - FAILED: 처리 실패
 */
public enum DocumentStatus {
    /** 문서 처리 중 상태 */
    PROCESSING,

    /** 문서 처리 완료 상태 */
    COMPLETED,

    /** 문서 처리 실패 상태 */
    FAILED
}
