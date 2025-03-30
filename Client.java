public class Client {
    String ip;
    String mac;

    String destIP; //Hardcoded for now
    String destMAC;

    String message;

    Segment seg;
    Packet pac;
    public void createMessage(String msg){
        this.message = msg;
    }

    public void createSegment(){
        seg = new Segment(ip, destIP);
        seg.addPayload(message);
    }

    public void createPacket(){
        pac = new Packet(ip, destIP, seg);
    }

}
