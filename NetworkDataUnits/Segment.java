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

        @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(sourcePort).append(destPort)
        .append(seqNum).append(ackNum);

        if (payload != null) {
            sb.append(payload.toString());
        }
        return sb.toString();
    }

    public Segment(Segment original){
        this.sourcePort = original.sourcePort;
        this.destPort = original.destPort;
        this.seqNum = original.seqNum;
        this.ackNum = original.ackNum;
        this.checksum = original.checksum;
        this.payload = original.payload;  
    }
}
