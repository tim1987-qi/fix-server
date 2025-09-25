package com.fixserver.protocol;

import java.util.Map;
import java.util.HashMap;

/**
 * FIX field definitions and validation rules
 */
public class FieldDefinition {
    
    private static final Map<Integer, FieldInfo> FIELD_DEFINITIONS = new HashMap<>();
    
    static {
        // Standard header fields
        addField(8, "BeginString", FieldType.STRING, true);
        addField(9, "BodyLength", FieldType.LENGTH, true);
        addField(35, "MsgType", FieldType.STRING, true);
        addField(49, "SenderCompID", FieldType.STRING, true);
        addField(56, "TargetCompID", FieldType.STRING, true);
        addField(34, "MsgSeqNum", FieldType.SEQNUM, true);
        addField(52, "SendingTime", FieldType.UTCTIMESTAMP, true);
        addField(10, "CheckSum", FieldType.STRING, true);
        
        // Common application fields
        addField(11, "ClOrdID", FieldType.STRING, false);
        addField(14, "CumQty", FieldType.QTY, false);
        addField(17, "ExecID", FieldType.STRING, false);
        addField(20, "ExecTransType", FieldType.CHAR, false);
        addField(31, "LastPx", FieldType.PRICE, false);
        addField(32, "LastQty", FieldType.QTY, false);
        addField(37, "OrderID", FieldType.STRING, false);
        addField(38, "OrderQty", FieldType.QTY, false);
        addField(39, "OrdStatus", FieldType.CHAR, false);
        addField(40, "OrdType", FieldType.CHAR, false);
        addField(44, "Price", FieldType.PRICE, false);
        addField(54, "Side", FieldType.CHAR, false);
        addField(55, "Symbol", FieldType.STRING, false);
        addField(58, "Text", FieldType.STRING, false);
        addField(60, "TransactTime", FieldType.UTCTIMESTAMP, false);
        addField(150, "ExecType", FieldType.CHAR, false);
        addField(151, "LeavesQty", FieldType.QTY, false);
        
        // Session-level fields
        addField(98, "EncryptMethod", FieldType.INT, false);
        addField(108, "HeartBtInt", FieldType.INT, false);
        addField(112, "TestReqID", FieldType.STRING, false);
        addField(123, "GapFillFlag", FieldType.BOOLEAN, false);
        addField(141, "ResetSeqNumFlag", FieldType.BOOLEAN, false);
        
        // Market data fields
        addField(262, "MDReqID", FieldType.STRING, false);
        addField(263, "SubscriptionRequestType", FieldType.CHAR, false);
        addField(264, "MarketDepth", FieldType.INT, false);
        addField(267, "NoMDEntryTypes", FieldType.NUMINGROUP, false);
        addField(268, "NoMDEntries", FieldType.NUMINGROUP, false);
        addField(269, "MDEntryType", FieldType.CHAR, false);
        addField(270, "MDEntryPx", FieldType.PRICE, false);
        addField(271, "MDEntrySize", FieldType.QTY, false);
        addField(272, "MDEntryDate", FieldType.UTCDATEONLY, false);
        addField(273, "MDEntryTime", FieldType.UTCTIMEONLY, false);
    }
    
    private static void addField(int tag, String name, FieldType type, boolean required) {
        FIELD_DEFINITIONS.put(tag, new FieldInfo(tag, name, type, required));
    }
    
    public static FieldInfo getFieldInfo(int tag) {
        return FIELD_DEFINITIONS.get(tag);
    }
    
    public static boolean isKnownField(int tag) {
        return FIELD_DEFINITIONS.containsKey(tag);
    }
    
    public static boolean isRequiredField(int tag) {
        FieldInfo info = FIELD_DEFINITIONS.get(tag);
        return info != null && info.isRequired();
    }
    
    public static String getFieldName(int tag) {
        FieldInfo info = FIELD_DEFINITIONS.get(tag);
        return info != null ? info.getName() : "Unknown(" + tag + ")";
    }
    
    public static FieldType getFieldType(int tag) {
        FieldInfo info = FIELD_DEFINITIONS.get(tag);
        return info != null ? info.getType() : FieldType.STRING;
    }
    
    /**
     * Field information container
     */
    public static class FieldInfo {
        private final int tag;
        private final String name;
        private final FieldType type;
        private final boolean required;
        
        public FieldInfo(int tag, String name, FieldType type, boolean required) {
            this.tag = tag;
            this.name = name;
            this.type = type;
            this.required = required;
        }
        
        public int getTag() { return tag; }
        public String getName() { return name; }
        public FieldType getType() { return type; }
        public boolean isRequired() { return required; }
        
        @Override
        public String toString() {
            return String.format("%d(%s):%s%s", tag, name, type, required ? "*" : "");
        }
    }
    
    /**
     * FIX field data types
     */
    public enum FieldType {
        STRING,
        CHAR,
        INT,
        FLOAT,
        PRICE,
        QTY,
        BOOLEAN,
        SEQNUM,
        LENGTH,
        NUMINGROUP,
        UTCTIMESTAMP,
        UTCDATEONLY,
        UTCTIMEONLY,
        DATA
    }
}