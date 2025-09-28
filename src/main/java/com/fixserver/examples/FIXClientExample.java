package com.fixserver.examples;

import com.fixserver.client.FIXClient;
import com.fixserver.client.FIXClientFactory;
import com.fixserver.client.FIXClientConfiguration;
import com.fixserver.client.FIXClientConnectionHandler;
import com.fixserver.client.FIXClientImpl;
import com.fixserver.client.messages.OrderMessage;
import com.fixserver.core.FIXMessage;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Example FIX client application demonstrating how to connect to a FIX server
 * and send/receive messages.
 * 
 * This example shows:
 * - Connecting to a FIX server
 * - Handling connection events
 * - Sending orders
 * - Receiving responses
 * - Interactive command-line interface
 * 
 * @author FIX Server Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
public class FIXClientExample {
    
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 9876;
    private static final String SENDER_COMP_ID = "CLIENT1";
    private static final String TARGET_COMP_ID = "SERVER1";
    
    public static void main(String[] args) {
        // Parse command line arguments
        String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;
        String senderCompId = args.length > 2 ? args[2] : SENDER_COMP_ID;
        String targetCompId = args.length > 3 ? args[3] : TARGET_COMP_ID;
        
        log.info("Starting FIX Client Example");
        log.info("Connecting to {}:{} as {} -> {}", host, port, senderCompId, targetCompId);
        
        // Create client configuration
        FIXClientConfiguration config = FIXClientConfiguration.builder()
                .host(host)
                .port(port)
                .senderCompId(senderCompId)
                .targetCompId(targetCompId)
                .heartbeatInterval(30)
                .autoHeartbeat(true)
                .autoResendRequest(true)
                .build();
        
        // Create client
        FIXClient client = FIXClientFactory.createClient(config);
        
        // Set up connection handler
        CountDownLatch connectedLatch = new CountDownLatch(1);
        client.setConnectionHandler(new FIXClientConnectionHandler() {
            @Override
            public void onConnected(FIXClient client) {
                log.info("Connected to FIX server");
            }
            
            @Override
            public void onDisconnected(FIXClient client, String reason) {
                log.info("Disconnected from FIX server: {}", reason);
            }
            
            @Override
            public void onError(FIXClient client, Throwable error) {
                log.error("Connection error", error);
            }
            
            @Override
            public void onLoggedOn(FIXClient client) {
                log.info("Logged on to FIX server - ready to send messages");
                connectedLatch.countDown();
            }
            
            @Override
            public void onLoggedOut(FIXClient client) {
                log.info("Logged out from FIX server");
            }
        });
        
        // Set up message handler
        client.setMessageHandler((message, clientRef) -> {
            log.info("Received message: Type={}, Sender={}, Target={}, SeqNum={}", 
                    message.getMessageType(), message.getSenderCompId(), message.getTargetCompId(), message.getMessageSequenceNumber());
            
            // Print some key fields
            String symbol = message.getField(55);
            String side = message.getField(54);
            String quantity = message.getField(38);
            String price = message.getField(44);
            
            if (symbol != null) {
                log.info("  Symbol: {}", symbol);
            }
            if (side != null) {
                log.info("  Side: {}", "1".equals(side) ? "BUY" : "SELL");
            }
            if (quantity != null) {
                log.info("  Quantity: {}", quantity);
            }
            if (price != null) {
                log.info("  Price: {}", price);
            }
        });
        
        try {
            // Connect to server
            client.connect().get(30, TimeUnit.SECONDS);
            
            // Wait for logon to complete
            if (!connectedLatch.await(30, TimeUnit.SECONDS)) {
                log.error("Failed to log on within timeout");
                return;
            }
            
            // Start interactive session
            runInteractiveSession(client);
            
        } catch (Exception e) {
            log.error("Error running client", e);
        } finally {
            // Disconnect
            try {
                client.disconnect().get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("Error disconnecting", e);
            }
            
            // Shutdown if it's our implementation
            if (client instanceof FIXClientImpl) {
                ((FIXClientImpl) client).shutdown();
            }
        }
    }
    
    private static void runInteractiveSession(FIXClient client) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("\n=== FIX Client Interactive Session ===");
        System.out.println("Commands:");
        System.out.println("  market <symbol> <buy|sell> <quantity> - Send market order");
        System.out.println("  limit <symbol> <buy|sell> <quantity> <price> - Send limit order");
        System.out.println("  cancel <clientOrderId> <symbol> <buy|sell> - Cancel order");
        System.out.println("  status <clientOrderId> <symbol> <buy|sell> - Get order status");
        System.out.println("  quit - Exit");
        System.out.println();
        
        int orderCounter = 1;
        
        while (true) {
            System.out.print("fix> ");
            String line = scanner.nextLine().trim();
            
            if (line.isEmpty()) {
                continue;
            }
            
            String[] parts = line.split("\\s+");
            String command = parts[0].toLowerCase();
            
            try {
                switch (command) {
                    case "quit":
                    case "exit":
                        return;
                        
                    case "market":
                        if (parts.length != 4) {
                            System.out.println("Usage: market <symbol> <buy|sell> <quantity>");
                            break;
                        }
                        sendMarketOrder(client, parts[1], parts[2], parts[3], orderCounter++);
                        break;
                        
                    case "limit":
                        if (parts.length != 5) {
                            System.out.println("Usage: limit <symbol> <buy|sell> <quantity> <price>");
                            break;
                        }
                        sendLimitOrder(client, parts[1], parts[2], parts[3], parts[4], orderCounter++);
                        break;
                        
                    case "cancel":
                        if (parts.length != 4) {
                            System.out.println("Usage: cancel <clientOrderId> <symbol> <buy|sell>");
                            break;
                        }
                        sendCancelOrder(client, parts[1], parts[2], parts[3], orderCounter++);
                        break;
                        
                    case "status":
                        if (parts.length != 4) {
                            System.out.println("Usage: status <clientOrderId> <symbol> <buy|sell>");
                            break;
                        }
                        sendStatusRequest(client, parts[1], parts[2], parts[3], orderCounter++);
                        break;
                        
                    default:
                        System.out.println("Unknown command: " + command);
                        break;
                }
            } catch (Exception e) {
                log.error("Error executing command: " + line, e);
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
    
    private static void sendMarketOrder(FIXClient client, String symbol, String sideStr, 
                                       String quantityStr, int orderCounter) throws Exception {
        OrderMessage.Side side = "buy".equalsIgnoreCase(sideStr) ? OrderMessage.Side.BUY : OrderMessage.Side.SELL;
        BigDecimal quantity = new BigDecimal(quantityStr);
        String clientOrderId = "ORDER_" + orderCounter;
        
        FIXMessage order = OrderMessage.marketOrder(clientOrderId, symbol, side, quantity);
        
        log.info("Sending market order: {} {} {} shares", side, quantity, symbol);
        client.sendMessage(order).get(5, TimeUnit.SECONDS);
        System.out.println("Market order sent: " + clientOrderId);
    }
    
    private static void sendLimitOrder(FIXClient client, String symbol, String sideStr, 
                                      String quantityStr, String priceStr, int orderCounter) throws Exception {
        OrderMessage.Side side = "buy".equalsIgnoreCase(sideStr) ? OrderMessage.Side.BUY : OrderMessage.Side.SELL;
        BigDecimal quantity = new BigDecimal(quantityStr);
        BigDecimal price = new BigDecimal(priceStr);
        String clientOrderId = "ORDER_" + orderCounter;
        
        FIXMessage order = OrderMessage.limitOrder(clientOrderId, symbol, side, quantity, price);
        
        log.info("Sending limit order: {} {} {} shares at {}", side, quantity, symbol, price);
        client.sendMessage(order).get(5, TimeUnit.SECONDS);
        System.out.println("Limit order sent: " + clientOrderId);
    }
    
    private static void sendCancelOrder(FIXClient client, String originalOrderId, String symbol, 
                                       String sideStr, int orderCounter) throws Exception {
        OrderMessage.Side side = "buy".equalsIgnoreCase(sideStr) ? OrderMessage.Side.BUY : OrderMessage.Side.SELL;
        String clientOrderId = "CANCEL_" + orderCounter;
        
        FIXMessage cancel = OrderMessage.orderCancelRequest(clientOrderId, originalOrderId, symbol, side);
        
        log.info("Sending cancel request for order: {}", originalOrderId);
        client.sendMessage(cancel).get(5, TimeUnit.SECONDS);
        System.out.println("Cancel request sent: " + clientOrderId);
    }
    
    private static void sendStatusRequest(FIXClient client, String originalOrderId, String symbol, 
                                         String sideStr, int orderCounter) throws Exception {
        OrderMessage.Side side = "buy".equalsIgnoreCase(sideStr) ? OrderMessage.Side.BUY : OrderMessage.Side.SELL;
        String clientOrderId = "STATUS_" + orderCounter;
        
        FIXMessage status = OrderMessage.orderStatusRequest(clientOrderId, originalOrderId, symbol, side);
        
        log.info("Sending status request for order: {}", originalOrderId);
        client.sendMessage(status).get(5, TimeUnit.SECONDS);
        System.out.println("Status request sent: " + clientOrderId);
    }
}