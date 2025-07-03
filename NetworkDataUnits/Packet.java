package NetworkDataUnits;
import java.io.Serializable;

public class Packet implements Serializable{
    public String srcIP;
    public String destIP;
    public String protocol;
    public int seqNum;
    public int ackNum;
    Segment payload;

    public Packet(String src, String dest, String protocol, int seqNum, int ackNum, Segment seg){
        this.srcIP = src;
        this.destIP = dest;
        this.protocol = protocol;
        this.payload = seg;
        this.seqNum = seqNum;
        this.ackNum = ackNum;
    }

    public Segment getPayload(){
        return payload;
    }
}
