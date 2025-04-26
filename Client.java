import java.io.*;
import java.net.Socket;

public class Client {
    String ip;
    String mac;

    String destIP; //Hardcoded for now
    String destMAC;

    String message;

    String routerHost;//Hardcoded for now
    int routerPort;

    Segment seg;
    Packet pac;

    private Socket socket;
    private ObjectOutputStream out;

    //Constructor
    public Client(String ip, String mac, String destIP, String routerHost, int routerPort){
        this.ip = ip;
        this.mac = mac;
        this.destIP = destIP;
        this.routerHost = routerHost;
        this.routerPort = routerPort;
        
        try {
            socket = new Socket(routerHost, routerPort);
            out = new ObjectOutputStream(socket.getOutputStream());

            out.writeObject(ip);
            out.flush();

            new Thread(new ResponseListener(socket)).start();;
        } catch (IOException e){
            System.out.println("Client " + ip + " failed to connect to router");
        }
    }

    //Internal Methods - Creating messages, frames, packets etc
    public void createMessage(String msg){
        this.message = msg;
    }

    public void createSegment(){
        seg = new Segment(ip, destIP);
        seg.addPayload(message);
    }

    public void createPacket(){
        pac = new Packet(ip, destIP, seg);
    }

    public void sendToRouter(){
        try{
            out.writeObject(pac);
            out.flush();
            
        } catch (IOException e){
            e.printStackTrace();
            System.out.println("Failed to communicate with router");
        }
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
