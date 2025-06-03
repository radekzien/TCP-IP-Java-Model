package NetworkDataUnits;
import java.io.Serializable;

public class Segment implements Serializable{
    String sourcePort;
    String destPort;
    int seqNum;
    int ackNum;
    int checksum;
    Object payload;

    public Segment(String ip, String destIP){
        this.sourcePort = ip;
        this.destPort = destIP;
    }

    public void addPayload(Object payload){
        this.payload = payload;
    }

    public Object getPayload(){
        return(payload);
    }
}
