package com.taskforge.timeline;

public enum JobEventType {
    CREATED,
    CLAIMED,
    SUCCEEDED,
    FAILED,
    DEAD,
    REQUEUED,
    STALE_LOCK_RELEASED
}
