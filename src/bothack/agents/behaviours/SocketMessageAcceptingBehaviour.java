package bothack.agents.behaviours;

import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * <p>Created by administrator on 12/22/14.</p>

    <p>this is a JADE Behaviour which will accept new communication through a tcp socket it has to be in separate thread
    so the whole process won't stop and wait until a single request is resolved</p>
 */
public class SocketMessageAcceptingBehaviour extends CyclicBehaviour {

    /* the socket on which the behaviour will listen */
    private final ServerSocket serverSocket;

    /* the factory which will create new behaviours once a connection is made */
    private final ThreadedBehaviourFactory requestProcessingFactory;

    /* constructor */
    public SocketMessageAcceptingBehaviour(ServerSocket socket){
        serverSocket = socket;
        requestProcessingFactory = new ThreadedBehaviourFactory();

    }
    /**
     * <p>this is an ovverride of a JADE framework behaviour lifecycle method</p>
     *
     * <p>the behaviour 'waits' for a new connection when it occurs a new socket nad a new behaviour to service it are created
     * at least i hope it works like that, and new behaviours are not created every time 'action' is called
     * </p>
     * */
    //TODO check how many SocketRequestProcessingBehaviours are being created, just in case
    @Override
    public void action() {
        try {
            while(true){
                myAgent.addBehaviour(requestProcessingFactory.wrap(new SocketRequestProcessingBehaviour(serverSocket.accept())));

                //this printout is only for on-the-fly testing, it should be moved to a log for future reference; change the comment when it happens
                System.out.println(myAgent.getLocalName() + " : SocketRequestProcessingBehaviour added ");
            }

        }catch (IOException e) {

            //this printout is only for on-the-fly testing, should be moved to a log in case anything bad happens; change the comment when it happens
            e.printStackTrace();
        }

    }
}
