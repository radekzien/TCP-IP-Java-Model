public class TransportLayer {
    private NetworkLayer networkLayer;

    public TransportLayer() {
        this.networkLayer = new NetworkLayer();
    }

    public void sendData(String data){
        System.out.println("Transport Layer: Segmenting Data...");
        String segment = "TCP segment: " + data;
        System.out.println("Transport Layer: Sending segment: " + segment);
        networkLayer.routeData(segment);
    }
}
