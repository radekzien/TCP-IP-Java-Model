package NetworkDataUnits;

public class DataUnitHandler {
    public Segment createSegment(String ip, String destIP, String message){
        Segment seg = new Segment(ip, destIP);
        seg.addPayload(message);
        return seg;
    }

    public Packet createPacket(String ip, String destIP, String protocol, int seq, int ack, Segment seg){
        Packet pac = new Packet(ip, destIP, protocol, seq, ack, seg);
        return pac;
    }
}
