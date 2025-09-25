package com.fixserver.protocol;

/**
 * FIX message types enumeration
 */
public enum MessageType {
    
    // Session-level messages
    HEARTBEAT("0", "Heartbeat"),
    TEST_REQUEST("1", "Test Request"),
    RESEND_REQUEST("2", "Resend Request"),
    REJECT("3", "Reject"),
    SEQUENCE_RESET("4", "Sequence Reset"),
    LOGOUT("5", "Logout"),
    LOGON("A", "Logon"),
    
    // Application-level messages
    EXECUTION_REPORT("8", "Execution Report"),
    ORDER_CANCEL_REJECT("9", "Order Cancel Reject"),
    NEW_ORDER_SINGLE("D", "New Order Single"),
    ORDER_CANCEL_REQUEST("F", "Order Cancel Request"),
    ORDER_CANCEL_REPLACE_REQUEST("G", "Order Cancel/Replace Request"),
    ORDER_STATUS_REQUEST("H", "Order Status Request"),
    
    // Business messages
    BUSINESS_MESSAGE_REJECT("j", "Business Message Reject"),
    USER_REQUEST("BE", "User Request"),
    USER_RESPONSE("BF", "User Response"),
    
    // Administrative messages
    XML_MESSAGE("n", "XML Message"),
    MARKET_DATA_REQUEST("V", "Market Data Request"),
    MARKET_DATA_SNAPSHOT("W", "Market Data Snapshot"),
    MARKET_DATA_INCREMENTAL_REFRESH("X", "Market Data Incremental Refresh"),
    MARKET_DATA_REQUEST_REJECT("Y", "Market Data Request Reject");
    
    private final String value;
    private final String description;
    
    MessageType(String value, String description) {
        this.value = value;
        this.description = description;
    }
    
    public String getValue() {
        return value;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static MessageType fromValue(String value) {
        for (MessageType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
    
    public static boolean isValid(String value) {
        return fromValue(value) != null;
    }
    
    public boolean isSessionLevel() {
        return this == HEARTBEAT || this == TEST_REQUEST || this == RESEND_REQUEST ||
               this == REJECT || this == SEQUENCE_RESET || this == LOGOUT || this == LOGON;
    }
    
    public boolean isApplicationLevel() {
        return !isSessionLevel();
    }
    
    @Override
    public String toString() {
        return value + " (" + description + ")";
    }
}