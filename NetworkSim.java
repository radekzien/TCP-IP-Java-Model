import java.io.IOException;

public class NetworkSim {
    public static void main(String[] args) {
        //Start router
        Router router = new Router();
        try {
            router.start(12345);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Client clientA = new Client("1.1.1.1", "AA:BB:CC:00", "2.2.2.2", "localhost", 12345);
        Client clientB = new Client("2.2.2.2", "EE:FF:GG:11", "1.1.1.1","localhost", 12345);

        clientA.createMessage("I am talking to you over a network!");
        clientA.createSegment();
        clientA.createPacket();
        clientA.sendToRouter();

        clientB.createMessage("I am also talking to you!");
        clientB.createSegment();
        clientB.createPacket();
        clientB.sendToRouter();

        
    }
}
