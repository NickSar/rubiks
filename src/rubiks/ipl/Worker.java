
package rubiks.ipl;

import java.io.*;
import ibis.ipl.*;
import java.io.IOException;

/* Worker Class that wraps:
 * 1. the  IbisIdentifier of every Worker
 * 2. The status "busy"/"ready"
 * 3. The SendPort
 * 4. and the number of cubesProecessed
*/
public class Worker {

    private final IbisIdentifier id;
    private String status;
    private final SendPort sport;

    Worker(IbisIdentifier id, String status, SendPort sp) {
        this.id = id;
        this.status = status;
        this.sport = sp;
    }

    public IbisIdentifier getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public SendPort getSendPort() {
        return sport;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
