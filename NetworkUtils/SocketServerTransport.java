package NetworkUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentMap;

import NetworkCommunication.ClientHandler;
import NetworkCommunication.PacketProcessor;
import NetworkDataUnits.Packet;
import SimUtils.SimConfig;

public class SocketServerTransport implements NetworkTransport{
    SimConfig config = new SimConfig();
    private ServerSocket serverSocket;
    private PacketListener listener;
    private PacketProcessor processor;
    private volatile Boolean running = false;
    
    private ConcurrentMap<String, ClientHandler> clients;

    private int port;

    public SocketServerTransport(int port, PacketProcessor processor, ConcurrentMap<String, ClientHandler> clients){
        this.port = port;
        this.processor = processor;
        this.clients = clients;
    }

    @Override
    public void start() throws IOException{
        serverSocket = new ServerSocket(port);
        running = true;
        new Thread (() -> {
            try {
                while (running){
                    Socket socket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(socket, processor);

                    handler.start();
                    String clientIP = "TEMP";
                    clients.put(clientIP, handler);
                }
            } catch (IOException e){
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void sendPacket(Packet packet) throws IOException {
        String destIP = packet.destIP;
        ClientHandler handler = clients.get(destIP);
        if (handler != null) {
            handler.sendPacket(packet);
        } else {
            config.printSeparator();
            System.out.println("No client handler for " + destIP);
        }
    }

    @Override
    public void stop() throws IOException {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
    }

    @Override
    public void setPacketListener(PacketListener listener) {
        this.listener = listener;
    }

    public void registerClient(String ip, ClientHandler handler) {
        clients.put(ip, handler);
    }

    public void unregisterClient(String ip) {
        clients.remove(ip);
    }

    // Called by ClientHandler when a packet arrives
    public void onPacketReceived(Packet packet) {
        if (listener != null) {
            listener.onPacketReceived(packet);
        }
    }
}