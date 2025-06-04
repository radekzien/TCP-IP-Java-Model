package NetworkCommunication;

import NetworkDataUnits.Packet;

public interface PacketProcessor {
    void onClientRegister(String ip, ClientHandler handler, String hostName);
    void onPacketReceived(Packet packet);
    void onClientDisconnect(String ip);
    String allocateAddress();
    String getRouterIP();
    void handleDHCP(Packet packet);
}
