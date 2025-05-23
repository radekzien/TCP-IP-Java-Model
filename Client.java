import java.io.*;
import java.net.Socket;

import NetworkCommunication.ResponseListener;
import NetworkDataUnits.DataUnitHandler;
import NetworkDataUnits.Packet;
import NetworkDataUnits.Segment;

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
    private ResponseListener listener;

    //Constructor
    public Client(String ip, String mac, String destIP, String routerHost, int routerPort){
        this.ip = ip;
        this.mac = mac;
        this.destIP = destIP;
        this.routerHost = routerHost;
        this.routerPort = routerPort;
        
        try {//Register with router upon instantiation
            socket = new Socket(routerHost, routerPort);
            out = new ObjectOutputStream(socket.getOutputStream());

            out.writeObject(ip);
            out.flush();

            listener = new ResponseListener(socket);
            listener.start();
        } catch (IOException e){
            System.out.println("Client " + ip + " failed to connect to router");
        }
    }

    //Internal Methods - Creating messages, frames, packets etc
    public void createMessage(String msg){
        DataUnitHandler duh = new DataUnitHandler();
        this.message = msg;
        seg = duh.createSegment(ip, destIP, msg);
        pac = duh.createPacket(ip, destIP, seg);
    }

    public void sendToRouter(){
        if (socket == null || socket.isClosed()) {
            System.out.println("Cannot send: socket is closed");
            return;
        }
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
            if(listener != null){
                listener.shutdown();
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
