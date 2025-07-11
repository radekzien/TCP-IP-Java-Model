package SimUtils;

import java.util.*;


public class MACAssigner{
//-----VARIABLES-----
    private Set<String> existingMACs =  new HashSet<>(); //Tracks existing MAC addresses - ensures uniqueness
    private static Random rand =  new Random();

//-----METHODS-----
    private static String getHex() {
        int value = rand.nextInt(256);
        return String.format("%02X", value);
    }

    public String assignMAC(){
        String mac;
        do {
            mac = getHex() + ":" + getHex() + ":" + getHex();
        } while(existingMACs.contains(mac));

        existingMACs.add(mac);
        return mac;
    }
}