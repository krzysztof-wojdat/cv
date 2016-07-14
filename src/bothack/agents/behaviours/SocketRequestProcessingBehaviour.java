package bothack.agents.behaviours;

import bothack.agents.LoginAgent;
import bothack.agents.authentication.NethackCallbackHandler;
import bothack.agents.authentication.TestCallbackHandler;
import bothack.agents.messages.ErrorMessage;
import bothack.agents.messages.LoginMessage;
import bothack.classes.*;
import bothack.classes.Error;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.StaleProxyException;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.net.Socket;

/**
 * Created by administrator on 12/22/14.
<p>
      this is a behavior which processes a socket message received by LoginAgent, it starts a new instance of nethack dungeon,
        that is an agent running instance of Nethack class. It does not process any other messages since any other communication through sockets
        should be between user client and the Nethack dungeon.
 </p>
 */
public class SocketRequestProcessingBehaviour extends OneShotBehaviour {

    /* this is the socket which will be used for the connection between the client and the LoginAgent; the NethackAgent will have a separate socket */
    private final Socket socket;

    /* writers used for transmitting the return message */
    private BufferedWriter out;
    private StringWriter writer;

    /* message template not used*/
    //TODO REMOVE THIS WHEN COMMENTING IS FINISHED; REMOVE ALL UNUSED VARIABLES
    MessageTemplate mt;


    public SocketRequestProcessingBehaviour(Socket socket){
        super();
        this.socket = socket;
    }


    //TODO the queue element will stay int the queue forever if there occurs an error while creating an agent on error remove the element from queue
    //TODO check if there is any other way to validate if there are no "stale" elements in the queue, some sort of timeout perhaps
    /**
     * this method is responsible for starting a new NethackAgent on the JADE platform;
     * as arguments it receives the new dungeon name a cookie to authenticate the player,
     * the new prospective dungeonName is placed in the queue which the login agent will be monitoring, when LoginAgent receives the data of the new socket of the Nethack Agent
     * he will remove the QueueElement form the Queue and pass the data to the client
     *
     *
     * @param name the name of the client requesting a new Dungeon
     * @param requestSocket the socket to which the client is connected, on which he is waiting for a response
     * @return either null if everything is ok or an error object with a code an short description
     */
    public bothack.classes.Error startNewDungeon(String name, Socket requestSocket){

        String dungeon = "dungeon_"+name;
        String cookie = "";
        Integer portNumber =0;
        AID dungeonAddress = new AID(dungeon,AID.ISLOCALNAME);
        cookie = ((LoginAgent)myAgent).getCookieManager().getCookie();
        Object[] args = {cookie,dungeon,myAgent.getLocalName()};
        QueueElement queueElement = new QueueElement(dungeon, null,requestSocket);
        ((LoginAgent) myAgent).addToQueue(queueElement);
        //start the dungeon
        try {
            myAgent.getContainerController().createNewAgent(dungeon,"bothack.agents.NethackAgent",args).start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
            return new Error(145,"Failed to create new agent");
        }
        return null;
    }

    /**
     * <p>this is a method of JADE behaviour lifecycle </p>
     * <p>this method reads in the message put into the stream by the client application, authorizes the client, processes the message
     * it and in case of an error returns it to the user application</p>
     * <p>only LoginMessage are processed by this method</p>
     *
     * @return returns void
     */
    @Override
    public void action() {
        System.out.println("requestReceived");
        try {
            /**
             *  here we are getting the message from the stream
             */
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            writer = new StringWriter();
            String input;
            JAXBContext jaxbContext = JAXBContext.newInstance(LoginMessage.class,bothack.classes.Error.class,Address.class, ErrorMessage.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            Marshaller m = jaxbContext.createMarshaller();
            input = in.readLine();
            StringReader reader = new StringReader(input);
            Object o = unmarshaller.unmarshal(reader);
            /**
             * a check whether this message is a Login message
             */
            if( o instanceof LoginMessage){
                String name = ((LoginMessage) o).getName();
                String password = ((LoginMessage) o).getPassword();
                LoginContext lc = new LoginContext("Test", new NethackCallbackHandler(name,password));
                //the line below is for testing purposes only
                //LoginContext lc = new LoginContext("Test", new TestCallbackHandler());
                lc.login();
                /*m.marshal(startNewDungeon(name), out);*/
                /**
                 *  here a new dungeon is created, only after the user has successfully logged in; any errors are immediately returned to the client app
                 */
                Error err = startNewDungeon(name, socket);
                if(null != err){
                    m.marshal(err, writer);
                    out.write(writer.toString());
                    out.newLine();
                    out.flush();
                    out.close();
                    socket.close();
                    return;
                }
            }
            /**
             * if not an error is immediately returned to the client, with the appropriate code
             */
            else{
                bothack.classes.Error err = new bothack.classes.Error(126,"Message not recognized");
                m.marshal(err, writer);
                out.write(writer.toString());
                out.newLine();
                out.flush();
                out.close();
                socket.close();
                System.err.println(myAgent.getName() + " : Message not recognized");
            }
        } catch (LoginException e) {
            /**
             * login exceptions are returned to the client here,
             * as well as any jaxb exceptions that may occur while writing any of the messages to the output stream
             */
        //TODO are all JAXBExceptions needed? check
            e.printStackTrace();
            if(e instanceof FailedLoginException){
                bothack.classes.Error err = new bothack.classes.Error(127,"Failed login exception");
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setError(err);
                try {
                    JAXBContext jaxbContext = JAXBContext.newInstance(bothack.classes.Error.class,ErrorMessage.class);
                    Marshaller m = jaxbContext.createMarshaller();
                    m.marshal(errorMessage, writer);
                    out.write(writer.toString());
                    out.newLine();
                    out.flush();
                    out.close();
                    socket.close();
                } catch (JAXBException e1) {
                    e1.printStackTrace();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

}
