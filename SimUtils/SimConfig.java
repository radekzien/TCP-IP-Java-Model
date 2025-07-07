package SimUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SimConfig {

    public static int addressAmount = 4; //INTEGER: Determines size of address space
    public static String routerHost = "0.0.0.0"; //STRING: Host on which router runs
    public static int routerPort = 12345; //INTEGER: Port on which router runs
    public static String networkIP = "192.168.1."; //STRING: Network prefix. Router always has IP x.x.x.1, clients have x.x.x.2 - 255
    public static double errorChance = 0.1; //DOUBLE: Chance of corrupted packet

    public String getHost(){
        return(routerHost);
    }

    public int getAmount(){
        return(addressAmount);
    }

    public int getPort(){
        return(routerPort);
    }

    public String getNetworkIP(){
        return(networkIP);
    }

    public double getErrorChance(){
        return(errorChance);
    }

    public void printSeparator(){
        LocalDateTime now = LocalDateTime.now();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss:SSS");

        String time = now.format(formatter);
        System.out.println("\r\n------------------------ " + time + "\r\n");
    }
}