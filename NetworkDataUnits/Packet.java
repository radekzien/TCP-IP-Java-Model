package NetworkDataUnits;
import java.io.Serializable;

public class Packet implements Serializable{
    public String srcIP;
    public String destIP;
    Segment payload;

    public Packet(String src, String dest, Segment seg){
        this.srcIP = src;
        this.destIP = dest;
        this.payload = seg;
    }

    public Segment getPayload(){
        return payload;
    }
}
