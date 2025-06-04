import java.io.IOException;

public class NetworkSim {
    public static void main(String[] args) {
        //Start router
        Router router = new Router("192.168.1.1", "AA:BB:CC:DD:EE:FF");
        try {
            router.start(23456);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Client clientA = new Client("A-Client", "AA:BB:CC:00", "2.2.2.2", "localhost", 23456);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Sleep interrupted");
        }

        Client clientB = new Client("B-Client", "EE:FF:GG:11", "1.1.1.1","localhost", 23456);
        
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Sleep interrupted");
        }    
        
        //clientA.createTCPMessage("I am talking to you over a network!");
        //clientA.sendToRouter();

       // clientB.createTCPMessage("I am also talking to you!");
        //clientB.sendToRouter();

        
    }
}
