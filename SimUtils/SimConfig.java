package SimUtils;

public class SimConfig {

    public static int addressAmount = 4; //Determines size of address space
    public static String routerHost = "0.0.0.0"; //Host on which router runs
    public static int routerPort = 12345; //Port on which router runs
    public static String networkIP = "192.168.1."; //Network prefix. Router always has IP x.x.x.1, clients have x.x.x.2 - 255

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
}