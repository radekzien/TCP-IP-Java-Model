public class DataLinkLayer {
    public void transmitData(String data){
        System.out.println("Daya Link Layer: Transmitting data");
        String frame = "Ethernet Frame " + data;
        physicalLayerTransmit(frame);
    }

    private void physicalLayerTransmit(String data) {
        System.out.println("Physical Layer: Sending data...");
        System.out.println("Data transmitted: " + data);
    }
}
