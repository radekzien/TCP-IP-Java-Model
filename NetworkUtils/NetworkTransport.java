package NetworkUtils;

import java.io.IOException;

import NetworkDataUnits.Packet;

public interface NetworkTransport {
    void sendPacket(Packet packet) throws IOException;
    void start() throws IOException;
    void stop() throws IOException;
    void setPacketListener(PacketListener listener);
    interface PacketListener {
        void onPacketReceived(Packet packet);
    }
}