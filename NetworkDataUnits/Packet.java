package NetworkDataUnits;
import java.io.Serializable;

public class Packet implements Serializable{
    public String srcIP;
    public String destIP;
    public String protocol;
    Segment payload;

    public Packet(String src, String dest, String protocol, Segment seg){
        this.srcIP = src;
        this.destIP = dest;
        this.protocol = protocol;
        this.payload = seg;
    }

    public Segment getPayload(){
        return payload;
    }
}
