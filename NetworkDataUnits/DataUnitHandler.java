package NetworkDataUnits;

public class DataUnitHandler {
    public Segment createSegment(String ip, String destIP, String message){
        Segment seg = new Segment(ip, destIP);
        seg.addPayload(message);
        return seg;
    }

    public Packet createPacket(String ip, String destIP, String protocol, Segment seg){
        Packet pac = new Packet(ip, destIP, protocol, seg);
        return pac;
    }
}
