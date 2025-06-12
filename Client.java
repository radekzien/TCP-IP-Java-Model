import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.swing.SwingUtilities;

import NetworkCommunication.ClientCallback;
import NetworkCommunication.ResponseListener;
import NetworkDataUnits.DataUnitHandler;
import NetworkDataUnits.Packet;
import NetworkDataUnits.Segment;

public class Client  implements ClientCallback{
//----- VARIABLES -----
    String hostName;
    String ip = "0.0.0.0";
    String mac;

    String destIP; //Hardcoded for now
    String destMAC;

    String message;

    String routerHost;
    int routerPort;

    Segment seg;
    Packet pac;

    String routerIP = "";

    private Socket socket;
    private ObjectOutputStream out;
    private ResponseListener listener;
    private DataUnitHandler duh = new DataUnitHandler();
    private ConcurrentMap<String, String> connectionList = new ConcurrentHashMap<>();

    ClientGUI clientGUI;

//----- MAIN -----
    public static void main(String[] args) {
        if(args.length != 2){
            System.out.println("Usage: java Client <hostName> <mac>");
            return;
        }

        String hostName = args[0];
        String mac = args[1];
        String routerHost = "0.0.0.0";
        int routerPort = 12345;

        Client client = new Client(hostName, mac, "0.0.0.0", routerHost, routerPort);
    }

//----- CONSTRUCTOR -----
    public Client (String hostName, String mac, String destIP, String routerHost, int routerPort){
        this.hostName = hostName;
        this.mac = mac;
        this.destIP = destIP;
        this.routerHost = routerHost;
        this.routerPort = routerPort;
        
        try {//Register with router upon instantiation
            socket = new Socket(routerHost, routerPort);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();

            listener = new ResponseListener(socket, this);
            listener.start();

            seg = duh.createSegment(ip, "255.255.255.255", hostName);
            pac = duh.createPacket(ip, "255.255.255.255", "DHCP", seg);

            out.writeObject(pac);
            out.flush();

        } catch (IOException e){
            System.out.println("Client " + ip + " failed to connect to router: " + e.getMessage());
        }
    }

// ----- MESSAGING AND MESSAGE HANDLING -----
    public void createTCPMessage(String msg){
        this.message = msg;
        seg = duh.createSegment(ip, destIP, msg);
        pac = duh.createPacket(ip, destIP, "TCP", seg);

        System.out.println("Created Packet: \nSender IP: " + ip + "\n" + "Destination IP: " + destIP + "\n" +"Protocol: " + pac.protocol + "Segment Payload: " + seg.getPayload());
    }

    public void sendToRouter(){
        if (socket == null || socket.isClosed()) {
            System.out.println("Cannot send: socket is closed");
            return;
        }
        try{
            out.writeObject(pac);
             System.out.println("Sent Packet: \nSender IP: " + pac.srcIP + "\n" + "Destination IP: " + pac.destIP + "\n" +"Protocol: " + pac.protocol + "\n" + "Segment Payload: " + pac.getPayload().getPayload().toString());
            out.flush();
           
            
        } catch (IOException e){
            e.printStackTrace();
            System.out.println("Failed to communicate with router");
        }
    }

        @Override
    public void processDHCP(Packet packet){
        Segment seg = packet.getPayload();
        Object payload = seg.getPayload();
        routerIP = packet.srcIP;
        if(payload instanceof String){
            ip = (String) payload;
            SwingUtilities.invokeLater(() -> clientGUI = new ClientGUI(this));
        }

    }

    @Override
    public void sendToApp(String ip, Object message){
        if(message instanceof String){
            clientGUI.receiveMessage(ip, (String) message);
        } else {
            return; //Handle this somehow
        }
    }

//----- CLIENT SYSTEM -----
    @Override
    public void onClientListUpdated(ConcurrentMap<String, String> newList) {
        connectionList.clear();
        connectionList.putAll(newList);
        System.out.println(hostName + " updated connection list:");
        connectionList.forEach((ip, name) ->
            System.out.println(" - " + ip + " (" + name + ")")
            
        );
        if(clientGUI != null){
            clientGUI.updateClientList(newList);
        }
    }

    public ConcurrentMap<String, String> getConnectionList(){
        return(connectionList);
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
