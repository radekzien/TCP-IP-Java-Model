package NetworkDataUnits;

import java.io.Serializable;
import java.util.concurrent.ConcurrentMap;

public class ClientListPayload implements Serializable{
//-----VARIABLES-----
    private final ConcurrentMap<String, String> clientList;

//-----CONSTRUCTOR-----
    public ClientListPayload(ConcurrentMap<String, String> clientList) {
        this.clientList = clientList;
    }

//-----GETTERS-----
    public ConcurrentMap<String, String> getClientList() {
        return clientList;
    }
}
