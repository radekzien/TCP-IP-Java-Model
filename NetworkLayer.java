public class NetworkLayer {
    private DataLinkLayer dataLinkLayer;

    public NetworkLayer(){
        this.dataLinkLayer = new DataLinkLayer();
    }

    public void routeData(String data){
        System.out.println("Network Layer: Routing data");
        String ipPacket = "IP Packet: " + data;
        System.out.println("Sending packet: " + ipPacket);
        dataLinkLayer.transmitData(ipPacket);
    }
}
