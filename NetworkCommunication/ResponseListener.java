package NetworkCommunication;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

import NetworkDataUnits.ClientListPayload;
import NetworkDataUnits.Packet;
import NetworkDataUnits.Segment;
import NetworkUtils.Protocols;
import SimUtils.SimConfig;

public class ResponseListener extends Thread {
//-----VARIABLES-----
    SimConfig config = new SimConfig();

    private Socket socket;
    private ObjectInputStream in;
    private boolean running = true;
    private ClientCallback callback;

//-----CONSTRUCTOR AND RUNNING METHOD-----
    public ResponseListener(Socket socket, ClientCallback callback){
        this.socket = socket;
        this.callback = callback;
        try {
            this.in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            System.out.println("Failed to initialize input stream");
            e.printStackTrace();
        }
    }
    @Override
    public void run() {
        try {
            while (running && !socket.isClosed()) {
                try {
                    Object response = in.readObject();

                    if (response instanceof Packet packet) {
                        config.printSeparator();
                        System.out.println(packet.protocol + " packet received from " + packet.srcIP);
                        if(config.printPackets){
                            System.out.println(packet.toString());
                        }

                        //-----HANDLE BCAST PACKETS-----
                        if(packet.protocol == Protocols.BCAST){
                            Segment resSeg = packet.getPayload();
                            Object payload = resSeg.getPayload();

                            if(payload instanceof ClientListPayload clientList){
                                callback.onClientListUpdated(clientList.getClientList());
                            }

                        //-----HANDLE DHCP_ACK/_NACK PACKETS-----
                        } else if(packet.protocol == Protocols.DHCP_ACK || packet.protocol == Protocols.DHCP_NACK){
                            callback.processDHCP(packet);
                        } else if(packet.protocol == Protocols.DISCONNECT_ACK){
                                config.printSeparator();
                                System.out.println("Received DISCONNECT-ACK from " + packet.srcIP);
                                callback.onDisconnectACK();
                        
                        //-----HANDLE TCP PACKETS-----
                        } else if (packet.protocol == Protocols.TCP){
                            if(packet.checksum == packet.computeChecksum()){//Compute checksum
                                if(packet.seqNum == callback.getExpSeqNum(packet.srcIP)){//Check Seqnum
                                    callback.processTCP(packet);
                                } else {
                                    config.printSeparator();
                                    System.out.println("Unexpected SeqNum from " + packet.srcIP +". Expected " + callback.getExpSeqNum(packet.srcIP) + " received " + packet.seqNum);
                                }
                            } else {
                                config.printSeparator();
                                System.out.println("Invalid checksum from " + packet.srcIP + ". Expected " + packet.checksum + " Received " + packet.computeChecksum());
                            }

                        //-----HANDLE TCP_ACK PACKETS-----  
                        } else if(packet.protocol == Protocols.TCP_ACK){
                            if(packet.checksum == packet.computeChecksum()){
                                callback.processTCPACK(packet);
                            } else {
                                config.printSeparator();
                                System.out.println("Invalid checksum from " + packet.srcIP);
                            }
                        }
                    }

                    
                } catch (EOFException e) {
                    config.printSeparator();
                    System.out.println("Connection closed by router");
                    break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            if (running) {
                config.printSeparator();
                callback.handleRouterDisconnect();
            }
        }
    }

//-----UTILS-----
    public void shutdown(){
        try {
            running = false;
            if (in != null) {
                in.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Error while closing resources.");
            e.printStackTrace();
        }
    }
}
