package NetworkCommunication;
import java.io.*;
import java.net.Socket;

import NetworkDataUnits.*;
import NetworkUtils.*;
import SimUtils.SimConfig;

public class ClientHandler extends Thread {
//-----VARIABLES-----
    SimConfig config = new SimConfig(); //Import config

    private Socket socket;
    private PacketProcessor processor;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String clientIP;
    private boolean connected;

//-----CONSTRUCTOR-----
    public ClientHandler(Socket socket, PacketProcessor processor) {
        this.socket = socket;
        this.processor = processor;
    }

//-----RUNNING METHOD-----
    @Override
    public void run() {
        try {
            initializeConnection();
            handleIncomingPackets();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("ClientHandler error for " + clientIP + ": " + e.getMessage());
            config.printSeparator();
        } finally {
            cleanup();
        }
    }

//-----SOCKET AND CONNECTION-----
    private void initializeConnection() throws IOException {
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
        connected = true;
    }

//-----CLIENT METHODS-----
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
                if(packet.protocol == Protocols.DHCP){//For connecting clients
                    processor.handleDHCP(packet, this);
                } else if(packet.protocol == Protocols.DISCONNECT){//For disconnecting clients
                    System.out.println("Received DISCONNECT packet from " + clientIP);

                    Segment ackSeg = new Segment(processor.getRouterIP(), clientIP);
                    Packet ackPacket = new Packet(processor.getRouterIP(), clientIP, Protocols.DISCONNECT_ACK, -1, -1, ackSeg);
                    ackPacket.assignChecksum();
                    sendPacket(ackPacket);

                    break;
                } else{ 
                    processor.onPacketReceived(packet); //Handles other packets not for the router
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
            config.printSeparator();
            System.out.println("Failed to send packet to " + clientIP);
        }
    }

//-----UTILS-----
    public void cleanup() {
        try {
            if (clientIP != null) {
                processor.onClientDisconnect(clientIP);
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            config.printSeparator();
            System.out.println("Error closing client connection: " + clientIP);
        }
    }
}
