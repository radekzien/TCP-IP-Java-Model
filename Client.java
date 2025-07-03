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
//----- VARIABLES -----
    String hostName;
    String ip = "0.0.0.0";
    String mac;

    String destIP; //Hardcoded for now
    String destMAC;

    String message;

    String routerHost;
    int routerPort;

    Segment seg;
    Packet pac;

    String routerIP = "";

    private Socket socket;
    private ObjectOutputStream out;
    private ResponseListener listener;
    private DataUnitHandler duh = new DataUnitHandler();
    private ConcurrentMap<String, String> connectionList = new ConcurrentHashMap<>();

    ClientGUI clientGUI;

//TCP VARIABLES
    int sendSeq = 0;
    int expSeq = 0;
    ArrayList<Integer> expectedACKs = new ArrayList<>();
    ConcurrentMap<Packet, String> inTransit = new ConcurrentHashMap<>(); //REPLACE STRING WITH TIMER OBJECT

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

            seg = duh.createSegment(ip, "255.255.255.255", hostName);
            pac = duh.createPacket(ip, "255.255.255.255", "DHCP", -1, -1, seg);

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
        Packet pac = new Packet(ip, destIP, "TCP", sendSeq, -1, seg);
        expectedACKs.add(sendSeq);
        inTransit.put(pac, "Retransmission timer"); //ADD RETRANSMISSION TIMER + LOGIC
        sendToRouter(pac);
        updateSendSeq();
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
        updateExpSeq();
        Segment ackSeg = new Segment(getIP(), packet.srcIP);
        Packet ackPac = new Packet(getIP(), packet.srcIP, "TCP-ACK", -1, packet.seqNum, ackSeg);
        sendToRouter(ackPac);
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
    public void updateSendSeq(){
        sendSeq = (sendSeq + 1) % 5;
    }

    public void updateExpSeq(){
        expSeq = (expSeq + 1) % 5;
    }

    public int getExpSeqNum(){
        return expSeq;
    }

    public String getIP(){
        return(ip);
    }

    public boolean isAck(int ackNum){
        return(expectedACKs.contains(ackNum));
    }

//----- DISCONNECTION -----
    public void close() {
        try {
            if(out != null){
                seg = duh.createSegment(ip, routerIP, "DISCONNECT");
                pac = duh.createPacket(ip, routerIP, "DISCONNECT", -1, -1, seg);
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