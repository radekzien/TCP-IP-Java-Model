package SimUtils;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;


public class MACAssigner{
    private Set<String> existingMACs =  new HashSet<>();
    private static Random rand =  new Random();

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