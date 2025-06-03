package NetworkDataUnits;

import java.io.Serializable;
import java.util.concurrent.ConcurrentMap;

public class ClientListPayload implements Serializable{
    private final ConcurrentMap<String, String> clientList;

    public ClientListPayload(ConcurrentMap<String, String> clientList) {
        this.clientList = clientList;
    }

    public ConcurrentMap<String, String> getClientList() {
        return clientList;
    }
}
