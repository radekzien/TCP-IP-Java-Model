package NetworkDataUnits;
import java.io.Serializable;

public class Frame implements Serializable{
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
