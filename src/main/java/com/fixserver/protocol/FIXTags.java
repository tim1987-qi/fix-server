package com.fixserver.protocol;

import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive FIX protocol tag definitions and utilities.
 * 
 * This class provides:
 * - Standard FIX field tag constants
 * - Human-readable field names for logging
 * - Message type definitions
 * - Field validation utilities
 * 
 * Based on FIX 4.4 specification with common extensions.
 * 
 * @author FIX Server Team
 * @version 1.0
 * @since 1.0
 */
public final class FIXTags {
    
    // ========== STANDARD HEADER FIELDS ==========
    /** BeginString - FIX protocol version */
    public static final int BEGIN_STRING = 8;
    /** BodyLength - Length of message body */
    public static final int BODY_LENGTH = 9;
    /** CheckSum - Message checksum */
    public static final int CHECKSUM = 10;
    /** MsgType - Message type */
    public static final int MSG_TYPE = 35;
    /** SenderCompID - Sender company ID */
    public static final int SENDER_COMP_ID = 49;
    /** TargetCompID - Target company ID */
    public static final int TARGET_COMP_ID = 56;
    /** MsgSeqNum - Message sequence number */
    public static final int MSG_SEQ_NUM = 34;
    /** SendingTime - Time of message transmission */
    public static final int SENDING_TIME = 52;
    /** OrigSendingTime - Original sending time */
    public static final int ORIG_SENDING_TIME = 122;
    /** PossDupFlag - Possible duplicate flag */
    public static final int POSS_DUP_FLAG = 43;
    
    // ========== SESSION LEVEL FIELDS ==========
    /** HeartBtInt - Heartbeat interval */
    public static final int HEARTBT_INT = 108;
    /** TestReqID - Test request ID */
    public static final int TEST_REQ_ID = 112;
    /** EncryptMethod - Encryption method */
    public static final int ENCRYPT_METHOD = 98;
    /** Username - Username for authentication */
    public static final int USERNAME = 553;
    /** Password - Password for authentication */
    public static final int PASSWORD = 554;
    /** ResetSeqNumFlag - Reset sequence number flag */
    public static final int RESET_SEQ_NUM_FLAG = 141;
    /** NextExpectedMsgSeqNum - Next expected sequence number */
    public static final int NEXT_EXPECTED_MSG_SEQ_NUM = 789;
    
    // ========== BUSINESS MESSAGE FIELDS ==========
    /** ClOrdID - Client order ID */
    public static final int CL_ORD_ID = 11;
    /** OrderID - Order ID */
    public static final int ORDER_ID = 37;
    /** ExecID - Execution ID */
    public static final int EXEC_ID = 17;
    /** ExecType - Execution type */
    public static final int EXEC_TYPE = 150;
    /** OrdStatus - Order status */
    public static final int ORD_STATUS = 39;
    /** Symbol - Trading symbol */
    public static final int SYMBOL = 55;
    /** Side - Buy/Sell indicator */
    public static final int SIDE = 54;
    /** OrderQty - Order quantity */
    public static final int ORDER_QTY = 38;
    /** Price - Order price */
    public static final int PRICE = 44;
    /** OrdType - Order type */
    public static final int ORD_TYPE = 40;
    /** TimeInForce - Time in force */
    public static final int TIME_IN_FORCE = 59;
    /** TransactTime - Transaction time */
    public static final int TRANSACT_TIME = 60;
    /** LastQty - Last executed quantity */
    public static final int LAST_QTY = 32;
    /** LastPx - Last executed price */
    public static final int LAST_PX = 31;
    /** CumQty - Cumulative quantity */
    public static final int CUM_QTY = 14;
    /** AvgPx - Average price */
    public static final int AVG_PX = 6;
    /** LeavesQty - Remaining quantity */
    public static final int LEAVES_QTY = 151;
    
    // ========== REJECT AND ERROR FIELDS ==========
    /** Text - Free format text */
    public static final int TEXT = 58;
    /** RefSeqNum - Reference sequence number */
    public static final int REF_SEQ_NUM = 45;
    /** RefTagID - Reference tag ID */
    public static final int REF_TAG_ID = 371;
    /** RefMsgType - Reference message type */
    public static final int REF_MSG_TYPE = 372;
    /** SessionRejectReason - Session reject reason */
    public static final int SESSION_REJECT_REASON = 373;
    /** BusinessRejectReason - Business reject reason */
    public static final int BUSINESS_REJECT_REASON = 380;
    /** BusinessRejectRefID - Business reject reference ID */
    public static final int BUSINESS_REJECT_REF_ID = 379;
    
    // ========== GAP FILL AND RESEND FIELDS ==========
    /** BeginSeqNo - Begin sequence number */
    public static final int BEGIN_SEQ_NO = 7;
    /** EndSeqNo - End sequence number */
    public static final int END_SEQ_NO = 16;
    /** GapFillFlag - Gap fill flag */
    public static final int GAP_FILL_FLAG = 123;
    /** NewSeqNo - New sequence number */
    public static final int NEW_SEQ_NO = 36;
    
    // ========== MESSAGE TYPE CONSTANTS ==========
    public static final class MsgType {
        /** Heartbeat */
        public static final String HEARTBEAT = "0";
        /** Test Request */
        public static final String TEST_REQUEST = "1";
        /** Resend Request */
        public static final String RESEND_REQUEST = "2";
        /** Reject */
        public static final String REJECT = "3";
        /** Sequence Reset */
        public static final String SEQUENCE_RESET = "4";
        /** Logout */
        public static final String LOGOUT = "5";
        /** Logon */
        public static final String LOGON = "A";
        /** New Order Single */
        public static final String NEW_ORDER_SINGLE = "D";
        /** Execution Report */
        public static final String EXECUTION_REPORT = "8";
        /** Order Cancel Reject */
        public static final String ORDER_CANCEL_REJECT = "9";
        /** Business Message Reject */
        public static final String BUSINESS_MESSAGE_REJECT = "j";
    }
    
    // ========== FIELD NAME MAPPINGS FOR LOGGING ==========
    private static final Map<Integer, String> FIELD_NAMES = new HashMap<>();
    private static final Map<String, String> MSG_TYPE_NAMES = new HashMap<>();
    
    static {
        // Initialize field name mappings
        FIELD_NAMES.put(BEGIN_STRING, "BeginString");
        FIELD_NAMES.put(BODY_LENGTH, "BodyLength");
        FIELD_NAMES.put(CHECKSUM, "CheckSum");
        FIELD_NAMES.put(MSG_TYPE, "MsgType");
        FIELD_NAMES.put(SENDER_COMP_ID, "SenderCompID");
        FIELD_NAMES.put(TARGET_COMP_ID, "TargetCompID");
        FIELD_NAMES.put(MSG_SEQ_NUM, "MsgSeqNum");
        FIELD_NAMES.put(SENDING_TIME, "SendingTime");
        FIELD_NAMES.put(ORIG_SENDING_TIME, "OrigSendingTime");
        FIELD_NAMES.put(POSS_DUP_FLAG, "PossDupFlag");
        
        FIELD_NAMES.put(HEARTBT_INT, "HeartBtInt");
        FIELD_NAMES.put(TEST_REQ_ID, "TestReqID");
        FIELD_NAMES.put(ENCRYPT_METHOD, "EncryptMethod");
        FIELD_NAMES.put(USERNAME, "Username");
        FIELD_NAMES.put(PASSWORD, "Password");
        FIELD_NAMES.put(RESET_SEQ_NUM_FLAG, "ResetSeqNumFlag");
        FIELD_NAMES.put(NEXT_EXPECTED_MSG_SEQ_NUM, "NextExpectedMsgSeqNum");
        
        FIELD_NAMES.put(CL_ORD_ID, "ClOrdID");
        FIELD_NAMES.put(ORDER_ID, "OrderID");
        FIELD_NAMES.put(EXEC_ID, "ExecID");
        FIELD_NAMES.put(EXEC_TYPE, "ExecType");
        FIELD_NAMES.put(ORD_STATUS, "OrdStatus");
        FIELD_NAMES.put(SYMBOL, "Symbol");
        FIELD_NAMES.put(SIDE, "Side");
        FIELD_NAMES.put(ORDER_QTY, "OrderQty");
        FIELD_NAMES.put(PRICE, "Price");
        FIELD_NAMES.put(ORD_TYPE, "OrdType");
        FIELD_NAMES.put(TIME_IN_FORCE, "TimeInForce");
        FIELD_NAMES.put(TRANSACT_TIME, "TransactTime");
        FIELD_NAMES.put(LAST_QTY, "LastQty");
        FIELD_NAMES.put(LAST_PX, "LastPx");
        FIELD_NAMES.put(CUM_QTY, "CumQty");
        FIELD_NAMES.put(AVG_PX, "AvgPx");
        FIELD_NAMES.put(LEAVES_QTY, "LeavesQty");
        
        FIELD_NAMES.put(TEXT, "Text");
        FIELD_NAMES.put(REF_SEQ_NUM, "RefSeqNum");
        FIELD_NAMES.put(REF_TAG_ID, "RefTagID");
        FIELD_NAMES.put(REF_MSG_TYPE, "RefMsgType");
        FIELD_NAMES.put(SESSION_REJECT_REASON, "SessionRejectReason");
        FIELD_NAMES.put(BUSINESS_REJECT_REASON, "BusinessRejectReason");
        FIELD_NAMES.put(BUSINESS_REJECT_REF_ID, "BusinessRejectRefID");
        
        FIELD_NAMES.put(BEGIN_SEQ_NO, "BeginSeqNo");
        FIELD_NAMES.put(END_SEQ_NO, "EndSeqNo");
        FIELD_NAMES.put(GAP_FILL_FLAG, "GapFillFlag");
        FIELD_NAMES.put(NEW_SEQ_NO, "NewSeqNo");
        
        // Initialize message type names
        MSG_TYPE_NAMES.put(MsgType.HEARTBEAT, "Heartbeat");
        MSG_TYPE_NAMES.put(MsgType.TEST_REQUEST, "TestRequest");
        MSG_TYPE_NAMES.put(MsgType.RESEND_REQUEST, "ResendRequest");
        MSG_TYPE_NAMES.put(MsgType.REJECT, "Reject");
        MSG_TYPE_NAMES.put(MsgType.SEQUENCE_RESET, "SequenceReset");
        MSG_TYPE_NAMES.put(MsgType.LOGOUT, "Logout");
        MSG_TYPE_NAMES.put(MsgType.LOGON, "Logon");
        MSG_TYPE_NAMES.put(MsgType.NEW_ORDER_SINGLE, "NewOrderSingle");
        MSG_TYPE_NAMES.put(MsgType.EXECUTION_REPORT, "ExecutionReport");
        MSG_TYPE_NAMES.put(MsgType.ORDER_CANCEL_REJECT, "OrderCancelReject");
        MSG_TYPE_NAMES.put(MsgType.BUSINESS_MESSAGE_REJECT, "BusinessMessageReject");
    }
    
    /**
     * Get human-readable field name for a tag number.
     * 
     * @param tag FIX field tag number
     * @return human-readable field name or "Tag{number}" if unknown
     */
    public static String getFieldName(int tag) {
        return FIELD_NAMES.getOrDefault(tag, "Tag" + tag);
    }
    
    /**
     * Get human-readable message type name.
     * 
     * @param msgType FIX message type code
     * @return human-readable message type name or the original code if unknown
     */
    public static String getMessageTypeName(String msgType) {
        return MSG_TYPE_NAMES.getOrDefault(msgType, msgType);
    }
    
    /**
     * Format a FIX message for human-readable logging.
     * Converts tag numbers to field names and message types to readable names.
     * 
     * @param fixMessage raw FIX message string
     * @return human-readable formatted message
     */
    public static String formatForLogging(String fixMessage) {
        if (fixMessage == null || fixMessage.isEmpty()) {
            return fixMessage;
        }
        
        StringBuilder formatted = new StringBuilder();
        String[] fields = fixMessage.split("\\001"); // Split by SOH
        
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                formatted.append(" | ");
            }
            
            String field = fields[i];
            int equalPos = field.indexOf('=');
            if (equalPos > 0) {
                try {
                    int tag = Integer.parseInt(field.substring(0, equalPos));
                    String value = field.substring(equalPos + 1);
                    String fieldName = getFieldName(tag);
                    
                    // Special formatting for message type
                    if (tag == MSG_TYPE) {
                        String msgTypeName = getMessageTypeName(value);
                        formatted.append(fieldName).append("=").append(value)
                                .append("(").append(msgTypeName).append(")");
                    } else {
                        formatted.append(fieldName).append("=").append(value);
                    }
                } catch (NumberFormatException e) {
                    // If tag is not a number, keep original format
                    formatted.append(field);
                }
            } else {
                formatted.append(field);
            }
        }
        
        return formatted.toString();
    }
    
    /**
     * Check if a field is a required header field.
     * 
     * @param tag field tag number
     * @return true if field is required in message header
     */
    public static boolean isRequiredHeaderField(int tag) {
        return tag == BEGIN_STRING || tag == BODY_LENGTH || tag == MSG_TYPE ||
               tag == SENDER_COMP_ID || tag == TARGET_COMP_ID || tag == MSG_SEQ_NUM ||
               tag == SENDING_TIME;
    }
    
    /**
     * Check if a field is a standard trailer field.
     * 
     * @param tag field tag number
     * @return true if field belongs in message trailer
     */
    public static boolean isTrailerField(int tag) {
        return tag == CHECKSUM;
    }
    
    /**
     * Get the expected data type for a field.
     * 
     * @param tag field tag number
     * @return expected data type as string
     */
    public static String getFieldDataType(int tag) {
        switch (tag) {
            case BODY_LENGTH:
            case MSG_SEQ_NUM:
            case HEARTBT_INT:
            case ORDER_QTY:
            case LAST_QTY:
            case CUM_QTY:
            case LEAVES_QTY:
            case REF_SEQ_NUM:
            case BEGIN_SEQ_NO:
            case END_SEQ_NO:
            case NEW_SEQ_NO:
                return "INT";
                
            case PRICE:
            case LAST_PX:
            case AVG_PX:
                return "PRICE";
                
            case SENDING_TIME:
            case ORIG_SENDING_TIME:
            case TRANSACT_TIME:
                return "UTCTIMESTAMP";
                
            case POSS_DUP_FLAG:
            case RESET_SEQ_NUM_FLAG:
            case GAP_FILL_FLAG:
                return "BOOLEAN";
                
            default:
                return "STRING";
        }
    }
    
    // Private constructor to prevent instantiation
    private FIXTags() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}