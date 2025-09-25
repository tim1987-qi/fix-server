import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Simple test client to send a FIX message to the Netty server
 */
public class SendTestMessage {
    
    private static final String SOH = "\001"; // FIX field separator
    private static final DateTimeFormatter FIX_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss");
    
    public static void main(String[] args) {
        String host = "localhost";
        int port = 9879;
        
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            System.out.println("Connected to Netty FIX Server at " + host + ":" + port);
            
            // Send Logon message first
            String logonMessage = createLogonMessage();
            System.out.println("Sending Logon: " + logonMessage.replace(SOH, "|"));
            out.println(logonMessage);
            
            // Read logon response
            String response = in.readLine();
            if (response != null) {
                System.out.println("Received Logon Response: " + response.replace(SOH, "|"));
            }
            
            // Send a New Order Single message
            String orderMessage = createNewOrderMessage();
            System.out.println("Sending Order: " + orderMessage.replace(SOH, "|"));
            out.println(orderMessage);
            
            // Read order response
            response = in.readLine();
            if (response != null) {
                System.out.println("Received Order Response: " + response.replace(SOH, "|"));
            }
            
            // Send heartbeat
            String heartbeatMessage = createHeartbeatMessage();
            System.out.println("Sending Heartbeat: " + heartbeatMessage.replace(SOH, "|"));
            out.println(heartbeatMessage);
            
            // Read heartbeat response
            response = in.readLine();
            if (response != null) {
                System.out.println("Received Heartbeat Response: " + response.replace(SOH, "|"));
            }
            
            Thread.sleep(2000); // Wait a bit to see server logs
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String createLogonMessage() {
        String sendingTime = LocalDateTime.now().format(FIX_TIME_FORMAT);
        
        StringBuilder msg = new StringBuilder();
        msg.append("8=FIX.4.4").append(SOH);        // BeginString
        msg.append("35=A").append(SOH);             // MsgType = Logon
        msg.append("49=TESTCLIENT").append(SOH);    // SenderCompID
        msg.append("56=SERVER1").append(SOH);       // TargetCompID
        msg.append("34=1").append(SOH);             // MsgSeqNum
        msg.append("52=").append(sendingTime).append(SOH); // SendingTime
        msg.append("98=0").append(SOH);             // EncryptMethod = None
        msg.append("108=30").append(SOH);           // HeartBtInt = 30 seconds
        
        // Calculate body length (everything after the length field)
        String body = msg.substring(msg.indexOf("35="));
        int bodyLength = body.length();
        
        // Insert body length after BeginString
        String finalMsg = "8=FIX.4.4" + SOH + "9=" + bodyLength + SOH + body;
        
        // Calculate and append checksum
        int checksum = calculateChecksum(finalMsg);
        finalMsg += "10=" + String.format("%03d", checksum) + SOH;
        
        return finalMsg;
    }
    
    private static String createNewOrderMessage() {
        String sendingTime = LocalDateTime.now().format(FIX_TIME_FORMAT);
        
        StringBuilder msg = new StringBuilder();
        msg.append("8=FIX.4.4").append(SOH);        // BeginString
        msg.append("35=D").append(SOH);             // MsgType = NewOrderSingle
        msg.append("49=TESTCLIENT").append(SOH);    // SenderCompID
        msg.append("56=SERVER1").append(SOH);       // TargetCompID
        msg.append("34=2").append(SOH);             // MsgSeqNum
        msg.append("52=").append(sendingTime).append(SOH); // SendingTime
        msg.append("11=ORDER123").append(SOH);      // ClOrdID
        msg.append("55=AAPL").append(SOH);          // Symbol
        msg.append("54=1").append(SOH);             // Side = Buy
        msg.append("38=100").append(SOH);           // OrderQty
        msg.append("40=1").append(SOH);             // OrdType = Market
        msg.append("60=").append(sendingTime).append(SOH); // TransactTime
        
        // Calculate body length
        String body = msg.substring(msg.indexOf("35="));
        int bodyLength = body.length();
        
        // Insert body length
        String finalMsg = "8=FIX.4.4" + SOH + "9=" + bodyLength + SOH + body;
        
        // Calculate and append checksum
        int checksum = calculateChecksum(finalMsg);
        finalMsg += "10=" + String.format("%03d", checksum) + SOH;
        
        return finalMsg;
    }
    
    private static String createHeartbeatMessage() {
        String sendingTime = LocalDateTime.now().format(FIX_TIME_FORMAT);
        
        StringBuilder msg = new StringBuilder();
        msg.append("8=FIX.4.4").append(SOH);        // BeginString
        msg.append("35=0").append(SOH);             // MsgType = Heartbeat
        msg.append("49=TESTCLIENT").append(SOH);    // SenderCompID
        msg.append("56=SERVER1").append(SOH);       // TargetCompID
        msg.append("34=3").append(SOH);             // MsgSeqNum
        msg.append("52=").append(sendingTime).append(SOH); // SendingTime
        
        // Calculate body length
        String body = msg.substring(msg.indexOf("35="));
        int bodyLength = body.length();
        
        // Insert body length
        String finalMsg = "8=FIX.4.4" + SOH + "9=" + bodyLength + SOH + body;
        
        // Calculate and append checksum
        int checksum = calculateChecksum(finalMsg);
        finalMsg += "10=" + String.format("%03d", checksum) + SOH;
        
        return finalMsg;
    }
    
    private static int calculateChecksum(String message) {
        int sum = 0;
        for (char c : message.toCharArray()) {
            sum += (int) c;
        }
        return sum % 256;
    }
}