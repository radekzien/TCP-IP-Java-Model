import NetworkCommunication.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import javax.swing.SwingUtilities;

import NetworkDataUnits.*;
import SimUtils.*;
import NetworkUtils.*;

public class Client  implements ClientCallback{
//-----SIMULATION VARIABLES-----
    static SimConfig config = new SimConfig();
    ErrorSim errorSim = new ErrorSim();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(config.getThreadAmount());

//-----VARIABLES-----
//Identifiers
    String hostName;
    String ip = "0.0.0.0"; //Initial IP pre-DHCP
    String mac;

//Router Information
    String routerHost;
    int routerPort;
    String routerIP = "";

//Assisitng variables
    private Socket socket;
    private ObjectOutputStream out;
    private ResponseListener listener;
    private ConcurrentMap<String, String> connectionList = new ConcurrentHashMap<>();

//GUI
    ClientGUI clientGUI;

//-----TCP VARIABLES-----
    ConcurrentHashMap<String, Integer> sendSeqs = new ConcurrentHashMap<>(); //Tracks the seqNums that are going to be sent
    ConcurrentHashMap<String, Integer> expSeqs = new ConcurrentHashMap<>();  //Tracks expected seqNums
    ConcurrentHashMap<String, Set<Integer>> expectedACKs = new ConcurrentHashMap<>(); //Tracks expected ackNums
    ConcurrentMap<Packet, ScheduledFuture<?>> inTransit = new ConcurrentHashMap<>(); //Tracks which packets are in transit (for retransmission)
    final int maxRetries = 3;
    final int retryIntervalSeconds = 2;

//ACK BOOLEAN VARIABLES
    private volatile boolean disconnectAckReceived = false;

//----- MAIN -----
    public static void main(String[] args) {

        if(args.length != 1){
            System.out.println("Usage: java Client <hostName>");
            return;
        }

        MACAssigner assigner = new MACAssigner();
        String hostName = args[0];

        String mac = assigner.assignMAC();
        String routerHost = config.getHost();
        int routerPort = config.getPort();

        Client client = new Client(hostName, mac, "0.0.0.0", routerHost, routerPort);
    }

//----- CONSTRUCTOR -----
    public Client (String hostName, String mac, String destIP, String routerHost, int routerPort){
        this.hostName = hostName;
        this.mac = mac;
        this.routerHost = routerHost;
        this.routerPort = routerPort;
        
        try {//Register with router upon instantiation
            socket = new Socket(routerHost, routerPort);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();

            listener = new ResponseListener(socket, this);
            listener.start();

            Segment seg = new Segment(ip, "255.255.255.255");
            seg.addPayload(hostName);
            Packet pac = new Packet(ip, "255.255.255.255", Protocols.DHCP, -1, -1, seg);
            pac.assignChecksum();

            sendToRouter(pac);

        } catch (IOException e){
            config.printSeparator();
            System.out.println("Failed to connect to router: ");
            System.out.println("    " + e.getMessage());
        }
    }

// ----- SENDING OUTGOING PACKETS -----
    public void sendTCP(String msg, String destIP){
        //Create TCP Segment
        Segment seg = new Segment(ip, destIP);
        seg.addPayload(msg);
        sendSeqs.putIfAbsent(destIP, 0);
        int sendSeq = sendSeqs.get(destIP);

        //Create TCP Packet
        Packet pac = new Packet(ip, destIP, Protocols.TCP, sendSeq, -1, seg);
        pac.assignChecksum();

        //Expect the TCP_ACK for the packet being sent
        expectedACKs.putIfAbsent(destIP, ConcurrentHashMap.newKeySet());
        expectedACKs.get(destIP).add(sendSeq);
        
        //Create retransmit task
        RetransmitTask task = new RetransmitTask(pac, destIP);

        //Update seqNum for next message to this IP address
        updateSendSeq(destIP);

        //Schedule retransmit task in case failure
        ScheduledFuture<?> future = scheduler.schedule(task, retryIntervalSeconds, TimeUnit.SECONDS);
        inTransit.put(pac, future); //Tracks packet - at this point the client believes the packet has been sent

        Packet finalPac = errorSim.addError(pac); //Adds chance of a corrupted packet

        //Simulate packet loss or send packet
        if(!errorSim.isDropped()){
            sendToRouter(finalPac);
        } else {
            config.printSeparator();
            System.out.println(":::Simulation message: Packet dropped:::");
        }
    }

    public void sendToRouter(Packet pac){
        if (socket == null || socket.isClosed()) {
            config.printSeparator();
            System.out.println("Cannot send: socket is closed");
            return;
        }
        try{
            config.printSeparator();
            System.out.println("Sending " + pac.protocol + " packet...");
            out.writeObject(pac);
            if(config.printPackets){
                System.out.println(pac.toString());
            }

            out.flush();
           
            
        } catch (IOException e){
            e.printStackTrace();
            config.printSeparator();
            System.out.println("Failed to communicate with router");
        }
    }


//----- HANDLING INCOMING PACKETS -----
    @Override
    public void processTCP(Packet packet){
        sendToApp(packet.srcIP, packet.getPayload().getPayload());
        updateExpSeq(packet.srcIP);
        Segment ackSeg = new Segment(getIP(), packet.srcIP);
        Packet ackPac = new Packet(getIP(), packet.srcIP, Protocols.TCP_ACK, -1, packet.seqNum, ackSeg);
        ackPac.assignChecksum();
        sendToRouter(ackPac);
    }

    @Override
    public void processTCPACK(Packet packet){
        Set<Integer> acks = expectedACKs.get(packet.srcIP);
        if (acks != null) {
            expectedACKs.get(packet.srcIP).remove(packet.ackNum);

            inTransit.entrySet().removeIf(entry -> {
                boolean match = entry.getKey().seqNum == packet.ackNum && entry.getKey().srcIP.equals(packet.destIP);
                if (match) {
                    entry.getValue().cancel(false);
                }
                return match;
            });
        }
    }

        @Override
    public void processDHCP(Packet packet){
        if(packet.protocol == Protocols.DHCP_ACK){
            Segment seg = packet.getPayload();
            Object payload = seg.getPayload();
            routerIP = packet.srcIP;
            if(payload instanceof String){
                ip = (String) payload;
                SwingUtilities.invokeLater(() -> clientGUI = new ClientGUI(this));

                config.printSeparator();
                System.out.println("Starting Client " + hostName + "\nIP: " + ip + "\nMAC: " + mac + "\nrouterHost: " + routerHost + "\nrouterPort: " + Integer.toString(routerPort));
            }
        } else {
            config.printSeparator();
            System.out.println("NO IPs AVAILABLE. PLEASE TRY AGAIN.");
            System.out.println("Exiting...");
                        if(scheduler != null){
                scheduler.shutdown();
            }
            if(listener != null){
                listener.shutdown();
            }
            try {   
                socket.close();
            } catch (IOException e){
                e.printStackTrace();
            }
        }


    }

    @Override
    public void sendToApp(String ip, Object message){
        if(message instanceof String){
            clientGUI.receiveMessage(ip, (String) message);
        } else {
            clientGUI.sendingError("Couldn't handle message");
        }
    }

//----- CONNECTIONS LIST -----
    @Override
    public void onClientListUpdated(ConcurrentMap<String, String> newList) {
        handleDisconnectedClients(newList);
        connectionList.clear();
        connectionList.putAll(newList);
        config.printSeparator();
        System.out.println(hostName + " updated connection list:");
        connectionList.forEach((ip, name) ->
            System.out.println(" - " + ip + " (" + name + ")")
            
        );
        if(clientGUI != null){
            clientGUI.updateClientList(newList);
        }
    }

    public ConcurrentMap<String, String> getConnectionList(){
        return(connectionList);
    }

    public void handleDisconnectedClients(ConcurrentMap<String, String> newList) {
        Set<String> oldIPs = new HashSet<>(connectionList.keySet());
        Set<String> newIPs = new HashSet<>(newList.keySet());

        Set<String> removedIPs = new HashSet<>(oldIPs);
        removedIPs.removeAll(newIPs);

        if(!removedIPs.isEmpty()){
            for(String ip : removedIPs){
                clientGUI.clearHistory(ip);
                resetTCP(ip);
            }
        }
    }

// ----- TCP UTILS -----
    public void updateSendSeq(String destIP){
        int sendSeq = sendSeqs.get(destIP);
        sendSeq = (sendSeq + 1) % 5;
        sendSeqs.put(destIP, sendSeq);
    }

    public void updateExpSeq(String srcIP){
        expSeqs.putIfAbsent(srcIP, 0);
        int expSeq = expSeqs.get(srcIP);
        expSeq = (expSeq + 1) % 5;
        expSeqs.put(srcIP, expSeq);
    }

    public int getExpSeqNum(String srcIP){
        expSeqs.putIfAbsent(srcIP, 0);
        return expSeqs.get(srcIP);
    }

    public String getIP(){
        return(ip);
    }

    public boolean isAck(String ip, int ackNum){
        return (expectedACKs.containsKey(ip) && expectedACKs.get(ip).contains(ackNum));
    }

    private class RetransmitTask implements Runnable {
         private final Packet packet;
        private final String destIP;
        private int retries = 0;

        RetransmitTask(Packet packet, String destIP) {
            this.packet = packet;
            this.destIP = destIP;
        }

        @Override
        public void run() {
            Set<Integer> expected = expectedACKs.get(destIP);
            if (expected != null && expected.contains(packet.seqNum)) {
                if (retries < maxRetries) {
                    config.printSeparator();
                    System.out.println("Retransmitting packet with seq " + packet.seqNum + " to " + destIP + " (retry #" + (retries+1) + ")");
                    sendToRouter(packet);
                    retries++;
                    ScheduledFuture<?> future = scheduler.schedule(this, retryIntervalSeconds, TimeUnit.SECONDS);
                    inTransit.put(packet, future);
                } else {
                    config.printSeparator();
                    System.out.println("Max retries reached for packet seq " + packet.seqNum + ". Giving up.");
                    expected.remove(packet.seqNum);
                    inTransit.remove(packet);
                    clientGUI.sendingError("No ACK");
                }
            } else {
                System.out.println("ACK received for seq " + packet.seqNum + ", cancelling retransmission.");
                ScheduledFuture<?> future = inTransit.remove(packet);
                if (future != null) future.cancel(false);
            }
        }
    }

    public void resetTCP(String ip){
        expectedACKs.remove(ip);
        sendSeqs.remove(ip);
        expSeqs.remove(ip);
    }

//----- DISCONNECTION -----
    public void close() {
        try {
            if(out != null){
                Segment seg = new Segment(ip, routerIP);
                seg.addPayload("DISCONNECT");
                Packet pac = new Packet(ip, routerIP, Protocols.DISCONNECT, -1, -1, seg);
                pac.assignChecksum();
                if(config.printPackets){
                    config.printSeparator();
                    System.out.println("Sent packet:\n" + pac.toString());
                }
                out.writeObject(pac);
                out.flush();

                //For disconnect-ack timeout
                synchronized(this){
                    long waitUntil = System.currentTimeMillis() + 5000;
                    while(!disconnectAckReceived && System.currentTimeMillis() < waitUntil){
                        wait(waitUntil - System.currentTimeMillis());
                    }
                }
                if(disconnectAckReceived){
                    config.printSeparator();
                    System.out.println("DISCONNECT-ACK Recieved. Closing connection.");
                } else {
                    config.printSeparator();
                    System.out.println("DISCONNECT-ACK TIMEOUT. Closing connection anyway.");
                }

            }
            if(scheduler != null){
                scheduler.shutdown();
            }
            if(listener != null){
                listener.shutdown();
            }
            socket.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void handleRouterDisconnect(){
        config.printSeparator();
        System.out.println("Router Disconnected. Shutting down.");
        System.out.println(":::RESTART TO RECONNECT:::");
        if(scheduler != null){
                scheduler.shutdown();
            }
        if(listener != null){
            listener.shutdown();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }


    @Override
    public void onDisconnectACK() {
        disconnectAckReceived = true;
        synchronized(this) {
            notifyAll();  // wake up any waiting thread
        }
    }


}