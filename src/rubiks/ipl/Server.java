package rubiks.ipl;

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.MessageUpcall;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;
import java.io.IOException;
/**
 * Server class for the parallel implementation
 * 
 * @author Nikos Sarris
 */

public class Server implements MessageUpcall{

    private Cube cube = null;

    PortType portType = new PortType(PortType.COMMUNICATION_RELIABLE,
            PortType.SERIALIZATION_DATA, PortType.RECEIVE_AUTO_UPCALLS,
            PortType.CONNECTION_MANY_TO_ONE);

    public Server(String[] args, Ibis ibis){
        initializeCube(args);
        try {
            run(ibis);
        } catch(Exception e) {
            e.printStackTrace(System.err);
        }
        
    }

    public void upcall(ReadMessage message) throws IOException {
        String s = message.readString();
        System.out.println(message.origin() + " says: " + s);
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
            cube = new Cube(size, twists, seed);
        } else {
            try {
                cube = new Cube(fileName);
            } catch (Exception e) {
                System.err.println("Cannot load cube from file: " + e);
                System.exit(1);
            }
        }

        // print cube info
        System.out.println("Searching for solution for cube of size "
                + cube.getSize() + ", twists = " + twists + ", seed = " + seed);
        cube.print(System.out);
        System.out.flush();        
    }

    private void run(Ibis myIbis) throws Exception {
        
        // Create a receive port, pass ourselves as the message upcall
        // handler
        ReceivePort receiver = myIbis.createReceivePort(portType, "server",
                this);
        // enable connections
        receiver.enableConnections();
        // enable upcalls
        receiver.enableMessageUpcalls();
        
        // do nothing for a minute (will get upcalls for messages
        Thread.sleep(60000);

        // Close receive port.
        receiver.close();
    }

}