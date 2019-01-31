package rubiks.ipl;

import ibis.ipl.*;
import java.io.IOException;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicInteger;

public class Client implements MessageUpcall {

    /*  In this implementation we only use upcall ports
     *  for the communication of the workers and the master.
     */
    PortType upCallPort = new PortType(PortType.COMMUNICATION_RELIABLE,
            PortType.SERIALIZATION_OBJECT_IBIS, PortType.RECEIVE_AUTO_UPCALLS,
            PortType.CONNECTION_MANY_TO_ONE,
            PortType.CONNECTION_UPCALLS);

    
    private CubeCache cache;
    private SendPort sender;
    private ReceivePort receiver;
    
    public static final boolean PRINT_SOLUTION = false;

    public Client(IbisIdentifier masterIbisId) throws IOException{
        
        /*  Create a SendPort to communicate with Master
         *  and connect.
         */
        System.out.println("I am ibis " + Rubiks.ibis.identifier() + " and I will initialize ports to master Id: " + masterIbisId);
    
        sender = Rubiks.ibis.createSendPort(upCallPort);
        sender.connect(masterIbisId, "master");
        System.out.println("I am ibis " + Rubiks.ibis.identifier() + " and I initialized ports");

        /*  Create a receive port to receive messages from master.
         */
        receiver = Rubiks.ibis.createReceivePort(upCallPort, "worker", this);
        cache = null;
    }

     /*  Send an integer to Master. -1 upon start to signal availability
     *  the computed solutions otherwise.
     */
    private void sendInt(int x) throws IOException{
        WriteMessage w = sender.newMessage();
        w.writeInt(x);
        w.finish();
    }

    @Override
    public void upcall(ReadMessage rm) throws IOException, ClassNotFoundException {

        /*  The upcall method. If Worker receives an "execute" command. solves the received
         *  board and returns the result. If receives a close command frees the ports and
         *  shutsdown.
         */
        int sols;
        Cube cb;
        String command = rm.readString();
        
        System.out.println("I am ibis: " + Rubiks.ibis.identifier() + " and I received " + command);
        if (command.equals("execute")){
            cb = (Cube) rm.readObject();
            rm.finish();
            sols = solutions(cb,cache);
            sendInt(sols);
        } else if(command.equals("close")){
            rm.finish();
            sender.close();
            receiver.close();
            synchronized(this) {
                this.notify();
            }
        }
    }

    /**
     * Recursive function to find a solution for a given cube. Only searches to
     * the bound set in the cube object.
     * 
     * @param cube
     *            cube to solve
     * @param cache
     *            cache of cubes used for new cube objects
     * @return the number of solutions found
     */
    private static int solutions(Cube cube, CubeCache cache) {
        if (cube.isSolved()) {
            return 1;
        }

        if (cube.getTwists() >= cube.getBound()) {
            return 0;
        }

        // generate all possible cubes from this one by twisting it in
        // every possible way. Gets new objects from the cache       
        Cube[] children = cube.generateChildren(cache);

        int result = 0;
        
        for (Cube child : children) { 
            // recursion step
            int childSolutions = solutions(child, cache);
            if (childSolutions > 0) {
                result += childSolutions;
                if (PRINT_SOLUTION) {
                    child.print(System.err);
                }
            }
            // put child object in cache
            cache.put(child);
        }

        return result;
    }

    
    public void run() throws Exception {
        // enable connections
        receiver.enableConnections();

        // enable upcalls
        receiver.enableMessageUpcalls();

        // Send an initialization message
        System.out.println("I am ibis " + Rubiks.ibis.identifier() + " and I will send a ready message");
        sendInt(-1);
        

        // Make sure this thread doesn't finish prematurely
        synchronized (this) {
            this.wait();
        }
    }
}