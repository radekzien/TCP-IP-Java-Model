package NetworkCommunication;

import java.util.concurrent.ConcurrentMap;

public interface ClientCallback {
 void onClientListUpdated(ConcurrentMap<String, String> newConcurrentMap);
}