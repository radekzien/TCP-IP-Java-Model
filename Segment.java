import java.io.Serializable;

public class Segment implements Serializable{
    String sourcePort;
    String destPort;
    int seqNum;
    int ackNum;
    int checksum;
    String payload;

    public Segment(String ip, String destIP){
        this.sourcePort = ip;
        this.destPort = destIP;
    }

    public void addPayload(String message){
        this.payload = message;
    }

    public String getPayload(){
        return(payload);
    }
}
