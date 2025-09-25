package com.fixserver.client.messages;

import com.fixserver.core.FIXMessage;
import com.fixserver.protocol.MessageType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OrderMessage utility class.
 * 
 * Tests the creation of various FIX order message types and validates
 * that all required fields are properly set.
 * 
 * @author FIX Server Team
 * @version 1.0
 * @since 1.0
 */
class OrderMessageTest {
    
    @Test
    void testNewOrderSingle() {
        String clientOrderId = "ORDER123";
        String symbol = "AAPL";
        OrderMessage.Side side = OrderMessage.Side.BUY;
        BigDecimal quantity = new BigDecimal("100");
        OrderMessage.OrderType orderType = OrderMessage.OrderType.LIMIT;
        BigDecimal price = new BigDecimal("150.50");
        OrderMessage.TimeInForce timeInForce = OrderMessage.TimeInForce.DAY;
        
        FIXMessage message = OrderMessage.newOrderSingle(clientOrderId, symbol, side, 
                quantity, orderType, price, timeInForce);
        
        assertNotNull(message);
        assertEquals(MessageType.NEW_ORDER_SINGLE.getValue(), message.getMessageType());
        assertEquals(clientOrderId, message.getField(11)); // ClOrdID
        assertEquals(symbol, message.getField(55)); // Symbol
        assertEquals(side.getCode(), message.getField(54)); // Side
        assertEquals(quantity.toString(), message.getField(38)); // OrderQty
        assertEquals(orderType.getCode(), message.getField(40)); // OrdType
        assertEquals(price.toString(), message.getField(44)); // Price
        assertEquals(timeInForce.getCode(), message.getField(59)); // TimeInForce
        assertNotNull(message.getField(60)); // TransactTime
    }
    
    @Test
    void testMarketOrder() {
        String clientOrderId = "MKT001";
        String symbol = "GOOGL";
        OrderMessage.Side side = OrderMessage.Side.SELL;
        BigDecimal quantity = new BigDecimal("50");
        
        FIXMessage message = OrderMessage.marketOrder(clientOrderId, symbol, side, quantity);
        
        assertNotNull(message);
        assertEquals(MessageType.NEW_ORDER_SINGLE.getValue(), message.getMessageType());
        assertEquals(clientOrderId, message.getField(11));
        assertEquals(symbol, message.getField(55));
        assertEquals(side.getCode(), message.getField(54));
        assertEquals(quantity.toString(), message.getField(38));
        assertEquals(OrderMessage.OrderType.MARKET.getCode(), message.getField(40));
        assertEquals(OrderMessage.TimeInForce.DAY.getCode(), message.getField(59));
        assertNull(message.getField(44)); // No price for market order
    }
    
    @Test
    void testLimitOrder() {
        String clientOrderId = "LMT001";
        String symbol = "MSFT";
        OrderMessage.Side side = OrderMessage.Side.BUY;
        BigDecimal quantity = new BigDecimal("200");
        BigDecimal price = new BigDecimal("300.25");
        
        FIXMessage message = OrderMessage.limitOrder(clientOrderId, symbol, side, quantity, price);
        
        assertNotNull(message);
        assertEquals(MessageType.NEW_ORDER_SINGLE.getValue(), message.getMessageType());
        assertEquals(clientOrderId, message.getField(11));
        assertEquals(symbol, message.getField(55));
        assertEquals(side.getCode(), message.getField(54));
        assertEquals(quantity.toString(), message.getField(38));
        assertEquals(OrderMessage.OrderType.LIMIT.getCode(), message.getField(40));
        assertEquals(price.toString(), message.getField(44));
        assertEquals(OrderMessage.TimeInForce.DAY.getCode(), message.getField(59));
    }
    
    @Test
    void testOrderCancelRequest() {
        String clientOrderId = "CANCEL001";
        String originalClientOrderId = "ORDER123";
        String symbol = "AAPL";
        OrderMessage.Side side = OrderMessage.Side.BUY;
        
        FIXMessage message = OrderMessage.orderCancelRequest(clientOrderId, originalClientOrderId, symbol, side);
        
        assertNotNull(message);
        assertEquals(MessageType.ORDER_CANCEL_REQUEST.getValue(), message.getMessageType());
        assertEquals(clientOrderId, message.getField(11)); // ClOrdID
        assertEquals(originalClientOrderId, message.getField(41)); // OrigClOrdID
        assertEquals(symbol, message.getField(55)); // Symbol
        assertEquals(side.getCode(), message.getField(54)); // Side
        assertNotNull(message.getField(60)); // TransactTime
    }
    
    @Test
    void testOrderStatusRequest() {
        String clientOrderId = "STATUS001";
        String originalClientOrderId = "ORDER123";
        String symbol = "AAPL";
        OrderMessage.Side side = OrderMessage.Side.BUY;
        
        FIXMessage message = OrderMessage.orderStatusRequest(clientOrderId, originalClientOrderId, symbol, side);
        
        assertNotNull(message);
        assertEquals(MessageType.ORDER_STATUS_REQUEST.getValue(), message.getMessageType());
        assertEquals(clientOrderId, message.getField(11)); // ClOrdID
        assertEquals(originalClientOrderId, message.getField(41)); // OrigClOrdID
        assertEquals(symbol, message.getField(55)); // Symbol
        assertEquals(side.getCode(), message.getField(54)); // Side
    }
    
    @Test
    void testSideEnum() {
        assertEquals("1", OrderMessage.Side.BUY.getCode());
        assertEquals("2", OrderMessage.Side.SELL.getCode());
    }
    
    @Test
    void testOrderTypeEnum() {
        assertEquals("1", OrderMessage.OrderType.MARKET.getCode());
        assertEquals("2", OrderMessage.OrderType.LIMIT.getCode());
        assertEquals("3", OrderMessage.OrderType.STOP.getCode());
        assertEquals("4", OrderMessage.OrderType.STOP_LIMIT.getCode());
    }
    
    @Test
    void testTimeInForceEnum() {
        assertEquals("0", OrderMessage.TimeInForce.DAY.getCode());
        assertEquals("1", OrderMessage.TimeInForce.GOOD_TILL_CANCEL.getCode());
        assertEquals("3", OrderMessage.TimeInForce.IMMEDIATE_OR_CANCEL.getCode());
        assertEquals("4", OrderMessage.TimeInForce.FILL_OR_KILL.getCode());
    }
    
    @Test
    void testNewOrderSingleWithoutPrice() {
        // Test market order (no price)
        FIXMessage message = OrderMessage.newOrderSingle("ORDER123", "AAPL", OrderMessage.Side.BUY,
                new BigDecimal("100"), OrderMessage.OrderType.MARKET, null, OrderMessage.TimeInForce.DAY);
        
        assertNotNull(message);
        assertNull(message.getField(44)); // No price field
        assertEquals(OrderMessage.OrderType.MARKET.getCode(), message.getField(40));
    }
    
    @Test
    void testDifferentTimeInForceOptions() {
        FIXMessage gtcOrder = OrderMessage.newOrderSingle("GTC001", "AAPL", OrderMessage.Side.BUY,
                new BigDecimal("100"), OrderMessage.OrderType.LIMIT, new BigDecimal("150.00"), 
                OrderMessage.TimeInForce.GOOD_TILL_CANCEL);
        
        assertEquals(OrderMessage.TimeInForce.GOOD_TILL_CANCEL.getCode(), gtcOrder.getField(59));
        
        FIXMessage iocOrder = OrderMessage.newOrderSingle("IOC001", "AAPL", OrderMessage.Side.BUY,
                new BigDecimal("100"), OrderMessage.OrderType.LIMIT, new BigDecimal("150.00"), 
                OrderMessage.TimeInForce.IMMEDIATE_OR_CANCEL);
        
        assertEquals(OrderMessage.TimeInForce.IMMEDIATE_OR_CANCEL.getCode(), iocOrder.getField(59));
    }
}