package NetworkCommunication;

import java.util.concurrent.ConcurrentMap;

import NetworkDataUnits.Packet;

public interface ClientCallback {
 void onClientListUpdated(ConcurrentMap<String, String> newConcurrentMap);
 void processDHCP(Packet packet);
 void sendToApp(String ip, Object message);
 void onDisconnectACK();
 void processTCP(Packet packet);
 boolean isAck(String ip, int ackNum);
 int getExpSeqNum();
 void processTCPACK(Packet packet);
}