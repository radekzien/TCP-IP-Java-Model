public class AppLayer {
    private TransportLayer transportLayer;

    public AppLayer(){
        this.transportLayer = new TransportLayer();
    }

    public void sendMessage(String message){
        System.out.println("Application Layer: sending \"" + message + "\"");
        transportLayer.sendData(message);
    }
}