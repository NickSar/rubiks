package rubiks.ipl;

import ibis.ipl.*;
import java.io.IOException;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicInteger;

public class Client implements MessageUpcall {
   
    private CubeCache cache;
    private SendPort sender;
    private ReceivePort receiver;
    public static Ibis ibis;
    private final Rubiks parent;

    public static final boolean PRINT_SOLUTION = false;

    public Client(Rubiks parent) throws IOException{
        this.parent = parent;       
        cache = null;
    }

     /*  Send an signal to Master. SG_WORKER_INITIALIZED upon start to signal availability
     *  the computed solutions otherwise.
     */
    private void sendInt(int x) throws IOException{
        WriteMessage w = sender.newMessage();
        w.writeInt(x);
        w.finish();
    }

    @Override
    public void upcall(ReadMessage rm) throws IOException, ClassNotFoundException {

        /*  The upcall method. If Worker receives an CMD_EXECUTE command it will solve the received
         *  cube and will return the result. If a CMD_CLOSE command is received then worker frees the ports and
         *  shuts down.
         */
        int sols;
        Cube cb;
        String command = rm.readString();
        System.out.println("received: " + command);
        if (command.equals(Rubiks.CMD_EXECUTE)){
            cb = (Cube) rm.readObject();
            rm.finish();
            if (cache == null){
                cache = new CubeCache(cb.getSize());
            }
            sols = solutions(cb,cache);
            sendInt(sols);
        } else if(command.equals(Rubiks.CMD_CLOSE)){
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

    
    public void run(IbisIdentifier masterIbisId) throws Exception {
        
        /*  Create a receive port to receive messages from master.
            Pass ourselves as the message upcall handler
         */
        receiver = parent.ibis.createReceivePort(Rubiks.upCallPort, "worker", this);
        receiver.enableConnections();
        receiver.enableMessageUpcalls();
        
        /*  Create a SendPort to communicate with Master
         *  and connect.
         */
        sender = parent.ibis.createSendPort(Rubiks.upCallPort);
        sender.connect(masterIbisId, "master");

        // Send an initialization message
        sendInt(Rubiks.SG_WORKER_INITIALIZED);
        
        // Make sure this thread doesn't finish prematurely
        synchronized (this) {
            this.wait();
        }
    }
}