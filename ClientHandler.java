import java.io.*;
import java.net.Socket;

public class ClientHandler extends Thread {
    private Socket socket;
    private Router router;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String clientIP;

    public ClientHandler(Socket socket, Router router) {
        this.socket = socket;
        this.router = router;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            Object ipObj = in.readObject();
            if (ipObj instanceof String) {
                clientIP = (String) ipObj;
                router.registerClient(clientIP, this);
            }

            while (true) {
                Object obj = in.readObject();
                if (obj instanceof Packet) {
                    Packet pac = (Packet) obj;
                    router.receivePacket(pac);
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Client disconnected: " + clientIP);
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
}
