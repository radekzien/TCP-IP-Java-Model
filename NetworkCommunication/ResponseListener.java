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
    SimConfig config = new SimConfig();
    private Socket socket;
    private ObjectInputStream in;
    private boolean running = true;
    private ClientCallback callback;

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
                        System.out.println(packet.protocol + " packet received from " + packet.srcIP);
                        config.printSeparator();
                        if(packet.protocol == Protocols.BCAST){
                            Segment resSeg = packet.getPayload();
                            Object payload = resSeg.getPayload();

                            if(payload instanceof ClientListPayload clientList){
                                callback.onClientListUpdated(clientList.getClientList());
                            }
                        } else if(packet.protocol == Protocols.DHCP_ACK || packet.protocol == Protocols.DHCP_NACK){
                            callback.processDHCP(packet);
                        } else if(packet.protocol == Protocols.DISCONNECT_ACK){
                                System.out.println("Received DISCONNECT-ACK from " + packet.srcIP);
                                config.printSeparator();
                                callback.onDisconnectACK();
                        } else if (packet.protocol == Protocols.TCP){
                            if(packet.checksum == packet.computeChecksum()){
                                if(packet.seqNum == callback.getExpSeqNum(packet.srcIP)){
                                    callback.processTCP(packet);
                                } else {
                                    System.out.println("Unexpected SeqNum from " + packet.srcIP);
                                }
                            } else {
                                System.out.println("Invalid checksum");
                            }
                        } else if(packet.protocol == Protocols.TCP_ACK){
                            if(packet.checksum == packet.computeChecksum()){
                                callback.processTCPACK(packet);
                            } else {
                                    System.out.println("Invalid checksum");
                            }
                        } else {
                            System.out.println(packet.srcIP + ": " + packet.getPayload().getPayload());
                           
                        }
                    }
                } catch (EOFException e) {
                    System.out.println("Connection closed by router");
                    config.printSeparator();
                    break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            if (running) {
                callback.handleRouterDisconnect();
                config.printSeparator();
            }
        }
    }
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
