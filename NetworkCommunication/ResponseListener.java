package NetworkCommunication;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

import NetworkDataUnits.Packet;

public class ResponseListener extends Thread {
    private Socket socket;
    private ObjectInputStream in;

    public ResponseListener(Socket socket){
        this.socket = socket;
    }
    @Override
    public void run(){
        try {
            in = new ObjectInputStream(socket.getInputStream());
            while(true){
                Object response = in.readObject();

                if(response instanceof Packet){
                    Packet responseMessage = (Packet) response;
                    System.out.println(responseMessage.srcIP + ": " + responseMessage.getPayload().getPayload());
                }
            }
        } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                System.out.println("Failed to receive message from router");
            } finally {
                try {
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
}
