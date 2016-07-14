package bothack.agents;

import bothack.agents.behaviours.*;
import bothack.agents.messages.ErrorMessage;
import bothack.classes.Address;
import jade.core.AID;
import jade.core.Agent;
import jade.core.NotFoundException;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * <p>this is the main workhorse of the system</p>
 * <p>this class extends JADE Framework agent functionality. It's main purpose is to run the NethackBehaviour which runs an instance of the nethack game for the player</p>
 */

public class NethackAgent extends Agent {
    /* the socket through which client application will communicate with Nethack*/
    private ServerSocket socket;

    /* the behaviour which runs an instance of the Nethack class and actually lays the game */
    private NethackBehaviour nb;

    /* this is the behaviour which will accept control message for this agent through JADE framework */
    private NethackJadeMessageAcceptingBehaviour jadeMessages;

    /* this is the behaviour which will accept messages from client application through a socket */
    private NethackSocketMessageAcceptingBehaviour socketMessages;

    /* a factory which will enable placing the 'socket' behaviour in a separate thread */
    private ThreadedBehaviourFactory tbf;

    /* the AID of the login agent will be used to send back necessary contact information to the client application*/
    private AID loginAgent;

    /* list of possible visualisation agents, the results of player actions will be transmitted to them*/
    private ArrayList<AID> visualizers;

    /* the security cookie given by the LoginAgent, used to authenticate the client app*/
    private String cookie;

    /* the arguments with which this agent was created*/
    private Object[] args;

    /* timestam, when was the last message accepted*/
    private long lastMessageAccepted;

    /* difference between when the last message was accepted and current timestamp?*/
    private long delta;

    /**
     * an override of the JADE Framework Agent lifecycle method
     * since parts of this class need elements of the JADE Framework, the constructor cannot be used, since it would provide unexpected results
     */
    @Override
    public void setup(){
        tbf = new ThreadedBehaviourFactory();
        delta = 10;
        args = getArguments();
        cookie = (String)args[0];
        loginAgent = new AID((String)args[2],AID.ISLOCALNAME);
        visualizers = new ArrayList<AID>();
        //start the socket
        try {
            socket = new ServerSocket(0);
        //find visualizers
            findVisualizers();
        //add behaviours
            nb = new NethackBehaviour((String)args[1]);
            addBehaviour(nb);
            socketMessages = new NethackSocketMessageAcceptingBehaviour(socket,nb);
            addBehaviour(tbf.wrap(socketMessages));
            jadeMessages = new NethackJadeMessageAcceptingBehaviour(nb);
        addBehaviour(jadeMessages);
        //send status response to the login agent
            Address address = new Address();
            address.setPort(socket.getLocalPort());
            address.setCookie(cookie);
            address.setDungeon((String)args[1]);
            addBehaviour(new ObjectSendingBehaviour(loginAgent,address,ACLMessage.ACCEPT_PROPOSAL));
            addBehaviour(new TimerBehaviour(delta));
            lastMessageAccepted = System.currentTimeMillis();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void takeDown(){
        try {
            //finish all cyclic behaviours
            tbf.interrupt(socketMessages);
            jadeMessages.done();
            removeBehaviour(jadeMessages);
            //close the socket
            socket.close();
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }



    }

    /**
     * searches the platform fo the LoginAgent which is the main node of the entire system
     * @return returns AID of the login agent
     */
    public AID findNethack(){
        DFAgentDescription[] result = null;
        DFAgentDescription dfAgentDescription = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        sd.setType("dungeon_login");
        dfAgentDescription.addServices(sd);
        try{
            result = DFService.search(this,dfAgentDescription);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        if(result.length != 1){
            return null;
        }
        else {
            return result[0].getName();
        }
    }

    /**
     * method which finds the register visualising clients connected to the platform
     *
     * @return true if any visualising services were found false otherwise
     */
    private boolean findVisualizers(){
        DFAgentDescription[] result = null;
        DFAgentDescription dfAgentDescription = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        sd.setType("dupa");
        dfAgentDescription.addServices(sd);
        SearchConstraints searchConstraints = new SearchConstraints();
        searchConstraints.setMaxResults(new Long(-1));
        try{
            result = DFService.search(this, dfAgentDescription,searchConstraints);
            if(result.length>0){
                for(int i =0; i<result.length;i++){
                    visualizers.add(result[i].getName());
                }
            }
            else {
                return false;
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * method for authenticating the user through a cookie, set during Agent setup through it's starting arguments
     *
     * @param cookie the cookie provided by the user client
     * @param socket the socket through which the message was sent and to which erro message should be sent
     * @param receiver agent AID an error response should be sent
     * @return true if the cookies match false otherwise
     */
    public boolean cookieChecker(String cookie, Socket socket, AID receiver){
        if(cookie.equals(this.cookie)){
            return true;
        }
        else{
            bothack.classes.Error error = new bothack.classes.Error(129,"NethackAgent received an incorrect cookie");
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setError(error);
            addBehaviour(new ObjectSendingBehaviour(socket,receiver,errorMessage));
            return false;
        }
    }


    /* a lot of getters and setters for all of fields in this class*/

    public ServerSocket getSocket() {
        return socket;
    }

    public void setSocket(ServerSocket socket) {
        this.socket = socket;
    }

    public NethackBehaviour getNb() {
        return nb;
    }

    public void setNb(NethackBehaviour nb) {
        this.nb = nb;
    }

    public AID getLoginAgent() {
        return loginAgent;
    }

    public void setLoginAgent(AID loginAgent) {
        this.loginAgent = loginAgent;
    }

    public ArrayList<AID> getVisualizers() {
        return visualizers;
    }

    public void setVisualizers(ArrayList<AID> visualizers) {
        this.visualizers = visualizers;
    }

    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public long getLastMessageAccepted() {
        return lastMessageAccepted;
    }

    public void setLastMessageAccepted(long lastMessageAccepted) {
        this.lastMessageAccepted = lastMessageAccepted;
    }

    public long getDelta() {
        return delta;
    }

    public void setDelta(long delta) {
        this.delta = delta;
    }
}