package NetworkUtils;

import NetworkDataUnits.Packet;

public interface PacketListener {
    void onPacketReceived(Packet packet);
}
