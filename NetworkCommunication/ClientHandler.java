package NetworkCommunication;
import java.io.*;
import java.net.Socket;

import NetworkDataUnits.Packet;

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
            registerClient();
            handleIncomingPackets();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("ClientHandler error for " + clientIP + ": " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void initializeConnection() throws IOException {
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    private void registerClient() throws IOException, ClassNotFoundException {
        Object ipObj = in.readObject();
        if (ipObj instanceof String ip) {
            clientIP = ip;
            processor.onClientRegister(clientIP, this);
        } else {
            throw new IOException("Invalid client IP received.");
        }
    }

    private void handleIncomingPackets() throws IOException, ClassNotFoundException {
        while (connected && !socket.isClosed()) {
            Object obj = in.readObject();
            if (obj instanceof Packet packet) {
                processor.onPacketReceived(packet);
            }
        }
    }

    public void sendPacket(Packet pac) {
        try {
            out.writeObject(pac);
            out.flush();
        } catch (IOException e) {
            System.out.println("Failed to send packet to " + clientIP);
        }
    }

    private void cleanup() {
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
