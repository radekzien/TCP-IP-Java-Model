public class Frame {
    String srcMAC;
    String destMAC;
    Packet payload;

    public Frame(String src, String dest, Packet pac){
        this.srcMAC = src;
        this.destMAC = dest;
        this.payload = pac;
    }

    public Packet getPayload(){
        return(payload);
    }
}
