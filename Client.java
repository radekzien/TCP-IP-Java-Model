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

    //Constructor
    public Client(String ip, String mac, String destIP, String routerHost, int routerPort){
        this.ip = ip;
        this.mac = mac;
        this.destIP = destIP;
        this.routerHost = routerHost;
        this.routerPort = routerPort;
        sendIPtoRouter(routerHost, routerPort);
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

    //Networking methods
    public void sendIPtoRouter(String routerHost, int routerPort){
        try(Socket socket = new Socket(routerHost, routerPort);
        
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            out.writeObject(ip);
            out.flush();
        } catch (IOException e){
            e.printStackTrace();
            System.out.println("Failed to send IP to router");
        }
    }

    public void sendToRouter(){
        try(Socket socket = new Socket(routerHost, routerPort);
        
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            out.writeObject(pac);
            
            //Process response
            Object response = in.readObject();
            if(response instanceof Packet){
                Packet reply = (Packet) response;
                System.out.println(reply.srcIP + ": " + reply.getPayload().getPayload());
            }

        } catch (IOException | ClassNotFoundException e){
            e.printStackTrace();
            System.out.println("Failed to communicate with router");
        }
        

    }

}
