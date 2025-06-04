package NetworkCommunication;

import java.util.concurrent.ConcurrentMap;

import NetworkDataUnits.Packet;

public interface ClientCallback {
 void onClientListUpdated(ConcurrentMap<String, String> newConcurrentMap);
 void processDHCP(Packet packet);
}