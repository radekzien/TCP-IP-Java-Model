package NetworkCommunication;

import NetworkDataUnits.Packet;

public interface PacketProcessor {
    void onClientRegister(String ip, ClientHandler handler);
    void onPacketReceived(Packet packet);
    void onClientDisconnect(String ip);
}
