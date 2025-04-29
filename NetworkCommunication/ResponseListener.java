package NetworkCommunication;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

import NetworkDataUnits.Packet;

public class ResponseListener extends Thread {
    private Socket socket;
    private ObjectInputStream in;
    private boolean running = true;

    public ResponseListener(Socket socket){
        this.socket = socket;
    }
    @Override
    public void run(){
        try {
            in = new ObjectInputStream(socket.getInputStream());
            while(running && !socket.isClosed()){
                try{
                    Object response = in.readObject();

                if(response instanceof Packet){
                    Packet responseMessage = (Packet) response;
                    System.out.println(responseMessage.srcIP + ": " + responseMessage.getPayload().getPayload());
                }
                } catch (EOFException e) {
                    System.out.println("Connection closed by router");
                    break;
                }
                
            }
        } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                System.out.println("Failed to receive message from router");
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
