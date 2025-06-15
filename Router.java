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
import NetworkUtils.SocketServerTransport;


public class Router implements Runnable, PacketProcessor, PacketListener {
//----- VARIABLES -----
    private SocketServerTransport transport;

    //----- ROUTER INFO -----
    String ip = "192.168.1.1";
    String mac;
    private boolean running = false;
    private int port = 12345;

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

//----- INTERFACES AND HANDLERS -----
    @Override
    public void onClientRegister(String ip, ClientHandler handler, String hostName) {
        connectedClients.put(ip, handler);
        clientList.put(ip, hostName);
        System.out.println("Registered client: " + ip);
        broadcastConnectionsList();
    }

    @Override
    public void onPacketReceived(Packet packet) {
        System.out.println("Packet received from: " + packet.srcIP);
        inBuffer.offer(packet);
    }

    @Override
    public void onClientDisconnect(String ip) {
        if (!connectedClients.containsKey(ip)) return;

        ClientHandler handler = connectedClients.remove(ip);
        clientList.remove(ip);
        if (addressSpace.containsKey(ip)) {
            addressSpace.put(ip, "");
            System.out.println("Unassigned IP: " + ip);
        }

        try {
                if (handler != null && !handler.isInterrupted()) {
                    handler.interrupt();
                }
            } catch (Exception e) {
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
        Packet packet = new Packet(ip, CLIENT_IP, "BCAST", listSeg);

        handler.sendPacket(packet);
    }
        
    }

//----- RUNNING METHODS -----
    public void start() throws IOException {
        transport = new SocketServerTransport(port, this, connectedClients);
        createAddresses();
        try{
            transport.start();
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

    public void switchPacket(){ //Checks will be added later
        Packet pac = inBuffer.poll();
        if(pac != null && "TCP".equals(pac.protocol)){
            outBuffer.add(pac);
            System.out.println("Router switched packet from " + pac.srcIP + " to " + pac.destIP);
        }
    }

    public void sendPacket() throws IOException{
        Packet pac = outBuffer.poll();
        transport.sendPacket(pac);
        System.out.println("Sent packet from " + pac.srcIP + " to " + pac.destIP);
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
    public void createAddresses(){
        addressSpace.put("192.168.1.2", "");
        addressSpace.put("192.168.1.3", "");
        addressSpace.put("192.168.1.4", "");
        addressSpace.put("192.168.1.5", "");
    }

    public String allocateAddress(){
        for (Map.Entry<String, String> entry : addressSpace.entrySet()) {
        if ("".equals(entry.getValue())) {
            addressSpace.put(entry.getKey(), "RESERVED");
            return entry.getKey();
        }
    }
        return null; // Or throw an exception, or return Optional<String>
    }   

    public String getRouterIP(){
        return ip;
    }

    public void handleDHCP(Packet packet, ClientHandler handler){
        Segment clientSegment = packet.getPayload();
        String clientOldIP = packet.srcIP;
        String clientNewIP = allocateAddress();
        System.out.println(clientNewIP);
          if (clientNewIP == null) {
            System.out.println("No IPs left in address space.");
            return;
        }
        
        Object segmentPayload = clientSegment.getPayload();
        if(segmentPayload instanceof String){
            String clientHostName = (String) segmentPayload;
            addressSpace.put(clientNewIP, clientHostName);

            if (handler != null) {
                connectedClients.remove(clientOldIP);
                handler.setClientIP(clientNewIP);
                onClientRegister(clientNewIP, handler, clientHostName);
            } else {
                System.out.println("Handler not found for DHCP request from " + clientOldIP);
            }


            Segment returnSeg = new Segment(ip, clientNewIP);
            returnSeg.addPayload(clientNewIP);
            Packet returnPac = new Packet(ip, clientOldIP, "DHCP-ACK", returnSeg);
            handler.sendPacket(returnPac);
        }
    }
}


