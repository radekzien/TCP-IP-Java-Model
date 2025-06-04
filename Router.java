import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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

public class Router implements Runnable, PacketProcessor {
    private ServerSocket serverSocket; //Server Socket
    
    //Router information
    String ip;
    String mac;
    private boolean running = false;

    //Constructor
    public Router(String ip, String mac) {
    this.ip = ip;
    this.mac = mac;
    createAddresses();
}

    //Buffers and tables
    private final Queue<Packet> inBuffer = new ConcurrentLinkedQueue<>();
    private final Queue<Packet> outBuffer = new ConcurrentLinkedQueue<>();
    private final ConcurrentMap<String, ClientHandler> connectedClients = new ConcurrentHashMap<>();
    public ConcurrentMap<String, String> clientList = new ConcurrentHashMap<>();
    private ConcurrentMap<String, String> addressSpace =  new ConcurrentHashMap<>();

    //Interface and Handler methods
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
        connectedClients.remove(ip);
        clientList.remove(ip);
        System.out.println("Client disconnected: " + ip);
        broadcastConnectionsList();
    }

    public void broadcastConnectionsList(){
    for (Map.Entry<String, ClientHandler> entry : connectedClients.entrySet()) {
        String CLIENT_IP = entry.getKey();
        ClientHandler handler = entry.getValue();
        
        Segment listSeg = new Segment(ip, CLIENT_IP);
        listSeg.addPayload(new ClientListPayload(new ConcurrentHashMap<>(clientList)));
        Packet packet = new Packet(ip, CLIENT_IP, "BCAST", listSeg);

        handler.sendPacket(packet);
    }
        
    }

    //Socket and Running methods
    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Router running on port: " + port);
    
        new Thread(() -> {
            try {
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(clientSocket, this);
                    handler.start(); //Starts new thread for each client
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    
        running = true;
        Thread thread = new Thread(this);
        thread.start();
    }

    public void stop() {
        running = false;
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
        if("TCP".equals(pac.protocol)){
            outBuffer.add(pac);
            System.out.println("Router switched packet");
        }
    }

    public void sendPacket(){
        Packet pac = outBuffer.poll();
        String destIP = pac.destIP;

        ClientHandler handler = connectedClients.get(destIP);
        if(handler != null){
            handler.sendPacket(pac);
            System.out.println("Router passed packet to " + destIP);
        } else {
            System.out.println("Destination with IP: " + destIP + " not connected");
        }
    }

    public void processBuffers(){
        if(!inBuffer.isEmpty())
            while(!inBuffer.isEmpty()){
                switchPacket();
            }
        while(!outBuffer.isEmpty()){
            sendPacket();
        }
    }

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
                onClientRegister(clientNewIP, handler, clientHostName);
            } else {
                System.out.println("Handler not found for DHCP request from " + clientOldIP);
            }


            Segment returnSeg = new Segment(clientNewIP, clientHostName);
            Object returnPayload = getRouterIP();
            returnSeg.addPayload(returnPayload);
            Packet returnPac = new Packet(clientHostName, clientOldIP, "DHCP-ACK", returnSeg);
            handler.sendPacket(returnPac);
        }
    }
}


