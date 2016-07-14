package bothack.agents;

import bothack.agents.behaviours.AddressAcceptingBehaviour;
import bothack.agents.behaviours.JadeMessageAcceptingBehaviour;
import bothack.agents.behaviours.SocketMessageAcceptingBehaviour;
import bothack.agents.authentication.CookieManager;
import bothack.classes.QueueElement;
import jade.core.Agent;
import jade.core.NotFoundException;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>Created by administrator on 12/22/14.</p>
 * <p>this is the central agent, it's responsible for validating potential users and launching dungeons, that is instances of NethackAgent for players</p>
 */


public class LoginAgent extends Agent {
    /*this is factory for two behaviours responsible for accepting new messages*/
    private ThreadedBehaviourFactory tbf;

    /* jade behaviour responsible for accepting messages through sockets*/
    private SocketMessageAcceptingBehaviour socketListener;

    /* the socket on which initial connections will be accepted */
    private ServerSocket socket;

    /* jade behaviour responsible for accepting messages through jade message protocol */
    private JadeMessageAcceptingBehaviour jadeListener;

    /* jade behaviour waiting for a message from a newly created agent sends to client program details on where to find the correct dungeon */
    private AddressAcceptingBehaviour addressAcceptingBehaviour;

    /* class responsible for creating a security cookie */
    //TODO replace this with token identification
    private CookieManager cookieManager;

    /* list of clients waiting to receive information on their newly created dungeon, instance of NethackAgent */
    private volatile ArrayList<QueueElement> waitingQueue;

    /*simple lock for securing the critical section of the code */
    private ReentrantLock lock;

    /**
     *  <p>initialize all necessary variables </p>
     *  <p>this method is part of the JADE framework agent lifecycle, basically it serves the purpose of a constructor,
     *  however since this class uses elements of the JADE framework its fields cannot be instantiated in a simple constructor
     * </p>
     * */
    @Override
    public void setup(){
        try {

                    /* set the property for login credential setup */
            System.setProperty("java.security.auth.login.config", "loginAgent.conf");

            lock = new ReentrantLock();
            tbf = new ThreadedBehaviourFactory();

            /* get the port on which this agent's ServerSocket  is running from jade arguments */
            Integer portNumber = Integer.parseInt((String) getArguments()[0]);

            /* launch new server socket on the port provided earlier */
            socket = new ServerSocket(portNumber);

            /* create a new cookie manager */
            cookieManager = new CookieManager();

            /* create a new waiting que */
            waitingQueue = new ArrayList<QueueElement>();

            /* register login agent with JADE */
            registerService();

            /* initialize the behaviour listening for contact on sockets */
            socketListener = new SocketMessageAcceptingBehaviour(socket);

            /* be sure to add it to the agent or it won't work as expected*/
            addBehaviour(tbf.wrap(socketListener));

            /* initialize the behaviour listening for contact from JADE agents or programs using jade platform */
            jadeListener = new JadeMessageAcceptingBehaviour();

            /* be sure to add it to the agent or it won't work as expected*/
            addBehaviour(jadeListener);

            /* initialize the behaviour sending responses with NethackAgent contact information to clients */
            addressAcceptingBehaviour = new AddressAcceptingBehaviour();

            /* be sure to add it to the agent or it won't work as expected*/
            addBehaviour(addressAcceptingBehaviour);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void takeDown(){
        /* get the behaviour threads and kill them
            note the sequence, first an interrupt then removal of the behaviour zombies might rise otherwise
         */
        try {
            /* send an interrupt to the behaviour */
            tbf.interrupt(socketListener);

            /* remove the behaviour whose thread was already interrupted */
            removeBehaviour(socketListener);

            /* use JADE method of the behaviour signalling it to stop it's activities */
            jadeListener.done();

            /* remove the behaviour after it was stopped */
            removeBehaviour(jadeListener);

            /* use JADE method of the behaviour signalling it to stop it's activities */
            addressAcceptingBehaviour.done();

            /* remove the behaviour after it was stopped */
            removeBehaviour(addressAcceptingBehaviour);
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }finally {
            try {
                /* try to close the socket of the socketListener */
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
        <p>method for registering the service this agent provides with JADE platform
        which will enable other jade agents to search for it using JADE queries</p>
     @return returns void
    */
    void registerService(){
        /* prepare a new message   */
        DFAgentDescription dfad = new DFAgentDescription();
        /* get the AID of the agent */
        dfad.setName(getAID());

        /* create a new service description and populate it*/
        ServiceDescription sd = new ServiceDescription();
        sd.setType("dungeon_login");
        sd.setName(getLocalName()+"-login");

        /* add the service description to the registering method */
        dfad.addServices(sd);
        try {
            /* try to register this new service with the directory facilitator */
            DFService.register(this, dfad);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    /** gets the cookie manager
     * @return returns the cookie manager
     * */
    public CookieManager getCookieManager() {
        return cookieManager;
    }

    /* sets the cookie manager */
    public void setCookieManager(CookieManager cookieManager) {
        this.cookieManager = cookieManager;
    }

    /**
     *  adds a new element to the waitingQue
     *  the lock is used to avoid mixing up data
     *
     *  @param element the new elemnt that is to be added to the queue
     *  @return returns true if new element was added correctly; false if an exception occurred
     *
     *  */
    public boolean addToQueue(QueueElement element){
        lock.lock();
        try{
            waitingQueue.add(element);

        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        finally {
            lock.unlock();
            return true;
        }
    }
    /** removes an element from the queue, the lock is used to protect the data while it's being accessed
     *
     * @param element the element which to remove
     * @return returns true if new element was removed correctly; false if an exception occurred
     * */
    public boolean removeFromQueue(QueueElement element){
        lock.lock();
        try{
            waitingQueue.remove(element);
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        finally {
            lock.unlock();
            return true;
        }
    }

    /** simply returns the queue; it should be unavailable to any other thread while it's being used
     * @return the arrayList which is the current queue
     * */
    public ArrayList<QueueElement> getWaitingQueue(){
        lock.lock();
        try{
            return  waitingQueue;
        }
        finally {
            lock.unlock();
        }

    }
}
