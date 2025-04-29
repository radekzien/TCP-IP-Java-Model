import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import NetworkCommunication.ClientHandler;
import NetworkCommunication.PacketProcessor;
import NetworkDataUnits.Packet;

public class Router implements Runnable, PacketProcessor {
    private ServerSocket serverSocket; //Server Socket
    
    //Router information
    String ip;
    String mac;
    private boolean running = false;

    //Buffers and tables
    private final Queue<Packet> inBuffer = new ConcurrentLinkedQueue<>();
    private final Queue<Packet> outBuffer = new ConcurrentLinkedQueue<>();
    private final ConcurrentMap<String, ClientHandler> connectedClients = new ConcurrentHashMap<>();
    //TODO: Add routing table when looking at introducing more complex multirouter networks

    //Interface and Handler methods
    @Override
    public void onClientRegister(String ip, ClientHandler handler) {
        connectedClients.put(ip, handler);
        System.out.println("Registered client: " + ip);
    }

    @Override
    public void onPacketReceived(Packet packet) {
        inBuffer.offer(packet);
        System.out.println("Packet received from: " + packet.srcIP);
    }

    @Override
    public void onClientDisconnect(String ip) {
        connectedClients.remove(ip);
        System.out.println("Client disconnected: " + ip);
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
        outBuffer.add(pac);
        System.out.println("Router switched packet");
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
}
