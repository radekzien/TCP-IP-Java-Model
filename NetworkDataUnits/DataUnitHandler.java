package NetworkDataUnits;

public class DataUnitHandler {
    public Segment createSegment(String ip, String destIP, String message){
        Segment seg = new Segment(ip, destIP);
        seg.addPayload(message);
        return seg;
    }

    public Packet createPacket(String ip, String protocol, String destIP, Segment seg){
        Packet pac = new Packet(ip, protocol, destIP, seg);
        return pac;
    }
}
