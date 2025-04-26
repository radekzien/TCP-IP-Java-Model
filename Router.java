import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Router implements Runnable {
    private ServerSocket serverSocket;
    private Map<String, ClientHandler> connectedClients = new HashMap<>();
    
    String ip;
    String mac;

    ArrayList<Packet>inBuffer = new ArrayList<>();
    ArrayList<Packet>outBuffer = new ArrayList<>();//Simplified for now

    private boolean running = false;


    //Socket and Running methods
    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Router running on port: " + port);
    
        new Thread(() -> {
            try {
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(clientSocket, this);
                    handler.start(); // Starts new thread for each client
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

    //Client methods 
    public void registerClient(String ip, ClientHandler handler) {
        connectedClients.put(ip, handler);
        System.out.println("Registered client with IP: " + ip);
    }
    
    //Packet Methods
    public void receivePacket(Packet pac){
        inBuffer.add(pac);
        System.out.println("Router received packet");
    }

    public void switchPacket(){ //Checks will be added later
        Packet pac = inBuffer.getFirst();
        inBuffer.remove(0);
        outBuffer.add(pac);
        System.out.println("Router switched packet");
    }

    public void sendPacket(){
        Packet pac = outBuffer.get(0);
        String destIP = pac.destIP;
        outBuffer.remove(0);

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
