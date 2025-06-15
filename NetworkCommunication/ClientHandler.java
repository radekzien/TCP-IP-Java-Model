package NetworkCommunication;
import java.io.*;
import java.net.Socket;

import NetworkDataUnits.Packet;
import NetworkDataUnits.Segment;

public class ClientHandler extends Thread {
    private Socket socket;
    private PacketProcessor processor;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String clientIP;
    private boolean connected;

    public ClientHandler(Socket socket, PacketProcessor processor) {
        this.socket = socket;
        this.processor = processor;
    }

    @Override
    public void run() {
        try {
            initializeConnection();
            handleIncomingPackets();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("ClientHandler error for " + clientIP + ": " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void initializeConnection() throws IOException {
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
        connected = true;
    }

    public void setClientIP(String ip) {
        this.clientIP = ip;
    }

    private void registerClient(String ip, String hostName){
            clientIP = ip;
            processor.onClientRegister(clientIP, this, hostName);
            connected = true;
    }

    private void handleIncomingPackets() throws IOException, ClassNotFoundException {
        while (connected && !socket.isClosed()) {
            Object obj = in.readObject();
            if (obj instanceof Packet packet) {
                if("DHCP".equals(packet.protocol)){
                    processor.handleDHCP(packet, this);
                } else if("DISCONNECT".equals(packet.protocol)){
                    System.out.println("Received DISCONNECT packet from " + clientIP);

                    Segment ackSeg = new Segment(processor.getRouterIP(), clientIP);
                    Packet ackPacket = new Packet(processor.getRouterIP(), clientIP, "DISCONNECT-ACK", ackSeg);
                    sendPacket(ackPacket);

                    break;
                } else{ 
                    processor.onPacketReceived(packet);
                }
            }
        }
    }

    public void sendPacket(Packet pac) {
        try {
            out.flush();
            out.writeObject(pac);
            out.flush();
        } catch (IOException e) {
            System.out.println("Failed to send packet to " + clientIP);
        }
    }

    public void cleanup() {
        try {
            if (clientIP != null) {
                processor.onClientDisconnect(clientIP);
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing client connection: " + clientIP);
        }
    }
}
