import java.util.ArrayList;

public class Router {
    ArrayList<Client> clients = new ArrayList<>();
    
    String ip;
    String mac;

    ArrayList<Packet>inBuffer = new ArrayList<>();
    ArrayList<Packet>outBuffer = new ArrayList<>();//Simplified for now

    public void receivePacket(Packet pac){
        inBuffer.add(pac);
    }

    public void switchPacket(){ //Checks will be added later
        Packet pac = inBuffer.getFirst();
        inBuffer.remove(0);
        outBuffer.add(pac);
    }

    public void sendPacket(){

    }

    public void Main(){
        if(!inBuffer.isEmpty())
            while(!inBuffer.isEmpty()){
                switchPacket();
            }
        if(!outBuffer.isEmpty()){
            sendPacket();
        }
    }
}
