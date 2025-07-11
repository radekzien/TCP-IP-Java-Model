package NetworkDataUnits;
import java.io.Serializable;
import java.util.Objects;

import NetworkUtils.Protocols;

public class Packet implements Serializable{
//-----VARIABLES-----
    //Header
    public String srcIP;
    public String destIP;
    public Protocols protocol;
    public int seqNum;
    public int ackNum;
    public int checksum;

    //Payload
    Segment payload;

//-----CONSTRUCTOR-----
    public Packet(String src, String dest, Protocols protocol, int seqNum, int ackNum, Segment seg){
        this.srcIP = src;
        this.destIP = dest;
        this.protocol = protocol;
        this.payload = seg;
        this.seqNum = seqNum;
        this.ackNum = ackNum;
    }

//-----------
    public Segment getPayload(){
        return payload;
    }

    public int computeChecksum() {
        String data = srcIP + destIP + protocol.name() + seqNum + ackNum;
        if (payload != null) {
            data += payload.toString();
        }

        byte[] bytes = data.getBytes();
        int sum = 0;

        for (byte b : bytes) {
            sum += (b & 0xFF);
        }

        return ~sum & 0xFFFF;
    }

    public void assignChecksum(){
        this.checksum = computeChecksum(); 
    }

    public String toString(){
        return
            "From: " + srcIP + "\n" +
            "To: " + destIP + "\n" +
            "Protocol " + protocol + "\n" +
            "seqNum " + seqNum + "\n" +
            "ackNum " + ackNum;
    }

    @Override
    public boolean equals(Object o) { //For checking ACK Packets
        if (this == o) return true;
        if (!(o instanceof Packet)) return false;
        Packet packet = (Packet) o;
        return seqNum == packet.seqNum &&
               Objects.equals(destIP, packet.destIP);
    }

    @Override
    public int hashCode() {
        return Objects.hash(destIP,seqNum);
    }

    //Constructor for copy intended for error simulation
    public Packet(Packet original) {
        this.srcIP = original.srcIP;
        this.destIP = original.destIP;
        this.protocol = original.protocol;
        this.seqNum = original.seqNum;
        this.ackNum = original.ackNum;
        this.checksum = original.checksum;
        this.payload = new Segment((Segment)original.payload); 
    }
}
