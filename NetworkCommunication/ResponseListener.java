package NetworkCommunication;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

import NetworkDataUnits.ClientListPayload;
import NetworkDataUnits.Packet;
import NetworkDataUnits.Segment;
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
                        if("BCAST".equals(packet.protocol)){
                            Segment resSeg = packet.getPayload();
                            Object payload = resSeg.getPayload();

                            if(payload instanceof ClientListPayload clientList){
                                callback.onClientListUpdated(clientList.getClientList());
                            }
                        } else if("DHCP-ACK".equals(packet.protocol)){
                            callback.processDHCP(packet);
                        } else if("DISCONNECT-ACK".equals(packet.protocol)){
                                System.out.println("Received DISCONNECT-ACK from " + packet.srcIP);
                                config.printSeparator();
                                callback.onDisconnectACK();
                        } else if ("TCP".equals(packet.protocol)){
                            if(packet.seqNum == callback.getExpSeqNum()){
                                callback.processTCP(packet);
                            } else {
                                System.out.println("Unexpected SeqNum from " + packet.srcIP);
                            }
                        } else if("TCP-ACK".equals(packet.protocol)){
                            if(callback.isAck(packet.ackNum)){
                                System.out.println("ACK received: " + packet.ackNum + " from " + packet.srcIP);
                                //Remove Ack from expectedACKs
                                //Stop timer
                                //Remove packet from transit list
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
                e.printStackTrace();
                System.out.println("Failed to receive message from router");
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
