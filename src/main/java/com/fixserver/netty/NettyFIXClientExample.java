package com.fixserver.netty;

import com.fixserver.core.FIXMessage;
import com.fixserver.core.FIXMessageImpl;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

/**
 * Example Netty-based FIX client for testing the Netty FIX server.
 * 
 * This client demonstrates how to connect to the Netty FIX server
 * and exchange FIX messages using Netty's asynchronous I/O.
 * 
 * @author FIX Server Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
public class NettyFIXClientExample {
    
    private final String host;
    private final int port;
    private final String senderCompId;
    private final String targetCompId;
    
    private EventLoopGroup group;
    private Channel channel;
    private final CountDownLatch logonLatch = new CountDownLatch(1);
    
    public NettyFIXClientExample(String host, int port, String senderCompId, String targetCompId) {
        this.host = host;
        this.port = port;
        this.senderCompId = senderCompId;
        this.targetCompId = targetCompId;
    }
    
    public void connect() throws Exception {
        group = new NioEventLoopGroup();
        
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            
                            // Add FIX message decoder/encoder
                            pipeline.addLast("fixDecoder", new FIXMessageDecoder());
                            pipeline.addLast("fixEncoder", new FIXMessageEncoder());
                            
                            // Add client message handler
                            pipeline.addLast("clientHandler", new NettyFIXClientHandler(logonLatch));
                        }
                    });
            
            // Connect to server
            ChannelFuture future = bootstrap.connect(host, port).sync();
            channel = future.channel();
            
            log.info("Connected to Netty FIX server at {}:{}", host, port);
            
            // Send logon message
            sendLogon();
            
            // Wait for logon response
            logonLatch.await();
            log.info("Logged on successfully");
            
            // Start interactive session
            runInteractiveSession();
            
        } finally {
            disconnect();
        }
    }
    
    private void sendLogon() throws Exception {
        FIXMessage logonMessage = new FIXMessageImpl();
        logonMessage.setField(FIXMessage.BEGIN_STRING, "FIX.4.4");
        logonMessage.setField(FIXMessage.MESSAGE_TYPE, "A"); // Logon
        logonMessage.setField(FIXMessage.SENDER_COMP_ID, senderCompId);
        logonMessage.setField(FIXMessage.TARGET_COMP_ID, targetCompId);
        logonMessage.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, "1");
        logonMessage.setField(FIXMessage.SENDING_TIME, LocalDateTime.now().toString());
        logonMessage.setField(98, "0"); // EncryptMethod (None)
        logonMessage.setField(108, "30"); // HeartBtInt
        
        String fixString = logonMessage.toFixString();
        channel.writeAndFlush(fixString);
        
        log.info("Sent logon message: {}", fixString.replace('\u0001', '|'));
    }
    
    private void runInteractiveSession() {
        System.out.println("\n=== Netty FIX Client Interactive Session ===");
        System.out.println("Commands:");
        System.out.println("  market <symbol> <buy|sell> <quantity> - Send market order");
        System.out.println("  limit <symbol> <buy|sell> <quantity> <price> - Send limit order");
        System.out.println("  heartbeat - Send heartbeat");
        System.out.println("  quit - Exit");
        System.out.println();
        
        Scanner scanner = new Scanner(System.in);
        int seqNum = 2;
        
        while (true) {
            System.out.print("netty-fix> ");
            String input = scanner.nextLine().trim();
            
            if (input.equals("quit")) {
                break;
            }
            
            try {
                if (input.equals("heartbeat")) {
                    sendHeartbeat(seqNum++);
                } else if (input.startsWith("market ")) {
                    handleMarketOrder(input, seqNum++);
                } else if (input.startsWith("limit ")) {
                    handleLimitOrder(input, seqNum++);
                } else if (!input.isEmpty()) {
                    System.out.println("Unknown command: " + input);
                }
            } catch (Exception e) {
                log.error("Error processing command: " + input, e);
            }
        }
        
        // Send logout
        try {
            sendLogout(seqNum);
        } catch (Exception e) {
            log.error("Error sending logout", e);
        }
    }
    
    private void sendHeartbeat(int seqNum) throws Exception {
        FIXMessage heartbeat = new FIXMessageImpl();
        heartbeat.setField(FIXMessage.BEGIN_STRING, "FIX.4.4");
        heartbeat.setField(FIXMessage.MESSAGE_TYPE, "0"); // Heartbeat
        heartbeat.setField(FIXMessage.SENDER_COMP_ID, senderCompId);
        heartbeat.setField(FIXMessage.TARGET_COMP_ID, targetCompId);
        heartbeat.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, String.valueOf(seqNum));
        heartbeat.setField(FIXMessage.SENDING_TIME, LocalDateTime.now().toString());
        
        String fixString = heartbeat.toFixString();
        channel.writeAndFlush(fixString);
        
        System.out.println("Heartbeat sent");
        log.debug("Sent heartbeat: {}", fixString.replace('\u0001', '|'));
    }
    
    private void handleMarketOrder(String input, int seqNum) throws Exception {
        String[] parts = input.split(" ");
        if (parts.length != 4) {
            System.out.println("Usage: market <symbol> <buy|sell> <quantity>");
            return;
        }
        
        String symbol = parts[1];
        String side = parts[2].equalsIgnoreCase("buy") ? "1" : "2";
        String quantity = parts[3];
        
        sendNewOrder(seqNum, symbol, side, quantity, "1", null); // Market order
        System.out.println("Market order sent: " + (side.equals("1") ? "BUY" : "SELL") + " " + quantity + " " + symbol);
    }
    
    private void handleLimitOrder(String input, int seqNum) throws Exception {
        String[] parts = input.split(" ");
        if (parts.length != 5) {
            System.out.println("Usage: limit <symbol> <buy|sell> <quantity> <price>");
            return;
        }
        
        String symbol = parts[1];
        String side = parts[2].equalsIgnoreCase("buy") ? "1" : "2";
        String quantity = parts[3];
        String price = parts[4];
        
        sendNewOrder(seqNum, symbol, side, quantity, "2", price); // Limit order
        System.out.println("Limit order sent: " + (side.equals("1") ? "BUY" : "SELL") + " " + quantity + " " + symbol + " at " + price);
    }
    
    private void sendNewOrder(int seqNum, String symbol, String side, String quantity, String orderType, String price) throws Exception {
        FIXMessage order = new FIXMessageImpl();
        order.setField(FIXMessage.BEGIN_STRING, "FIX.4.4");
        order.setField(FIXMessage.MESSAGE_TYPE, "D"); // New Order Single
        order.setField(FIXMessage.SENDER_COMP_ID, senderCompId);
        order.setField(FIXMessage.TARGET_COMP_ID, targetCompId);
        order.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, String.valueOf(seqNum));
        order.setField(FIXMessage.SENDING_TIME, LocalDateTime.now().toString());
        
        // Order fields
        order.setField(11, "ORDER_" + seqNum); // ClOrdID
        order.setField(55, symbol); // Symbol
        order.setField(54, side); // Side
        order.setField(38, quantity); // OrderQty
        order.setField(40, orderType); // OrdType
        order.setField(59, "0"); // TimeInForce (Day)
        order.setField(60, LocalDateTime.now().toString()); // TransactTime
        
        if (price != null) {
            order.setField(44, price); // Price
        }
        
        String fixString = order.toFixString();
        channel.writeAndFlush(fixString);
        
        log.debug("Sent order: {}", fixString.replace('\u0001', '|'));
    }
    
    private void sendLogout(int seqNum) throws Exception {
        FIXMessage logout = new FIXMessageImpl();
        logout.setField(FIXMessage.BEGIN_STRING, "FIX.4.4");
        logout.setField(FIXMessage.MESSAGE_TYPE, "5"); // Logout
        logout.setField(FIXMessage.SENDER_COMP_ID, senderCompId);
        logout.setField(FIXMessage.TARGET_COMP_ID, targetCompId);
        logout.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, String.valueOf(seqNum));
        logout.setField(FIXMessage.SENDING_TIME, LocalDateTime.now().toString());
        logout.setField(58, "Client requested disconnect"); // Text
        
        String fixString = logout.toFixString();
        channel.writeAndFlush(fixString);
        
        log.info("Sent logout message");
    }
    
    public void disconnect() {
        if (channel != null) {
            channel.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
        log.info("Disconnected from Netty FIX server");
    }
    
    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println("Usage: NettyFIXClientExample <host> <port> <senderCompId> <targetCompId>");
            System.out.println("Example: NettyFIXClientExample localhost 9879 CLIENT1 SERVER1");
            return;
        }
        
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String senderCompId = args[2];
        String targetCompId = args[3];
        
        NettyFIXClientExample client = new NettyFIXClientExample(host, port, senderCompId, targetCompId);
        
        try {
            client.connect();
        } catch (Exception e) {
            log.error("Error running Netty FIX client", e);
        }
    }
}