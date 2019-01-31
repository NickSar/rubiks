package rubiks.ipl;

import ibis.ipl.*;

import java.io.IOException;

/**
 * Parallel Implementation for rubik's cube puzzle solver
 * 
 * @author Nikos Sarris
 * 
 */

public class Rubiks {

    IbisCapabilities ibisCapabilities = new IbisCapabilities(
                        IbisCapabilities.ELECTIONS_STRICT,
                        IbisCapabilities.CLOSED_WORLD,
                        IbisCapabilities.TERMINATION);
    
    PortType upCallPort = new PortType(PortType.COMMUNICATION_RELIABLE,
                        PortType.SERIALIZATION_OBJECT_IBIS, PortType.RECEIVE_AUTO_UPCALLS,
                        PortType.CONNECTION_MANY_TO_ONE,
                        PortType.CONNECTION_UPCALLS);
    
    public static Ibis ibis;
    private final IbisIdentifier masterId;
    
    Rubiks() throws IbisCreationFailedException, IOException {

        // Create Ibis
        ibis = IbisFactory.createIbis(ibisCapabilities, null, upCallPort);

        // Postpone the elections until all ibises have joined
        ibis.registry().waitUntilPoolClosed();

        // Elect the Master
        masterId = ibis.registry().elect("Server");
    }

    private void run(String[] args) throws Exception {
        
        IbisIdentifier myId = ibis.identifier();

        if (masterId.equals(myId)) {
            new Master(masterId).run(args);
        } else {
            new Client(masterId).run();
        }
    }
    public static void main(String args[]){
        
        try {
            new Rubiks().run(args);
        } catch(Exception e) {
            e.printStackTrace(System.err);
        }
    }
}