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

    public final static IbisCapabilities ibisCapabilities = new IbisCapabilities(
                        IbisCapabilities.ELECTIONS_STRICT,
                        IbisCapabilities.CLOSED_WORLD,
                        IbisCapabilities.TERMINATION);
    
    public final static PortType upCallPort = new PortType(PortType.COMMUNICATION_RELIABLE,
                        PortType.SERIALIZATION_OBJECT_IBIS, PortType.RECEIVE_AUTO_UPCALLS,
                        PortType.CONNECTION_MANY_TO_ONE,
                        PortType.CONNECTION_UPCALLS);
    
    public final Ibis ibis;
    private final IbisIdentifier masterId;
    
    public static final int SG_WORKER_INITIALIZED = -1;
    public final static String CMD_EXECUTE = "execute";
    public final static String CMD_CLOSE = "close";
    public final static String WK_READY = "ready";
    public final static String WK_BUSY = "busy";

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
            new Master(this).run(args);
        } else {
            new Client(this).run(masterId);
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