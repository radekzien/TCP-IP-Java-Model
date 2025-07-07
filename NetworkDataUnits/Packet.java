package NetworkDataUnits;
import java.io.Serializable;
import java.util.Objects;

import NetworkUtils.Protocols;

public class Packet implements Serializable{
    public String srcIP;
    public String destIP;
    public Protocols protocol;
    public int seqNum;
    public int ackNum;
    public int checksum;
    Segment payload;

    public Packet(String src, String dest, Protocols protocol, int seqNum, int ackNum, Segment seg){
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
}
