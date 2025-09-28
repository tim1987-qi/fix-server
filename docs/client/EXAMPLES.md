# FIX Client Examples

## 📋 Overview

This document provides comprehensive examples of using the FIX Client library for various trading scenarios. Each example includes complete, runnable code with explanations and best practices.

## 🚀 Basic Examples

### 1. Simple Market Order Client

```java
package com.example.fixclient;

import com.fixserver.client.*;
import com.fixserver.client.messages.OrderMessage;
import com.fixserver.core.FIXMessage;
import com.fixserver.protocol.FIXTags;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SimpleMarketOrderClient {
    
    public static void main(String[] args) throws Exception {
        // Create client configuration
        FIXClientConfiguration config = FIXClientConfiguration.builder()
            .host("localhost")
            .port(9878)
            .senderCompId("SIMPLE_CLIENT")
            .targetCompId("SERVER1")
            .heartbeatInterval(30)
            .build();
        
        // Create client
        FIXClient client = FIXClientFactory.createClient(config);
        
        // Set up connection handler
        client.setConnectionHandler(new FIXClientConnectionHandler() {
            @Override
            public void onConnect(FIXClient client) {
                System.out.println("✓ Connected to FIX server");
            }
            
            @Override
            public void onDisconnect(FIXClient client) {
                System.out.println("✗ Disconnected from FIX server");
            }
            
            @Override
            public void onError(FIXClient client, Throwable error) {
                System.err.println("✗ Connection error: " + error.getMessage());
            }
        });
        
        // Set up message handler
        client.setMessageHandler(new FIXClientMessageHandler() {
            @Override
            public void onMessage(FIXClient client, FIXMessage message) {
                System.out.println("📨 Received: " + message.toFixString());
                
                if ("8".equals(message.getMessageType())) { // Execution Report
                    handleExecutionReport(message);
                }
            }
            
            private void handleExecutionReport(FIXMessage message) {
                String orderId = message.getField(FIXTags.ORDER_ID);
                String execType = message.getField(FIXTags.EXEC_TYPE);
                String orderStatus = message.getField(FIXTags.ORDER_STATUS);
                String symbol = message.getField(FIXTags.SYMBOL);
                
                System.out.printf("📊 Execution Report - Order: %s, Symbol: %s, ExecType: %s, Status: %s%n",
                                 orderId, symbol, execType, orderStatus);
            }
        });
        
        try {
            // Connect to server
            System.out.println("🔌 Connecting to FIX server...");
            client.connect().get(30, TimeUnit.SECONDS);
            
            // Send a market buy order
            System.out.println("📤 Sending market buy order...");
            FIXMessage buyOrder = OrderMessage.marketOrder("BUY_001", "AAPL", 100, OrderSide.BUY)
                .toFIXMessage();
            client.sendMessage(buyOrder).get(5, TimeUnit.SECONDS);
            
            // Send a market sell order
            System.out.println("📤 Sending market sell order...");
            FIXMessage sellOrder = OrderMessage.marketOrder("SELL_001", "GOOGL", 50, OrderSide.SELL)
                .toFIXMessage();
            client.sendMessage(sellOrder).get(5, TimeUnit.SECONDS);
            
            // Keep running to receive responses
            System.out.println("⏳ Waiting for responses (30 seconds)...");
            Thread.sleep(30000);
            
        } finally {
            // Clean shutdown
            System.out.println("🔌 Disconnecting...");
            client.disconnect().get(10, TimeUnit.SECONDS);
            client.shutdown();
            System.out.println("✓ Client shutdown complete");
        }
    }
}
```

### 2. Limit Order Client

```java
package com.example.fixclient;

import com.fixserver.client.*;
import com.fixserver.client.messages.OrderMessage;
import com.fixserver.core.FIXMessage;
import com.fixserver.protocol.FIXTags;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class LimitOrderClient {
    
    private static FIXClient client;
    private static boolean running = true;
    
    public static void main(String[] args) throws Exception {
        setupClient();
        runInteractiveSession();
    }
    
    private static void setupClient() throws Exception {
        FIXClientConfiguration config = FIXClientConfiguration.builder()
            .host("localhost")
            .port(9878)
            .senderCompId("LIMIT_CLIENT")
            .targetCompId("SERVER1")
            .build();
        
        client = FIXClientFactory.createClient(config);
        
        client.setConnectionHandler(new FIXClientConnectionHandler() {
            @Override
            public void onConnect(FIXClient client) {
                System.out.println("✓ Connected to FIX server");
                System.out.println("📝 You can now enter limit orders");
                printHelp();
            }
            
            @Override
            public void onDisconnect(FIXClient client) {
                System.out.println("✗ Disconnected from FIX server");
                running = false;
            }
            
            @Override
            public void onError(FIXClient client, Throwable error) {
                System.err.println("✗ Error: " + error.getMessage());
            }
        });
        
        client.setMessageHandler(new FIXClientMessageHandler() {
            @Override
            public void onMessage(FIXClient client, FIXMessage message) {
                handleMessage(message);
            }
        });
        
        client.connect().get(30, TimeUnit.SECONDS);
    }
    
    private static void runInteractiveSession() {
        Scanner scanner = new Scanner(System.in);
        
        while (running) {
            System.out.print("📝 Enter command (or 'help'): ");
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) continue;
            
            String[] parts = input.split("\\s+");
            String command = parts[0].toLowerCase();
            
            try {
                switch (command) {
                    case "buy":
                        if (parts.length >= 4) {
                            sendLimitOrder(parts[1], parts[2], Double.parseDouble(parts[3]), OrderSide.BUY);
                        } else {
                            System.out.println("❌ Usage: buy <symbol> <quantity> <price>");
                        }
                        break;
                    case "sell":
                        if (parts.length >= 4) {
                            sendLimitOrder(parts[1], parts[2], Double.parseDouble(parts[3]), OrderSide.SELL);
                        } else {
                            System.out.println("❌ Usage: sell <symbol> <quantity> <price>");
                        }
                        break;
                    case "help":
                        printHelp();
                        break;
                    case "quit":
                    case "exit":
                        running = false;
                        break;
                    default:
                        System.out.println("❌ Unknown command: " + command);
                        printHelp();
                }
            } catch (Exception e) {
                System.err.println("❌ Error processing command: " + e.getMessage());
            }
        }
        
        // Cleanup
        try {
            client.disconnect().get(5, TimeUnit.SECONDS);
            client.shutdown();
        } catch (Exception e) {
            System.err.println("Error during shutdown: " + e.getMessage());
        }
        
        scanner.close();
    }
    
    private static void sendLimitOrder(String symbol, String quantityStr, double price, OrderSide side) {
        try {
            int quantity = Integer.parseInt(quantityStr);
            String orderId = generateOrderId(symbol, side);
            
            FIXMessage limitOrder = OrderMessage.limitOrder(orderId, symbol, quantity, side, price)
                .toFIXMessage();
            
            client.sendMessage(limitOrder).get(5, TimeUnit.SECONDS);
            
            System.out.printf("📤 Sent %s limit order: %s %d shares of %s at $%.2f%n",
                             side.name().toLowerCase(), orderId, quantity, symbol, price);
            
        } catch (Exception e) {
            System.err.println("❌ Failed to send order: " + e.getMessage());
        }
    }
    
    private static String generateOrderId(String symbol, OrderSide side) {
        return String.format("%s_%s_%d", symbol, side.name(), System.currentTimeMillis());
    }
    
    private static void handleMessage(FIXMessage message) {
        String messageType = message.getMessageType();
        
        switch (messageType) {
            case "8": // Execution Report
                handleExecutionReport(message);
                break;
            case "9": // Order Cancel Reject
                handleOrderCancelReject(message);
                break;
            case "0": // Heartbeat
                // Ignore heartbeats in interactive mode
                break;
            default:
                System.out.println("📨 Received " + messageType + ": " + message.toFixString());
        }
    }
    
    private static void handleExecutionReport(FIXMessage message) {
        String orderId = message.getField(FIXTags.ORDER_ID);
        String symbol = message.getField(FIXTags.SYMBOL);
        String side = message.getField(FIXTags.SIDE);
        String execType = message.getField(FIXTags.EXEC_TYPE);
        String orderStatus = message.getField(FIXTags.ORDER_STATUS);
        String lastQty = message.getField(FIXTags.LAST_QTY);
        String lastPx = message.getField(FIXTags.LAST_PX);
        
        System.out.printf("📊 Execution Report - Order: %s, Symbol: %s, Side: %s, Status: %s%n",
                         orderId, symbol, "1".equals(side) ? "BUY" : "SELL", orderStatus);
        
        if (lastQty != null && lastPx != null) {
            System.out.printf("   💰 Executed: %s shares at $%s%n", lastQty, lastPx);
        }
    }
    
    private static void handleOrderCancelReject(FIXMessage message) {
        String orderId = message.getField(FIXTags.ORDER_ID);
        String reason = message.getField(FIXTags.TEXT);
        
        System.out.printf("❌ Order Cancel Reject - Order: %s, Reason: %s%n", orderId, reason);
    }
    
    private static void printHelp() {
        System.out.println("\n📋 Available commands:");
        System.out.println("  buy <symbol> <quantity> <price>   - Send limit buy order");
        System.out.println("  sell <symbol> <quantity> <price>  - Send limit sell order");
        System.out.println("  help                              - Show this help");
        System.out.println("  quit/exit                         - Exit the client");
        System.out.println("\n💡 Example: buy AAPL 100 150.50");
        System.out.println();
    }
}
```

## 🏢 Advanced Examples

### 3. Multi-Symbol Trading Client

```java
package com.example.fixclient;

import com.fixserver.client.*;
import com.fixserver.client.messages.OrderMessage;
import com.fixserver.core.FIXMessage;
import com.fixserver.protocol.FIXTags;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.Arrays;
import java.util.Random;

public class MultiSymbolTradingClient {
    
    private static final List<String> SYMBOLS = Arrays.asList(
        "AAPL", "GOOGL", "MSFT", "AMZN", "TSLA", "META", "NVDA", "NFLX"
    );
    
    private static final Random random = new Random();
    private static final AtomicInteger orderCounter = new AtomicInteger(0);
    
    private FIXClient client;
    private ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<String, OrderInfo> activeOrders = new ConcurrentHashMap<>();
    
    public static void main(String[] args) throws Exception {
        MultiSymbolTradingClient tradingClient = new MultiSymbolTradingClient();
        tradingClient.start();
        
        // Run for 2 minutes
        Thread.sleep(120000);
        
        tradingClient.stop();
    }
    
    public void start() throws Exception {
        setupClient();
        startTradingStrategy();
    }
    
    public void stop() throws Exception {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        
        if (client != null) {
            client.disconnect().get(10, TimeUnit.SECONDS);
            client.shutdown();
        }
        
        System.out.println("📊 Final Statistics:");
        System.out.println("   Active Orders: " + activeOrders.size());
        activeOrders.values().forEach(order -> 
            System.out.println("   - " + order.orderId + ": " + order.symbol + " " + order.status));
    }
    
    private void setupClient() throws Exception {
        FIXClientConfiguration config = FIXClientConfiguration.builder()
            .host("localhost")
            .port(9879) // Use Netty server for better performance
            .senderCompId("MULTI_SYMBOL_CLIENT")
            .targetCompId("SERVER1")
            .heartbeatInterval(30)
            .build();
        
        client = FIXClientFactory.createClient(config);
        
        client.setConnectionHandler(new FIXClientConnectionHandler() {
            @Override
            public void onConnect(FIXClient client) {
                System.out.println("✓ Multi-symbol trading client connected");
            }
            
            @Override
            public void onDisconnect(FIXClient client) {
                System.out.println("✗ Multi-symbol trading client disconnected");
            }
            
            @Override
            public void onError(FIXClient client, Throwable error) {
                System.err.println("✗ Trading client error: " + error.getMessage());
            }
        });
        
        client.setMessageHandler(this::handleMessage);
        
        client.connect().get(30, TimeUnit.SECONDS);
    }
    
    private void startTradingStrategy() {
        scheduler = Executors.newScheduledThreadPool(4);
        
        // Send random orders every 5 seconds
        scheduler.scheduleAtFixedRate(this::sendRandomOrder, 0, 5, TimeUnit.SECONDS);
        
        // Cancel some orders every 15 seconds
        scheduler.scheduleAtFixedRate(this::cancelRandomOrders, 10, 15, TimeUnit.SECONDS);
        
        // Print statistics every 30 seconds
        scheduler.scheduleAtFixedRate(this::printStatistics, 30, 30, TimeUnit.SECONDS);
    }
    
    private void sendRandomOrder() {
        try {
            String symbol = SYMBOLS.get(random.nextInt(SYMBOLS.size()));
            OrderSide side = random.nextBoolean() ? OrderSide.BUY : OrderSide.SELL;
            int quantity = (random.nextInt(10) + 1) * 100; // 100-1000 shares
            double basePrice = getBasePrice(symbol);
            double price = basePrice + (random.nextGaussian() * basePrice * 0.02); // ±2% variation
            
            String orderId = generateOrderId(symbol);
            
            FIXMessage order = OrderMessage.limitOrder(orderId, symbol, quantity, side, price)
                .toFIXMessage();
            
            client.sendMessage(order);
            
            // Track the order
            activeOrders.put(orderId, new OrderInfo(orderId, symbol, side, quantity, price, "NEW"));
            
            System.out.printf("📤 Sent %s order: %s %d %s @ $%.2f%n",
                             side.name(), orderId, quantity, symbol, price);
            
        } catch (Exception e) {
            System.err.println("❌ Failed to send random order: " + e.getMessage());
        }
    }
    
    private void cancelRandomOrders() {
        if (activeOrders.isEmpty()) return;
        
        // Cancel up to 2 random active orders
        List<String> orderIds = activeOrders.keySet().stream()
            .filter(id -> "NEW".equals(activeOrders.get(id).status) || 
                         "PARTIALLY_FILLED".equals(activeOrders.get(id).status))
            .limit(2)
            .collect(java.util.stream.Collectors.toList());
        
        for (String orderId : orderIds) {
            cancelOrder(orderId);
        }
    }
    
    private void cancelOrder(String orderId) {
        try {
            OrderInfo orderInfo = activeOrders.get(orderId);
            if (orderInfo == null) return;
            
            FIXMessage cancelRequest = FIXMessage.builder()
                .version("FIX.4.4")
                .messageType("F") // Order Cancel Request
                .field(FIXTags.ORIG_CL_ORD_ID, orderId)
                .field(FIXTags.CL_ORD_ID, generateOrderId(orderInfo.symbol))
                .field(FIXTags.SYMBOL, orderInfo.symbol)
                .field(FIXTags.SIDE, orderInfo.side == OrderSide.BUY ? "1" : "2")
                .build();
            
            client.sendMessage(cancelRequest);
            
            System.out.printf("🚫 Sent cancel request for order: %s%n", orderId);
            
        } catch (Exception e) {
            System.err.println("❌ Failed to cancel order " + orderId + ": " + e.getMessage());
        }
    }
    
    private void handleMessage(FIXClient client, FIXMessage message) {
        String messageType = message.getMessageType();
        
        switch (messageType) {
            case "8": // Execution Report
                handleExecutionReport(message);
                break;
            case "9": // Order Cancel Reject
                handleOrderCancelReject(message);
                break;
            case "0": // Heartbeat
                // Ignore heartbeats
                break;
            default:
                System.out.println("📨 Received " + messageType + ": " + message.toFixString());
        }
    }
    
    private void handleExecutionReport(FIXMessage message) {
        String orderId = message.getField(FIXTags.ORDER_ID);
        String execType = message.getField(FIXTags.EXEC_TYPE);
        String orderStatus = message.getField(FIXTags.ORDER_STATUS);
        String symbol = message.getField(FIXTags.SYMBOL);
        
        OrderInfo orderInfo = activeOrders.get(orderId);
        if (orderInfo != null) {
            orderInfo.status = orderStatus;
            
            if ("2".equals(orderStatus) || "8".equals(orderStatus)) { // Filled or Rejected
                activeOrders.remove(orderId);
            }
        }
        
        System.out.printf("📊 Execution Report - %s: %s (%s)%n", orderId, symbol, orderStatus);
        
        // Handle fills
        String lastQty = message.getField(FIXTags.LAST_QTY);
        String lastPx = message.getField(FIXTags.LAST_PX);
        if (lastQty != null && lastPx != null) {
            System.out.printf("   💰 Fill: %s shares @ $%s%n", lastQty, lastPx);
        }
    }
    
    private void handleOrderCancelReject(FIXMessage message) {
        String orderId = message.getField(FIXTags.ORDER_ID);
        String reason = message.getField(FIXTags.TEXT);
        
        System.out.printf("❌ Cancel Reject - %s: %s%n", orderId, reason);
    }
    
    private void printStatistics() {
        System.out.println("\n📊 Trading Statistics:");
        System.out.println("   Active Orders: " + activeOrders.size());
        
        long newOrders = activeOrders.values().stream()
            .mapToLong(order -> "NEW".equals(order.status) ? 1 : 0)
            .sum();
        
        long partiallyFilled = activeOrders.values().stream()
            .mapToLong(order -> "PARTIALLY_FILLED".equals(order.status) ? 1 : 0)
            .sum();
        
        System.out.println("   New: " + newOrders + ", Partially Filled: " + partiallyFilled);
        System.out.println();
    }
    
    private String generateOrderId(String symbol) {
        return String.format("%s_%d_%d", symbol, orderCounter.incrementAndGet(), System.currentTimeMillis());
    }
    
    private double getBasePrice(String symbol) {
        // Simulate base prices for different symbols
        switch (symbol) {
            case "AAPL": return 150.0;
            case "GOOGL": return 2800.0;
            case "MSFT": return 300.0;
            case "AMZN": return 3200.0;
            case "TSLA": return 800.0;
            case "META": return 250.0;
            case "NVDA": return 450.0;
            case "NFLX": return 400.0;
            default: return 100.0;
        }
    }
    
    private static class OrderInfo {
        final String orderId;
        final String symbol;
        final OrderSide side;
        final int quantity;
        final double price;
        String status;
        
        OrderInfo(String orderId, String symbol, OrderSide side, int quantity, double price, String status) {
            this.orderId = orderId;
            this.symbol = symbol;
            this.side = side;
            this.quantity = quantity;
            this.price = price;
            this.status = status;
        }
    }
}
```

### 4. High-Frequency Trading Client

```java
package com.example.fixclient;

import com.fixserver.client.*;
import com.fixserver.client.messages.OrderMessage;
import com.fixserver.core.FIXMessage;
import com.fixserver.protocol.FIXTags;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class HighFrequencyTradingClient {
    
    private static final int TARGET_MESSAGES_PER_SECOND = 1000;
    private static final int BURST_SIZE = 10;
    private static final String SYMBOL = "AAPL";
    
    private FIXClient client;
    private ScheduledExecutorService scheduler;
    private final AtomicLong messagesSent = new AtomicLong(0);
    private final AtomicLong messagesReceived = new AtomicLong(0);
    private final AtomicLong totalLatency = new AtomicLong(0);
    private final AtomicReference<Double> currentPrice = new AtomicReference<>(150.0);
    
    public static void main(String[] args) throws Exception {
        HighFrequencyTradingClient hftClient = new HighFrequencyTradingClient();
        hftClient.start();
        
        // Run for 1 minute
        Thread.sleep(60000);
        
        hftClient.stop();
    }
    
    public void start() throws Exception {
        setupHighPerformanceClient();
        startHighFrequencyTrading();
    }
    
    public void stop() throws Exception {
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        }
        
        if (client != null) {
            client.disconnect().get(5, TimeUnit.SECONDS);
            client.shutdown();
        }
        
        printFinalStatistics();
    }
    
    private void setupHighPerformanceClient() throws Exception {
        FIXClientConfiguration config = FIXClientConfiguration.builder()
            .host("localhost")
            .port(9879) // Netty server for high performance
            .senderCompId("HFT_CLIENT")
            .targetCompId("SERVER1")
            .heartbeatInterval(30)
            .connectionTimeout(5000)
            .build();
        
        client = FIXClientFactory.createClient(config);
        
        client.setConnectionHandler(new FIXClientConnectionHandler() {
            @Override
            public void onConnect(FIXClient client) {
                System.out.println("🚀 High-frequency trading client connected");
            }
            
            @Override
            public void onDisconnect(FIXClient client) {
                System.out.println("🛑 High-frequency trading client disconnected");
            }
            
            @Override
            public void onError(FIXClient client, Throwable error) {
                System.err.println("⚠️ HFT client error: " + error.getMessage());
            }
        });
        
        // Optimized message handler for minimal latency
        client.setMessageHandler(this::handleMessageOptimized);
        
        client.connect().get(10, TimeUnit.SECONDS);
    }
    
    private void startHighFrequencyTrading() {
        scheduler = Executors.newScheduledThreadPool(8);
        
        // Calculate interval for target message rate
        long intervalNanos = 1_000_000_000L / TARGET_MESSAGES_PER_SECOND;
        
        // Send bursts of orders at calculated intervals
        scheduler.scheduleAtFixedRate(this::sendOrderBurst, 0, intervalNanos, TimeUnit.NANOSECONDS);
        
        // Update price every 100ms
        scheduler.scheduleAtFixedRate(this::updatePrice, 0, 100, TimeUnit.MILLISECONDS);
        
        // Print statistics every 10 seconds
        scheduler.scheduleAtFixedRate(this::printStatistics, 10, 10, TimeUnit.SECONDS);
    }
    
    private void sendOrderBurst() {
        try {
            double price = currentPrice.get();
            
            for (int i = 0; i < BURST_SIZE; i++) {
                // Alternate between buy and sell orders
                OrderSide side = (i % 2 == 0) ? OrderSide.BUY : OrderSide.SELL;
                
                // Slightly adjust price for each order
                double orderPrice = price + (side == OrderSide.BUY ? -0.01 : 0.01) * (i + 1);
                
                String orderId = generateHighFrequencyOrderId();
                
                // Create order with timestamp for latency measurement
                FIXMessage order = OrderMessage.limitOrder(orderId, SYMBOL, 100, side, orderPrice)
                    .toFIXMessage();
                
                // Add timestamp for latency calculation
                order.setField(FIXTags.SENDING_TIME, String.valueOf(System.nanoTime()));
                
                // Send asynchronously for maximum throughput
                client.sendMessage(order);
                messagesSent.incrementAndGet();
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error in order burst: " + e.getMessage());
        }
    }
    
    private void handleMessageOptimized(FIXClient client, FIXMessage message) {
        long receiveTime = System.nanoTime();
        messagesReceived.incrementAndGet();
        
        // Calculate latency if timestamp is present
        String sendTimeStr = message.getField(FIXTags.SENDING_TIME);
        if (sendTimeStr != null) {
            try {
                long sendTime = Long.parseLong(sendTimeStr);
                long latency = receiveTime - sendTime;
                totalLatency.addAndGet(latency);
            } catch (NumberFormatException e) {
                // Ignore invalid timestamps
            }
        }
        
        // Minimal processing for maximum throughput
        String messageType = message.getMessageType();
        if ("8".equals(messageType)) { // Execution Report
            handleExecutionReportFast(message);
        }
        // Ignore other message types for performance
    }
    
    private void handleExecutionReportFast(FIXMessage message) {
        // Ultra-fast execution report processing
        String orderStatus = message.getField(FIXTags.ORDER_STATUS);
        
        // Only log fills to minimize overhead
        if ("2".equals(orderStatus)) { // Filled
            String orderId = message.getField(FIXTags.ORDER_ID);
            String lastQty = message.getField(FIXTags.LAST_QTY);
            String lastPx = message.getField(FIXTags.LAST_PX);
            
            // Minimal logging for performance
            if (messagesSent.get() % 1000 == 0) { // Log every 1000th fill
                System.out.printf("💰 Fill #%d: %s shares @ $%s%n", 
                                 messagesReceived.get(), lastQty, lastPx);
            }
        }
    }
    
    private void updatePrice() {
        // Simulate price movement
        double current = currentPrice.get();
        double change = (Math.random() - 0.5) * 0.1; // ±$0.05 change
        double newPrice = Math.max(100.0, Math.min(200.0, current + change));
        currentPrice.set(newPrice);
    }
    
    private void printStatistics() {
        long sent = messagesSent.get();
        long received = messagesReceived.get();
        long totalLatencyNanos = totalLatency.get();
        
        double avgLatencyMicros = received > 0 ? (totalLatencyNanos / (double) received) / 1000.0 : 0;
        double messagesPerSecond = sent / 10.0; // 10-second interval
        
        System.out.printf("📊 HFT Stats - Sent: %d, Received: %d, Rate: %.1f msg/s, Avg Latency: %.2f μs%n",
                         sent, received, messagesPerSecond, avgLatencyMicros);
    }
    
    private void printFinalStatistics() {
        long sent = messagesSent.get();
        long received = messagesReceived.get();
        long totalLatencyNanos = totalLatency.get();
        
        double avgLatencyMicros = received > 0 ? (totalLatencyNanos / (double) received) / 1000.0 : 0;
        double totalRate = sent / 60.0; // 1-minute test
        
        System.out.println("\n🏁 Final HFT Statistics:");
        System.out.printf("   Messages Sent: %d%n", sent);
        System.out.printf("   Messages Received: %d%n", received);
        System.out.printf("   Average Rate: %.1f messages/second%n", totalRate);
        System.out.printf("   Average Latency: %.2f microseconds%n", avgLatencyMicros);
        System.out.printf("   Success Rate: %.2f%%%n", (received / (double) sent) * 100);
    }
    
    private String generateHighFrequencyOrderId() {
        return String.format("HFT_%d_%d", 
                           Thread.currentThread().getId(), 
                           System.nanoTime());
    }
}
```

## 🔄 Reconnection and Resilience Examples

### 5. Resilient Client with Auto-Reconnection

```java
package com.example.fixclient;

import com.fixserver.client.*;
import com.fixserver.client.messages.OrderMessage;
import com.fixserver.core.FIXMessage;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Queue;

public class ResilientFIXClient {
    
    private FIXClient client;
    private final FIXClientConfiguration config;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private final Queue<FIXMessage> messageQueue = new ConcurrentLinkedQueue<>();
    
    private static final int MAX_RECONNECT_ATTEMPTS = 10;
    private static final int RECONNECT_DELAY_SECONDS = 5;
    private static final int MAX_QUEUE_SIZE = 1000;
    
    public ResilientFIXClient() {
        this.config = FIXClientConfiguration.builder()
            .host("localhost")
            .port(9878)
            .senderCompId("RESILIENT_CLIENT")
            .targetCompId("SERVER1")
            .heartbeatInterval(30)
            .connectionTimeout(10000)
            .build();
    }
    
    public static void main(String[] args) throws Exception {
        ResilientFIXClient resilientClient = new ResilientFIXClient();
        resilientClient.start();
        
        // Simulate trading activity
        resilientClient.simulateTrading();
        
        // Run for 5 minutes
        Thread.sleep(300000);
        
        resilientClient.stop();
    }
    
    public void start() throws Exception {
        createClient();
        connect();
        startMessageProcessor();
    }
    
    public void stop() throws Exception {
        scheduler.shutdown();
        
        if (client != null) {
            try {
                client.disconnect().get(5, TimeUnit.SECONDS);
                client.shutdown();
            } catch (Exception e) {
                System.err.println("Error during shutdown: " + e.getMessage());
            }
        }
        
        System.out.println("✓ Resilient client stopped");
    }
    
    private void createClient() {
        client = FIXClientFactory.createClient(config);
        
        client.setConnectionHandler(new FIXClientConnectionHandler() {
            @Override
            public void onConnect(FIXClient client) {
                System.out.println("✅ Connected to FIX server");
                connected.set(true);
                reconnectAttempts.set(0);
                
                // Process queued messages
                processQueuedMessages();
            }
            
            @Override
            public void onDisconnect(FIXClient client) {
                System.out.println("❌ Disconnected from FIX server");
                connected.set(false);
                
                // Attempt reconnection
                scheduleReconnect();
            }
            
            @Override
            public void onError(FIXClient client, Throwable error) {
                System.err.println("⚠️ Connection error: " + error.getMessage());
                connected.set(false);
                
                // Attempt reconnection on error
                scheduleReconnect();
            }
        });
        
        client.setMessageHandler(this::handleMessage);
    }
    
    private void connect() {
        try {
            System.out.println("🔌 Connecting to FIX server...");
            client.connect().get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("❌ Initial connection failed: " + e.getMessage());
            scheduleReconnect();
        }
    }
    
    private void scheduleReconnect() {
        int attempts = reconnectAttempts.incrementAndGet();
        
        if (attempts <= MAX_RECONNECT_ATTEMPTS) {
            int delay = RECONNECT_DELAY_SECONDS * attempts; // Exponential backoff
            
            System.out.printf("🔄 Scheduling reconnect attempt %d/%d in %d seconds%n",
                             attempts, MAX_RECONNECT_ATTEMPTS, delay);
            
            scheduler.schedule(this::attemptReconnect, delay, TimeUnit.SECONDS);
        } else {
            System.err.println("💀 Max reconnect attempts reached, giving up");
        }
    }
    
    private void attemptReconnect() {
        if (connected.get()) {
            return; // Already connected
        }
        
        try {
            System.out.println("🔄 Attempting to reconnect...");
            
            // Create new client instance
            createClient();
            
            // Attempt connection
            client.connect().get(15, TimeUnit.SECONDS);
            
        } catch (Exception e) {
            System.err.printf("❌ Reconnect attempt %d failed: %s%n", 
                             reconnectAttempts.get(), e.getMessage());
            
            // Schedule next attempt
            scheduleReconnect();
        }
    }
    
    public void sendMessage(FIXMessage message) {
        if (connected.get()) {
            try {
                client.sendMessage(message).get(5, TimeUnit.SECONDS);
                System.out.println("📤 Message sent successfully");
            } catch (Exception e) {
                System.err.println("❌ Failed to send message: " + e.getMessage());
                queueMessage(message);
            }
        } else {
            queueMessage(message);
        }
    }
    
    private void queueMessage(FIXMessage message) {
        if (messageQueue.size() < MAX_QUEUE_SIZE) {
            messageQueue.offer(message);
            System.out.printf("📥 Message queued (queue size: %d)%n", messageQueue.size());
        } else {
            System.err.println("❌ Message queue full, dropping message");
        }
    }
    
    private void processQueuedMessages() {
        if (messageQueue.isEmpty()) {
            return;
        }
        
        System.out.printf("📤 Processing %d queued messages%n", messageQueue.size());
        
        while (!messageQueue.isEmpty() && connected.get()) {
            FIXMessage message = messageQueue.poll();
            if (message != null) {
                try {
                    client.sendMessage(message).get(1, TimeUnit.SECONDS);
                } catch (Exception e) {
                    System.err.println("❌ Failed to send queued message: " + e.getMessage());
                    // Re-queue the message
                    messageQueue.offer(message);
                    break;
                }
            }
        }
        
        System.out.println("✅ Finished processing queued messages");
    }
    
    private void startMessageProcessor() {
        // Process any queued messages periodically
        scheduler.scheduleAtFixedRate(() -> {
            if (connected.get() && !messageQueue.isEmpty()) {
                processQueuedMessages();
            }
        }, 10, 10, TimeUnit.SECONDS);
    }
    
    private void handleMessage(FIXClient client, FIXMessage message) {
        System.out.println("📨 Received: " + message.getMessageType());
        
        // Handle different message types
        String messageType = message.getMessageType();
        switch (messageType) {
            case "8": // Execution Report
                System.out.println("📊 Execution Report received");
                break;
            case "9": // Order Cancel Reject
                System.out.println("❌ Order Cancel Reject received");
                break;
            default:
                System.out.println("📨 Other message type: " + messageType);
        }
    }
    
    private void simulateTrading() {
        // Send orders periodically to test resilience
        scheduler.scheduleAtFixedRate(() -> {
            try {
                FIXMessage order = OrderMessage.marketOrder(
                    "RESILIENT_" + System.currentTimeMillis(),
                    "AAPL",
                    100,
                    Math.random() > 0.5 ? OrderSide.BUY : OrderSide.SELL
                ).toFIXMessage();
                
                sendMessage(order);
                
            } catch (Exception e) {
                System.err.println("❌ Error in trading simulation: " + e.getMessage());
            }
        }, 5, 10, TimeUnit.SECONDS);
    }
}
```

## 🧪 Testing Examples

### 6. Client Testing Framework

```java
package com.example.fixclient.test;

import com.fixserver.client.*;
import com.fixserver.client.messages.OrderMessage;
import com.fixserver.core.FIXMessage;
import com.fixserver.protocol.FIXTags;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;
import java.util.ArrayList;

public class FIXClientTestFramework {
    
    public static void main(String[] args) throws Exception {
        FIXClientTestFramework testFramework = new FIXClientTestFramework();
        testFramework.runAllTests();
    }
    
    public void runAllTests() throws Exception {
        System.out.println("🧪 Starting FIX Client Test Suite");
        
        boolean allPassed = true;
        
        allPassed &= testBasicConnection();
        allPassed &= testMessageSending();
        allPassed &= testMessageReceiving();
        allPassed &= testReconnection();
        allPassed &= testConcurrentClients();
        allPassed &= testLatencyMeasurement();
        
        System.out.println("\n" + (allPassed ? "✅ All tests passed!" : "❌ Some tests failed!"));
    }
    
    private boolean testBasicConnection() {
        System.out.println("\n🔌 Testing Basic Connection...");
        
        try {
            FIXClientConfiguration config = FIXClientConfiguration.defaultConfig("TEST_CLIENT", "SERVER1");
            FIXClient client = FIXClientFactory.createClient(config);
            
            CountDownLatch connectLatch = new CountDownLatch(1);
            AtomicReference<Throwable> error = new AtomicReference<>();
            
            client.setConnectionHandler(new FIXClientConnectionHandler() {
                @Override
                public void onConnect(FIXClient client) {
                    connectLatch.countDown();
                }
                
                @Override
                public void onDisconnect(FIXClient client) {}
                
                @Override
                public void onError(FIXClient client, Throwable throwable) {
                    error.set(throwable);
                    connectLatch.countDown();
                }
            });
            
            client.connect();
            
            boolean connected = connectLatch.await(30, TimeUnit.SECONDS);
            
            if (!connected) {
                System.out.println("❌ Connection timeout");
                return false;
            }
            
            if (error.get() != null) {
                System.out.println("❌ Connection error: " + error.get().getMessage());
                return false;
            }
            
            if (!client.isConnected()) {
                System.out.println("❌ Client reports not connected");
                return false;
            }
            
            client.disconnect().get(5, TimeUnit.SECONDS);
            client.shutdown();
            
            System.out.println("✅ Basic connection test passed");
            return true;
            
        } catch (Exception e) {
            System.out.println("❌ Basic connection test failed: " + e.getMessage());
            return false;
        }
    }
    
    private boolean testMessageSending() {
        System.out.println("\n📤 Testing Message Sending...");
        
        try {
            FIXClient client = createTestClient("SEND_TEST_CLIENT");
            
            CountDownLatch connectLatch = new CountDownLatch(1);
            client.setConnectionHandler(new SimpleConnectionHandler(connectLatch));
            
            client.connect();
            connectLatch.await(30, TimeUnit.SECONDS);
            
            // Send multiple message types
            FIXMessage marketOrder = OrderMessage.marketOrder("SEND_TEST_1", "AAPL", 100, OrderSide.BUY)
                .toFIXMessage();
            
            FIXMessage limitOrder = OrderMessage.limitOrder("SEND_TEST_2", "GOOGL", 50, OrderSide.SELL, 2800.0)
                .toFIXMessage();
            
            // Test synchronous sending
            client.sendMessage(marketOrder).get(5, TimeUnit.SECONDS);
            client.sendMessage(limitOrder).get(5, TimeUnit.SECONDS);
            
            client.disconnect().get(5, TimeUnit.SECONDS);
            client.shutdown();
            
            System.out.println("✅ Message sending test passed");
            return true;
            
        } catch (Exception e) {
            System.out.println("❌ Message sending test failed: " + e.getMessage());
            return false;
        }
    }
    
    private boolean testMessageReceiving() {
        System.out.println("\n📥 Testing Message Receiving...");
        
        try {
            FIXClient client = createTestClient("RECEIVE_TEST_CLIENT");
            
            CountDownLatch connectLatch = new CountDownLatch(1);
            CountDownLatch messageLatch = new CountDownLatch(1);
            AtomicReference<FIXMessage> receivedMessage = new AtomicReference<>();
            
            client.setConnectionHandler(new SimpleConnectionHandler(connectLatch));
            client.setMessageHandler((c, msg) -> {
                if ("8".equals(msg.getMessageType())) { // Execution Report
                    receivedMessage.set(msg);
                    messageLatch.countDown();
                }
            });
            
            client.connect();
            connectLatch.await(30, TimeUnit.SECONDS);
            
            // Send an order to trigger a response
            FIXMessage order = OrderMessage.marketOrder("RECEIVE_TEST", "AAPL", 100, OrderSide.BUY)
                .toFIXMessage();
            client.sendMessage(order);
            
            // Wait for response
            boolean messageReceived = messageLatch.await(10, TimeUnit.SECONDS);
            
            client.disconnect().get(5, TimeUnit.SECONDS);
            client.shutdown();
            
            if (!messageReceived) {
                System.out.println("❌ No message received within timeout");
                return false;
            }
            
            if (receivedMessage.get() == null) {
                System.out.println("❌ Received message is null");
                return false;
            }
            
            System.out.println("✅ Message receiving test passed");
            return true;
            
        } catch (Exception e) {
            System.out.println("❌ Message receiving test failed: " + e.getMessage());
            return false;
        }
    }
    
    private boolean testReconnection() {
        System.out.println("\n🔄 Testing Reconnection...");
        
        try {
            FIXClient client = createTestClient("RECONNECT_TEST_CLIENT");
            
            CountDownLatch connectLatch = new CountDownLatch(1);
            CountDownLatch disconnectLatch = new CountDownLatch(1);
            CountDownLatch reconnectLatch = new CountDownLatch(1);
            
            AtomicInteger connectCount = new AtomicInteger(0);
            
            client.setConnectionHandler(new FIXClientConnectionHandler() {
                @Override
                public void onConnect(FIXClient client) {
                    int count = connectCount.incrementAndGet();
                    if (count == 1) {
                        connectLatch.countDown();
                    } else if (count == 2) {
                        reconnectLatch.countDown();
                    }
                }
                
                @Override
                public void onDisconnect(FIXClient client) {
                    disconnectLatch.countDown();
                }
                
                @Override
                public void onError(FIXClient client, Throwable error) {}
            });
            
            // Initial connection
            client.connect();
            connectLatch.await(30, TimeUnit.SECONDS);
            
            // Disconnect
            client.disconnect();
            disconnectLatch.await(10, TimeUnit.SECONDS);
            
            // Reconnect
            client.connect();
            reconnectLatch.await(30, TimeUnit.SECONDS);
            
            client.disconnect().get(5, TimeUnit.SECONDS);
            client.shutdown();
            
            if (connectCount.get() < 2) {
                System.out.println("❌ Reconnection failed, connect count: " + connectCount.get());
                return false;
            }
            
            System.out.println("✅ Reconnection test passed");
            return true;
            
        } catch (Exception e) {
            System.out.println("❌ Reconnection test failed: " + e.getMessage());
            return false;
        }
    }
    
    private boolean testConcurrentClients() {
        System.out.println("\n👥 Testing Concurrent Clients...");
        
        try {
            int clientCount = 5;
            List<FIXClient> clients = new ArrayList<>();
            CountDownLatch allConnectedLatch = new CountDownLatch(clientCount);
            
            // Create multiple clients
            for (int i = 0; i < clientCount; i++) {
                FIXClient client = createTestClient("CONCURRENT_CLIENT_" + i);
                client.setConnectionHandler(new SimpleConnectionHandler(allConnectedLatch));
                clients.add(client);
            }
            
            // Connect all clients concurrently
            for (FIXClient client : clients) {
                client.connect();
            }
            
            // Wait for all to connect
            boolean allConnected = allConnectedLatch.await(60, TimeUnit.SECONDS);
            
            if (!allConnected) {
                System.out.println("❌ Not all clients connected within timeout");
                return false;
            }
            
            // Send messages from all clients
            CountDownLatch allSentLatch = new CountDownLatch(clientCount);
            
            for (int i = 0; i < clientCount; i++) {
                final int clientIndex = i;
                CompletableFuture.runAsync(() -> {
                    try {
                        FIXMessage order = OrderMessage.marketOrder(
                            "CONCURRENT_" + clientIndex,
                            "AAPL",
                            100,
                            OrderSide.BUY
                        ).toFIXMessage();
                        
                        clients.get(clientIndex).sendMessage(order).get(5, TimeUnit.SECONDS);
                        allSentLatch.countDown();
                    } catch (Exception e) {
                        System.err.println("Error sending from client " + clientIndex + ": " + e.getMessage());
                    }
                });
            }
            
            boolean allSent = allSentLatch.await(30, TimeUnit.SECONDS);
            
            // Cleanup
            for (FIXClient client : clients) {
                try {
                    client.disconnect().get(5, TimeUnit.SECONDS);
                    client.shutdown();
                } catch (Exception e) {
                    System.err.println("Error disconnecting client: " + e.getMessage());
                }
            }
            
            if (!allSent) {
                System.out.println("❌ Not all messages sent within timeout");
                return false;
            }
            
            System.out.println("✅ Concurrent clients test passed");
            return true;
            
        } catch (Exception e) {
            System.out.println("❌ Concurrent clients test failed: " + e.getMessage());
            return false;
        }
    }
    
    private boolean testLatencyMeasurement() {
        System.out.println("\n⏱️ Testing Latency Measurement...");
        
        try {
            FIXClient client = createTestClient("LATENCY_TEST_CLIENT");
            
            CountDownLatch connectLatch = new CountDownLatch(1);
            CountDownLatch responseLatch = new CountDownLatch(10);
            
            List<Long> latencies = new ArrayList<>();
            
            client.setConnectionHandler(new SimpleConnectionHandler(connectLatch));
            client.setMessageHandler((c, msg) -> {
                if ("8".equals(msg.getMessageType())) {
                    String sendTimeStr = msg.getField(FIXTags.SENDING_TIME);
                    if (sendTimeStr != null) {
                        try {
                            long sendTime = Long.parseLong(sendTimeStr);
                            long latency = System.nanoTime() - sendTime;
                            latencies.add(latency);
                            responseLatch.countDown();
                        } catch (NumberFormatException e) {
                            // Ignore
                        }
                    }
                }
            });
            
            client.connect();
            connectLatch.await(30, TimeUnit.SECONDS);
            
            // Send 10 orders with timestamps
            for (int i = 0; i < 10; i++) {
                FIXMessage order = OrderMessage.marketOrder("LATENCY_" + i, "AAPL", 100, OrderSide.BUY)
                    .toFIXMessage();
                order.setField(FIXTags.SENDING_TIME, String.valueOf(System.nanoTime()));
                client.sendMessage(order);
                
                Thread.sleep(100); // Small delay between orders
            }
            
            boolean allResponses = responseLatch.await(30, TimeUnit.SECONDS);
            
            client.disconnect().get(5, TimeUnit.SECONDS);
            client.shutdown();
            
            if (!allResponses) {
                System.out.println("❌ Not all responses received for latency test");
                return false;
            }
            
            // Calculate statistics
            double avgLatencyMicros = latencies.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0) / 1000.0;
            
            System.out.printf("📊 Average latency: %.2f microseconds%n", avgLatencyMicros);
            
            if (avgLatencyMicros > 10000) { // 10ms threshold
                System.out.println("❌ Latency too high: " + avgLatencyMicros + " microseconds");
                return false;
            }
            
            System.out.println("✅ Latency measurement test passed");
            return true;
            
        } catch (Exception e) {
            System.out.println("❌ Latency measurement test failed: " + e.getMessage());
            return false;
        }
    }
    
    private FIXClient createTestClient(String senderCompId) {
        FIXClientConfiguration config = FIXClientConfiguration.builder()
            .host("localhost")
            .port(9878)
            .senderCompId(senderCompId)
            .targetCompId("SERVER1")
            .heartbeatInterval(30)
            .connectionTimeout(10000)
            .build();
        
        return FIXClientFactory.createClient(config);
    }
    
    private static class SimpleConnectionHandler implements FIXClientConnectionHandler {
        private final CountDownLatch latch;
        
        SimpleConnectionHandler(CountDownLatch latch) {
            this.latch = latch;
        }
        
        @Override
        public void onConnect(FIXClient client) {
            latch.countDown();
        }
        
        @Override
        public void onDisconnect(FIXClient client) {}
        
        @Override
        public void onError(FIXClient client, Throwable error) {}
    }
}
```

## 📚 Best Practices Summary

### 1. Connection Management
- Always use connection handlers to monitor state
- Implement proper reconnection logic for production
- Set appropriate timeouts for all operations

### 2. Message Processing
- Use asynchronous processing for high throughput
- Implement comprehensive error handling
- Cache frequently accessed fields for performance

### 3. Resource Management
- Always call `shutdown()` when done
- Use proper exception handling and cleanup
- Monitor memory usage in high-frequency scenarios

### 4. Performance Optimization
- Use Netty server (port 9879) for high performance
- Minimize object allocation in hot paths
- Consider object pooling for ultra-high frequency

### 5. Testing
- Test all connection scenarios (connect, disconnect, reconnect)
- Verify message sending and receiving
- Test concurrent client scenarios
- Measure and validate latency requirements

## 📚 Additional Resources

- **[Client Guide](CLIENT_GUIDE.md)** - Comprehensive client documentation
- **[API Reference](../development/API_REFERENCE.md)** - Complete API documentation
- **[Performance Guide](../performance/PERFORMANCE_GUIDE.md)** - Performance optimization
- **[Testing Guide](../development/TESTING.md)** - Testing strategies