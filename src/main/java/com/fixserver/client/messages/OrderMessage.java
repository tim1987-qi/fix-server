package com.fixserver.client.messages;

import com.fixserver.core.FIXMessage;
import com.fixserver.core.FIXMessageImpl;
import com.fixserver.protocol.MessageType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Utility class for creating FIX order messages.
 * 
 * Provides convenient methods for creating common order types
 * such as New Order Single, Order Cancel Request, etc.
 * 
 * @author FIX Server Team
 * @version 1.0
 * @since 1.0
 */
public class OrderMessage {
    
    /**
     * Order side enumeration.
     */
    public enum Side {
        BUY("1"),
        SELL("2");
        
        private final String code;
        
        Side(String code) {
            this.code = code;
        }
        
        public String getCode() {
            return code;
        }
    }
    
    /**
     * Order type enumeration.
     */
    public enum OrderType {
        MARKET("1"),
        LIMIT("2"),
        STOP("3"),
        STOP_LIMIT("4");
        
        private final String code;
        
        OrderType(String code) {
            this.code = code;
        }
        
        public String getCode() {
            return code;
        }
    }
    
    /**
     * Time in force enumeration.
     */
    public enum TimeInForce {
        DAY("0"),
        GOOD_TILL_CANCEL("1"),
        IMMEDIATE_OR_CANCEL("3"),
        FILL_OR_KILL("4");
        
        private final String code;
        
        TimeInForce(String code) {
            this.code = code;
        }
        
        public String getCode() {
            return code;
        }
    }
    
    /**
     * Creates a New Order Single message.
     * 
     * @param clientOrderId the client order ID
     * @param symbol the trading symbol
     * @param side the order side (buy/sell)
     * @param quantity the order quantity
     * @param orderType the order type
     * @param price the order price (null for market orders)
     * @param timeInForce the time in force
     * @return a new FIX message for the order
     */
    public static FIXMessage newOrderSingle(String clientOrderId, String symbol, Side side,
                                          BigDecimal quantity, OrderType orderType, 
                                          BigDecimal price, TimeInForce timeInForce) {
        FIXMessage message = new FIXMessageImpl();
        message.setField(FIXMessage.BEGIN_STRING, "FIX.4.4");
        message.setField(FIXMessage.MESSAGE_TYPE, MessageType.NEW_ORDER_SINGLE.getValue());
        
        // Required fields
        message.setField(11, clientOrderId);        // ClOrdID
        message.setField(55, symbol);               // Symbol
        message.setField(54, side.getCode());       // Side
        message.setField(38, quantity.toString());  // OrderQty
        message.setField(40, orderType.getCode());  // OrdType
        message.setField(59, timeInForce.getCode()); // TimeInForce
        message.setField(60, LocalDateTime.now().toString()); // TransactTime
        
        // Price (required for limit orders)
        if (price != null) {
            message.setField(44, price.toString()); // Price
        }
        
        return message;
    }
    
    /**
     * Creates a Market Order.
     * 
     * @param clientOrderId the client order ID
     * @param symbol the trading symbol
     * @param side the order side
     * @param quantity the order quantity
     * @return a market order message
     */
    public static FIXMessage marketOrder(String clientOrderId, String symbol, Side side, BigDecimal quantity) {
        return newOrderSingle(clientOrderId, symbol, side, quantity, OrderType.MARKET, null, TimeInForce.DAY);
    }
    
    /**
     * Creates a Limit Order.
     * 
     * @param clientOrderId the client order ID
     * @param symbol the trading symbol
     * @param side the order side
     * @param quantity the order quantity
     * @param price the limit price
     * @return a limit order message
     */
    public static FIXMessage limitOrder(String clientOrderId, String symbol, Side side, 
                                       BigDecimal quantity, BigDecimal price) {
        return newOrderSingle(clientOrderId, symbol, side, quantity, OrderType.LIMIT, price, TimeInForce.DAY);
    }
    
    /**
     * Creates an Order Cancel Request message.
     * 
     * @param clientOrderId the new client order ID for this cancel request
     * @param originalClientOrderId the original client order ID to cancel
     * @param symbol the trading symbol
     * @param side the original order side
     * @return an order cancel request message
     */
    public static FIXMessage orderCancelRequest(String clientOrderId, String originalClientOrderId,
                                               String symbol, Side side) {
        FIXMessage message = new FIXMessageImpl();
        message.setField(FIXMessage.BEGIN_STRING, "FIX.4.4");
        message.setField(FIXMessage.MESSAGE_TYPE, MessageType.ORDER_CANCEL_REQUEST.getValue());
        
        message.setField(11, clientOrderId);           // ClOrdID
        message.setField(41, originalClientOrderId);   // OrigClOrdID
        message.setField(55, symbol);                  // Symbol
        message.setField(54, side.getCode());          // Side
        message.setField(60, LocalDateTime.now().toString()); // TransactTime
        
        return message;
    }
    
    /**
     * Creates an Order Status Request message.
     * 
     * @param clientOrderId the client order ID for this status request
     * @param originalClientOrderId the original client order ID to query
     * @param symbol the trading symbol
     * @param side the order side
     * @return an order status request message
     */
    public static FIXMessage orderStatusRequest(String clientOrderId, String originalClientOrderId,
                                               String symbol, Side side) {
        FIXMessage message = new FIXMessageImpl();
        message.setField(FIXMessage.BEGIN_STRING, "FIX.4.4");
        message.setField(FIXMessage.MESSAGE_TYPE, MessageType.ORDER_STATUS_REQUEST.getValue());
        
        message.setField(11, clientOrderId);           // ClOrdID
        message.setField(41, originalClientOrderId);   // OrigClOrdID
        message.setField(55, symbol);                  // Symbol
        message.setField(54, side.getCode());          // Side
        
        return message;
    }
}