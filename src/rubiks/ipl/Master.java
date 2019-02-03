package rubiks.ipl;

import java.io.*;
import ibis.ipl.*;
import ibis.ipl.MessageUpcall;
import java.io.IOException;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Master class for the parallel implementation
 * 
 * @author Nikos Sarris
 */

public class Master implements MessageUpcall{

    PortType upCallPort = new PortType(PortType.COMMUNICATION_RELIABLE,
            PortType.SERIALIZATION_OBJECT_IBIS, PortType.RECEIVE_AUTO_UPCALLS,
            PortType.CONNECTION_MANY_TO_ONE,
            PortType.CONNECTION_UPCALLS);

    private CubeCache cache;
    private Cube firstCube;
    private IbisIdentifier myIbisId;

    private static ArrayDeque<Cube> childrenQueue;
    private static ArrayDeque<Worker> workerQueue;
    private static AtomicInteger totalSolutions;
    private static AtomicInteger busyWorkers;

    Master(IbisIdentifier myId) {
        cache = null;
        firstCube = null;
        childrenQueue = null;
        totalSolutions = new AtomicInteger(0);
        busyWorkers = new AtomicInteger(0);
        myIbisId = myId;
    }   

    public void upcall(ReadMessage message) throws IOException {

        Integer s = message.readInt();
        IbisIdentifier workerId = message.origin().ibisIdentifier();
        message.finish();
        Worker currentWorker;

        synchronized (workerQueue) {
            /* Distinguish two cases:
            *  1. if -1 is received this is translated to "i am ready" signal
            *     and a new worker is added to the workerQueue
            *  2. Otherwise the integer received is considered to be solutions
            *     and is added to the totalSolutions
            */
            System.out.println("I am Master and I received " + s + " from " + workerId);
            if (s == -1) {
                SendPort sp = Rubiks.ibis.createSendPort(upCallPort);
                sp.connect(workerId, "worker");
                currentWorker = new Worker(workerId, "ready", sp);
                workerQueue.addFirst(currentWorker);
            } else if (s >= 0) {
                totalSolutions.addAndGet(s);
                /* The worker that has just finished has to be set to status "ready"
                *  again in order to receive a new cube to solve */
                for (Iterator itr = workerQueue.iterator(); itr.hasNext(); ) {
                    currentWorker = (Worker) itr.next();
                    if (currentWorker.getId().equals(workerId)) {
                        currentWorker.setStatus("ready");
                        busyWorkers.decrementAndGet();
                    }
                }
            }
        }
    }
    /* Wrapper function to send a Cube (possibly), and a command
    * "execute" or "close" to the worker
    */
    public static void sendCubeToWorker(Cube cb, SendPort sPort) throws ConnectionFailedException, IOException {

        WriteMessage w = sPort.newMessage();

        w.writeString("execute");
        w.writeObject(cb);
        w.finish();
    }

    /* Iterate over the workerQueue to send cubes to "ready" workers
    *  as soon as our queue of cubes is not empty. */
    private void sendCube() throws ConnectionFailedException, IOException {

        Cube cb;
        int sols;
        int cubesPerWorker;
        synchronized(childrenQueue){
            synchronized(workerQueue){

                for (Worker currentWorker : workerQueue) {

                    if (currentWorker.getStatus().equals("ready")){

                        cubesPerWorker = currentWorker.getCubesProecessed();
                        currentWorker.setCubesProecessed(++cubesPerWorker);
                        cb = childrenQueue.pollLast();
                        sendCubeToWorker(cb, currentWorker.getSendPort());
                        busyWorkers.incrementAndGet();
                        currentWorker.setStatus("busy");
                        return;

                    }
                }
            }
        }
    }

    private  int solveAtBound (Cube cube, CubeCache cache) throws ConnectionFailedException, IOException {

        childrenQueue.clear();

        if(cube.isSolved()){
            totalSolutions.incrementAndGet();
            return totalSolutions.get();
        }

        if(cube.getTwists() >= cube.getBound()){
            return 0;
        }

        /*   Generating the children of the initial cube and add
         *   them to the global queue.
         */
        Cube[] children = cube.generateChildren(cache);
        int bound = cube.getBound();

        /* Add children cubes to the childrenQueue
        * Only three generations after the initial cube are added to the queue */
        for (Cube child : children){
            Cube[] grandChildren = child.generateChildren(cache);
            for (Cube grandChild : grandChildren){
                Cube[] grandgrandChildren = grandChild.generateChildren(cache);
                for (Cube grandgrandChild : grandgrandChildren){
                    grandChild.setBound(bound);
                    childrenQueue.addFirst(grandgrandChild);
                    }
                }
        }

        /* A new thread is used to start solving cubes from the childrenQueue
        * synchronization over the childrenQueue is crucial at this point because
        * at the same time another threads pulls cubes and sends them to Workers. */
        Thread t1 = new Thread(new Runnable()
        {
            public void run()
            {
              synchronized(childrenQueue){
                while(!childrenQueue.isEmpty())
                {

                    Cube cb = childrenQueue.pollLast();
                    int s = solutions(cb,null);
                    totalSolutions.addAndGet(s);
                }
              }
            }
        });
        t1.start();
        /*  Master Thread iterates over the global Queue and sends the data
         *  until this queue to empty.
         */
        synchronized(childrenQueue){
            while(!childrenQueue.isEmpty()){
                sendCube();
            }
        }

        /*  Wait for busy workers.
         */
        while(busyWorkers.get()>0){
            ;
        }

        /*  If total solutions have been found send message to workers to close.
         */
        if(totalSolutions.get()>0){
            for( Worker wk : workerQueue) {
                if( !wk.getId().equals(myIbisId) ) {
                    WriteMessage w = wk.getSendPort().newMessage();
                    w.writeString("close");
                    w.finish();
                    wk.getSendPort().close();
                }
            }

            synchronized(this){
                this.notifyAll();
            }
        }
        /*  Return the totalSolutions
         */
        return totalSolutions.get();
    }

    private void solve(Cube cube) throws IOException, ConnectionFailedException {

        // cache used for cube objects. Doing new cube() for every move
        // overloads the garbage collector

        CubeCache cache = new CubeCache(cube.getSize());
        int bound = 0;
        int result = 0;

        System.out.print("Bound now:");

        /* Solve sequentially with bound < 3
         * In most cases this returns 0 and then
         * we expand two levels of children and run in parallel */

        for (int i=0; i<3; i++){
            cube.setBound(i);
            result =  solutions(cube, null);
            if (result > 0 ){
                break;
            }
        }

        while (result == 0) {
            bound++;
            cube.setBound(bound);

            System.out.print(" " + bound);
            result = solveAtBound(cube, cache);
        }

        System.out.println();
        System.out.println("Solving cube possible in " + result + " ways of "
                + bound + " steps");
    }

    /*  Same as sequential
     */
    private static int solutions(Cube cube, CubeCache cache) {
        if (cube.isSolved()) {
            return 1;
        }

        if (cube.getTwists() >= cube.getBound()) {
            return 0;
        }

        // generate all possible cubes from this one
        // by moving the player in every direction

        if(cache==null) {
            cache = new CubeCache(cube.getSize());
        }

        Cube[] children = cube.generateChildren(cache) ;

        int result = 0;

        for (Cube child : children) {
            // recursion step
            int childSolutions = solutions(child, cache);
            if (childSolutions > 0) {
                result += childSolutions;
            }
            // put child object in cache
            cache.put(child);
        }
        return result;
    }

    public static void printUsage() {
        System.out.println("Rubiks Cube solver");
        System.out.println("");
        System.out
                .println("Does a number of random twists, then solves the rubiks cube with a simple");
        System.out
                .println(" brute-force approach. Can also take a file as input");
        System.out.println("");
        System.out.println("USAGE: Rubiks [OPTIONS]");
        System.out.println("");
        System.out.println("Options:");
        System.out.println("--size SIZE\t\tSize of cube (default: 3)");
        System.out
                .println("--twists TWISTS\t\tNumber of random twists (default: 11)");
        System.out
                .println("--seed SEED\t\tSeed of random generator (default: 0");
        System.out
                .println("--threads THREADS\t\tNumber of threads to use (default: 1, other values not supported by sequential version)");
        System.out.println("");
        System.out
                .println("--file FILE_NAME\t\tLoad cube from given file instead of generating it");
        System.out.println("");
    }

    private void initializeCube(String[] arguments) {

        // default parameters of puzzle
        int size = 3;
        int twists = 11;
        int seed = 0;
        String fileName = null;

        // number of threads used to solve puzzle
        // (not used in sequential version)

        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i].equalsIgnoreCase("--size")) {
                i++;
                size = Integer.parseInt(arguments[i]);
            } else if (arguments[i].equalsIgnoreCase("--twists")) {
                i++;
                twists = Integer.parseInt(arguments[i]);
            } else if (arguments[i].equalsIgnoreCase("--seed")) {
                i++;
                seed = Integer.parseInt(arguments[i]);
            } else if (arguments[i].equalsIgnoreCase("--file")) {
                i++;
                fileName = arguments[i];
            } else if (arguments[i].equalsIgnoreCase("--help") || arguments[i].equalsIgnoreCase("-h")) {
                printUsage();
                System.exit(0);
            } else {
                System.err.println("unknown option : " + arguments[i]);
                printUsage();
                System.exit(1);
            }
        }

        // create cube
        if (fileName == null) {
            firstCube = new Cube(size, twists, seed);
        } else {
            try {
                firstCube = new Cube(fileName);
            } catch (Exception e) {
                System.err.println("Cannot load cube from file: " + e);
                System.exit(1);
            }
        }

        // print cube info
        System.out.println("Searching for solution for cube of size "
                + firstCube.getSize() + ", twists = " + twists + ", seed = " + seed);
        firstCube.print(System.out);
        System.out.flush();        
    }
    

    public void run(String[] args) throws Exception {
        
        initializeCube(args);
        
        childrenQueue = new ArrayDeque<Cube>();
        workerQueue   = new ArrayDeque<Worker>();

        // Create the receive port to handle the messages 
        ReceivePort receiver = Rubiks.ibis.createReceivePort(upCallPort,"server", this);
        
        // Enable connections
        receiver.enableConnections();

        // Enable upcalls
        receiver.enableMessageUpcalls();

        // solve
        long start = System.currentTimeMillis();
        solve(firstCube);
        long end = System.currentTimeMillis();

        // NOTE: this is printed to standard error! The rest of the output is
        // constant for each set of parameters. Printing this to standard error
        // makes the output of standard out comparable with "diff"
        System.err.println("Solving cube took " + (end - start)
                + " milliseconds");
        
        receiver.close();
    }

}