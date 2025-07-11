import java.io.IOException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import NetworkCommunication.ClientHandler;
import NetworkCommunication.PacketProcessor;
import NetworkDataUnits.ClientListPayload;
import NetworkDataUnits.Packet;
import NetworkDataUnits.Segment;
import NetworkUtils.PacketListener;
import NetworkUtils.Protocols;
import NetworkUtils.SocketServerTransport;
import SimUtils.SimConfig;


public class Router implements Runnable, PacketProcessor, PacketListener {
    SimConfig config = new SimConfig();
//----- VARIABLES -----
    private SocketServerTransport transport;

    //----- ROUTER INFO -----
    String ip = config.getNetworkIP() + "1";
    String mac;
    private boolean running = false;
    private int port = config.getPort();

    //----- BUFFERS AND TABLES -----
    private final Queue<Packet> inBuffer = new ConcurrentLinkedQueue<>();
    private final Queue<Packet> outBuffer = new ConcurrentLinkedQueue<>();
    private final ConcurrentMap<String, ClientHandler> connectedClients = new ConcurrentHashMap<>();
    public ConcurrentMap<String, String> clientList = new ConcurrentHashMap<>();
    private ConcurrentMap<String, String> addressSpace =  new ConcurrentHashMap<>();

//----- MAIN -----
    public static void main(String[] args) throws IOException {
        Router router = new Router();
        router.start();
    }

//----- CLIENT CONNECTION -----
    @Override
    public void onClientRegister(String ip, ClientHandler handler, String hostName) {
        connectedClients.put(ip, handler);
        clientList.put(ip, hostName);
        config.printSeparator();
        System.out.println("Registered client: " + ip);
        broadcastConnectionsList();
    }

    @Override
    public void onClientDisconnect(String ip) {
        if (!connectedClients.containsKey(ip)) return;

        ClientHandler handler = connectedClients.remove(ip);
        clientList.remove(ip);
        if (addressSpace.containsKey(ip)) {
            addressSpace.put(ip, "");
            config.printSeparator();
            System.out.println("Unassigned IP: " + ip);
        }

        try {
                if (handler != null && !handler.isInterrupted()) {
                    handler.interrupt();
                }
            } catch (Exception e) {               
                config.printSeparator();
                System.out.println("Error during handler interrupt: " + e.getMessage());
            }

            System.out.println("Client disconnected: " + ip);
            broadcastConnectionsList();
    }

    public void broadcastConnectionsList(){
        for (Map.Entry<String, ClientHandler> entry : connectedClients.entrySet()) {
            String CLIENT_IP = entry.getKey();
            ClientHandler handler = entry.getValue();

            if (handler == null) {
                continue;
            }
            
            Segment listSeg = new Segment(ip, CLIENT_IP);
            listSeg.addPayload(new ClientListPayload(new ConcurrentHashMap<>(clientList)));
            Packet packet = new Packet(ip, CLIENT_IP, Protocols.BCAST, -1, -1, listSeg);
            packet.assignChecksum();

            handler.sendPacket(packet);
        }       
    }

//-----PACKET HANDLING-----
    @Override
    public void onPacketReceived(Packet packet) {
        config.printSeparator();
        System.out.println("Packet received from: " + packet.srcIP);
        inBuffer.offer(packet);
    }

    public void switchPacket(){
        Packet pac = inBuffer.poll();
        if(pac != null && (pac.protocol == Protocols.TCP|| pac.protocol == Protocols.TCP_ACK)){
            outBuffer.add(pac);
            config.printSeparator();
            System.out.println("Router switched packet from " + pac.srcIP + " to " + pac.destIP);
        }
    }

    public void sendPacket() throws IOException{
        Packet pac = outBuffer.poll();
        if(pac != null){
            transport.sendPacket(pac);
            config.printSeparator();
            System.out.println("Sent packet from " + pac.srcIP + " to " + pac.destIP);
        }
    }

//----- RUNNING METHODS -----
    public void start() throws IOException {
        transport = new SocketServerTransport(port, this, connectedClients);
        createAddresses(config.getAmount());
        try{
            transport.start();
            config.printSeparator();
            System.out.println("Router running on port: " + port);  
            running = true;
            new Thread(this).start();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public void stop() throws IOException{
        running = false;
        transport.stop();
    }

    @Override
    public void run() {
        while (running) {
            processBuffers();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void processBuffers(){
        if(!inBuffer.isEmpty())
            while(!inBuffer.isEmpty()){
                switchPacket();
            }
        while(!outBuffer.isEmpty()){
            try {
                sendPacket();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

//----- ADDRESS ALLOCATION AND DHCP -----
    public void createAddresses(int amount){
        if(amount <= 254){
            int i = 2;
            while(i < amount + 2){
                String address = "192.168.1." + Integer.toString(i);
                addressSpace.put(address, "");
                i++;
            }
        } else {
            System.out.println("ERROR: addressAmount needs to be <= 255. Change config");
            System.exit(0);
        }

    }

    public String allocateAddress(){
        for (Map.Entry<String, String> entry : addressSpace.entrySet()) {
        if ("".equals(entry.getValue())) {
            addressSpace.put(entry.getKey(), "RESERVED");
            return entry.getKey();
        }
    }
        return null;
    }   

    public void handleDHCP(Packet packet, ClientHandler handler){
        Segment clientSegment = packet.getPayload();
        String clientOldIP = packet.srcIP;
        String clientNewIP = allocateAddress();
          if (clientNewIP == null) {
            config.printSeparator();
            System.out.println("No IPs left in address space.");

            Segment returnSeg = new Segment(ip, clientOldIP);
            returnSeg.addPayload("NO IP AVAILABLE");
            Packet returnPac = new Packet(ip, clientOldIP, Protocols.DHCP_NACK, -1, -1, returnSeg);
            returnPac.assignChecksum();
            handler.sendPacket(returnPac);
            handler.cleanup();
            return;
        }
        
        Object segmentPayload = clientSegment.getPayload();
        if(segmentPayload instanceof String && clientNewIP != null){
            String clientHostName = (String) segmentPayload;
            addressSpace.put(clientNewIP, clientHostName);

            if (handler != null) {
                connectedClients.remove(clientOldIP);
                handler.setClientIP(clientNewIP);
                onClientRegister(clientNewIP, handler, clientHostName);
            } else {
                config.printSeparator();
                System.out.println("Handler not found for DHCP request from " + clientOldIP);
            }


            Segment returnSeg = new Segment(ip, clientNewIP);
            returnSeg.addPayload(clientNewIP);
            Packet returnPac = new Packet(ip, clientOldIP, Protocols.DHCP_ACK, -1, -1, returnSeg);
            returnPac.assignChecksum();
            handler.sendPacket(returnPac);
        }
    }

//----- UTILITY METHODS -----
    public String getRouterIP(){
        return ip;
    }
}


