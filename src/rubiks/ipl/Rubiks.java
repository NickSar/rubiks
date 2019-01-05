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
 * Parallel Implementation for rubik's cube puzzle solver
 * 
 * @author Nikos Sarris
 * 
 */

 

public class Rubiks implements MessageUpcall {


    IbisCapabilities ibisCapabilities = new IbisCapabilities(
            IbisCapabilities.ELECTIONS_STRICT);

    /**
     * Function called by Ibis to give us a newly arrived message
     * 
     * @param message
     *            the message
     * @throws IOException
     *             when the message cannot be read
     */

    private void client(Ibis myIbis, IbisIdentifier server) throws IOException {
                // Create a send port for sending requests and connect.
        SendPort sender = myIbis.createSendPort(portType);
        sender.connect(server, "server");

        // Send the message.
        WriteMessage w = sender.newMessage();
        w.writeString("Hi there");
        w.finish();

        // Close ports.
        sender.close();
    }

    private void run(String[] arguments) throws Exception {
        
        // Create an ibis instance.
        Ibis ibis = IbisFactory.createIbis(ibisCapabilities, null, portType);
        
        // Elect the server.
        IbisIdentifier server = ibis.registry().elect("server");

        // Run depending on my role
        if (server.equals(ibis.identifier())) {
            // server(ibis, arguments);
            new Server(arguments, ibis);
        } else {
            client(ibis, server);
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