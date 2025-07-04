import NetworkCommunication.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import javax.swing.SwingUtilities;

import NetworkDataUnits.*;
import SimUtils.*;

public class Client  implements ClientCallback{
    static SimConfig config = new SimConfig();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4); //Look into configurable pool size
//----- VARIABLES -----
    String hostName;
    String ip = "0.0.0.0";
    String mac;

    String destIP; //Hardcoded for now
    String destMAC;

    String message;

    String routerHost;
    int routerPort;

    String routerIP = "";

    private Socket socket;
    private ObjectOutputStream out;
    private ResponseListener listener;
    private ConcurrentMap<String, String> connectionList = new ConcurrentHashMap<>();

    ClientGUI clientGUI;

//TCP VARIABLES
    ConcurrentHashMap<String, Integer> sendSeqs = new ConcurrentHashMap<>(); 
    ConcurrentHashMap<String, Integer> expSeqs = new ConcurrentHashMap<>(); 
    ConcurrentHashMap<String, Set<Integer>> expectedACKs = new ConcurrentHashMap<>();
    ConcurrentMap<Packet, ScheduledFuture<?>> inTransit = new ConcurrentHashMap<>();
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

        System.out.println("Starting Client " + hostName + "\nMAC: " + mac + "\nrouterHost: " + routerHost + "\nrouterPort: " + Integer.toString(routerPort));
        config.printSeparator();
    }

//----- CONSTRUCTOR -----
    public Client (String hostName, String mac, String destIP, String routerHost, int routerPort){
        this.hostName = hostName;
        this.mac = mac;
        this.destIP = destIP;
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
            Packet pac = new Packet(ip, "255.255.255.255", "DHCP", -1, -1, seg);

            out.writeObject(pac);
            out.flush();

        } catch (IOException e){
            System.out.println("Client " + ip + " failed to connect to router: " + e.getMessage());
        }
    }

// ----- MESSAGING AND MESSAGE HANDLING -----
    public void sendTCP(String msg, String destIP){
        Segment seg = new Segment(ip, destIP);
        seg.addPayload(msg);
        sendSeqs.putIfAbsent(destIP, 0);
        int sendSeq = sendSeqs.get(destIP);

        //Send TCP
        Packet pac = new Packet(ip, destIP, "TCP", sendSeq, -1, seg);
        expectedACKs.putIfAbsent(destIP, ConcurrentHashMap.newKeySet());
        expectedACKs.get(destIP).add(sendSeq);
        
        RetransmitTask task = new RetransmitTask(pac, destIP);

        updateSendSeq(destIP);
        ScheduledFuture<?> future = scheduler.schedule(task, retryIntervalSeconds, TimeUnit.SECONDS);
        inTransit.put(pac, future);

        sendToRouter(pac);
    }

    public void sendToRouter(Packet pac){
        if (socket == null || socket.isClosed()) {
            System.out.println("Cannot send: socket is closed");
            return;
        }
        try{
            out.writeObject(pac);
            if(pac.getPayload().getPayload() instanceof String){
                System.out.println("Sent Packet: \nSender IP: " + pac.srcIP + "\n" + "Destination IP: " + pac.destIP + "\n" +"Protocol: " + pac.protocol + "\n" + "Segment Payload: " + pac.getPayload().getPayload().toString());
            } else {
                System.out.println("Sent Packet: \nSender IP: " + pac.srcIP + "\n" + "Destination IP: " + pac.destIP + "\n" +"Protocol: " + pac.protocol);
            }
             config.printSeparator();
            out.flush();
           
            
        } catch (IOException e){
            e.printStackTrace();
            System.out.println("Failed to communicate with router");
        }
    }

    @Override
    public void processTCP(Packet packet){
        sendToApp(packet.srcIP, packet.getPayload().getPayload());
        updateExpSeq(packet.srcIP);
        Segment ackSeg = new Segment(getIP(), packet.srcIP);
        Packet ackPac = new Packet(getIP(), packet.srcIP, "TCP-ACK", -1, packet.seqNum, ackSeg);
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
        Segment seg = packet.getPayload();
        Object payload = seg.getPayload();
        routerIP = packet.srcIP;
        if(payload instanceof String){
            ip = (String) payload;
            SwingUtilities.invokeLater(() -> clientGUI = new ClientGUI(this));
        }

    }

    @Override
    public void sendToApp(String ip, Object message){
        if(message instanceof String){
            clientGUI.receiveMessage(ip, (String) message);
        } else {
            return; //Handle this somehow
        }
    }

//----- CLIENT SYSTEM -----
    @Override
    public void onClientListUpdated(ConcurrentMap<String, String> newList) {
        connectionList.clear();
        connectionList.putAll(newList);
        System.out.println(hostName + " updated connection list:");
        connectionList.forEach((ip, name) ->
            System.out.println(" - " + ip + " (" + name + ")")
            
        );
        config.printSeparator();
        if(clientGUI != null){
            clientGUI.updateClientList(newList);
        }
    }

    public ConcurrentMap<String, String> getConnectionList(){
        return(connectionList);
    }

//-----TCP UTILS-----
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
                    System.out.println("Retransmitting packet with seq " + packet.seqNum + " to " + destIP + " (retry #" + (retries+1) + ")");
                    sendToRouter(packet);
                    retries++;
                    ScheduledFuture<?> future = scheduler.schedule(this, retryIntervalSeconds, TimeUnit.SECONDS);
                    inTransit.put(packet, future);
                } else {
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

//----- DISCONNECTION -----
    public void close() {
        try {
            if(out != null){
                Segment seg = new Segment(ip, routerIP);
                seg.addPayload("DISCONNECT");
                Packet pac = new Packet(ip, routerIP, "DISCONNECT", -1, -1, seg);
                System.out.println("Sent Packet: \nSender IP: " + pac.srcIP + "\n" + "Destination IP: " + pac.destIP + "\n" +"Protocol: " + pac.protocol + "\n" + "Segment Payload: " + pac.getPayload().getPayload().toString());
                config.printSeparator();
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
                    System.out.println("DISCONNECT-ACK Recieved. Closing connection.");
                } else {
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

    @Override
    public void onDisconnectACK() {
        disconnectAckReceived = true;
        synchronized(this) {
            notifyAll();  // wake up any waiting thread
        }
    }


}