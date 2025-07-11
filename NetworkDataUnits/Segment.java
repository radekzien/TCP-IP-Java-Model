package NetworkDataUnits;
import java.io.Serializable;

public class Segment implements Serializable{
//-----VARIABLES-----
    //Header
    String sourcePort;
    String destPort;


    //Payload
    Object payload;


//-----CONSTRUCTOR-----
    public Segment(String ip, String destIP){
        this.sourcePort = ip;
        this.destPort = destIP;
    }

//-----PAYLOAD METHODS-----
    public void addPayload(Object payload){
        this.payload = payload;
    }

    public Object getPayload(){
        return(payload);
    }

//Copy constructor for error simulation
    public Segment(Segment original){
        this.sourcePort = original.sourcePort;
        this.destPort = original.destPort;
        this.payload = original.payload;  
    }
}
