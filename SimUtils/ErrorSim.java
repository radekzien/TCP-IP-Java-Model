package SimUtils;

import java.util.Random;

import NetworkDataUnits.Packet;

public class ErrorSim {
    Random random = new Random();
    SimConfig config = new SimConfig();

    public Packet addError(Packet packet){
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

    public Boolean isDropped(){
        return (random.nextDouble() < config.getDropChance());
    }
}
