package SimUtils;

import java.util.Random;

import NetworkDataUnits.Packet;

public class ErrorSim {
//-----VARIABLES-----
    Random random = new Random();
    SimConfig config = new SimConfig();

//-----ERROR SIMULATION METHODS-----
    public Packet addError(Packet packet){//Simulates packet corruption
        Packet corruptedPac = new Packet(packet);
        if(random.nextDouble() < config.getErrorChance()){
            int choice = random.nextInt(3);
            switch(choice){
                case 0:
                    corruptedPac.checksum = corruptedPac.checksum + 1;
                    break;
                case 1:
                    corruptedPac.seqNum = corruptedPac.seqNum + 1;
                    break;
                case 2:
                    corruptedPac.ackNum = corruptedPac.ackNum + 1;
                    break;
            }
        }
        return corruptedPac;
    }

    public Boolean isDropped(){//Simulates packet loss
        return (random.nextDouble() < config.getDropChance());
    }
}
