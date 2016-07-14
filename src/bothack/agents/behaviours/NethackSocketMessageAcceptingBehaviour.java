package bothack.agents.behaviours;

import bothack.agents.NethackAgent;
import bothack.agents.messages.InteractMessage;
import bothack.agents.messages.QuitMessage;
import bothack.agents.messages.SetupMessage;
import bothack.classes.NethackChoice;
import bothack.classes.NethackDirectionObject;
import bothack.classes.NethackMenuChoice;
import bothack.classes.NethackStringObject;
import jade.core.behaviours.CyclicBehaviour;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by administrator on 12/24/14.
 *
 * <p>this is the behaviour which serves as a connection point for a client communicating with the platform through a tcp/ip socket</p>
 */
public class NethackSocketMessageAcceptingBehaviour extends CyclicBehaviour {

    /* the reference to the socket through which communication is accepted*/
    private final ServerSocket mainSocket;

    /* the behaviour which encapsulates Nethack game and returns a response to a specific control message*/
    private final NethackBehaviour nethackBehaviour;

    /**
     * a simple constructor
     *
     * @param socket the socket of the Agent created during agent setup, through this socket new instructions from the client will arive
     * @param nb the Behaviour encapsulating the game (Nethack); will return responses from the game
     */
    public NethackSocketMessageAcceptingBehaviour(ServerSocket socket,NethackBehaviour nb){
        super();
        mainSocket = socket;
        nethackBehaviour = nb;
    }

    /**
     * an override of the JADE Platform behaviour lifecycle methodh
     */
    @Override
    public void action() {
        try {

            /* a new socket is created once a connection is made; due to the way in which ServerSockets accept a new connection i.e putting on
            * hold the whole Thread, this behaviour needs to have a separate thread of execution from the parent Agent*/
            Socket requestSocket = mainSocket.accept();

            /* simple sequence of methods used to get the instructions, in xml form from the data stream*/
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestSocket.getInputStream()));
            String input = reader.readLine();
            StringReader stringReader = new StringReader(input);
            JAXBContext context = JAXBContext.newInstance(
                    QuitMessage.class, InteractMessage.class, SetupMessage.class, NethackMenuChoice.class,
                    NethackChoice.class,bothack.interfaces.Command.class,
                    NethackDirectionObject.class, NethackStringObject.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();

            /* creation of an object though JAXB */
            Object message = unmarshaller.unmarshal(stringReader);
            if(myAgent instanceof NethackAgent){

                /* the time when last message was accepted is updated*/
                //TODO perhapse should be moved to a point where the message is actually processed
                ((NethackAgent) myAgent).setLastMessageAccepted(System.currentTimeMillis());

                /* debug info, should be conditional and replicated in a Log file*/
                System.out.println("NethackAgent : Request message received");

                /* nethack behaviour processes the message according to the type of the message received
                * cookieChecker authenticates the client if authentication passes, then the appropriate method is called*/
                //TODO there is no handling of message unrecognized JAXB exception is handled however not passed to the client
                //TODO authentication errors are not passed to the client
                if(message instanceof SetupMessage ){
                    if(((NethackAgent) myAgent).cookieChecker(((SetupMessage) message).getCookie(),requestSocket,null))
                        nethackBehaviour.setup((SetupMessage) message,null,requestSocket);
                    else{
                        reader.close();
                        requestSocket.close();
                    }
                }
                else if(message instanceof InteractMessage){
                    if(((NethackAgent) myAgent).cookieChecker(((InteractMessage) message).getCookie(),requestSocket,null)){
                        nethackBehaviour.processInteractMessage((InteractMessage)message,null,requestSocket);
                    }
                    else{
                        reader.close();
                        requestSocket.close();
                    }
                }
                else if(message instanceof QuitMessage){
                    if(((NethackAgent) myAgent).cookieChecker(((QuitMessage) message).getCookie(),requestSocket,null)){
                        /* after accepting a quit message, and correctly processing it NethackSocketMessageAcceptingBehaviour calls
                        * NethackBehaviours done() method and subsequently removes it from the agent
                        * other closing operations should follow if they aren't already initiated in the done method*/
                        nethackBehaviour.quit((QuitMessage)message,null,requestSocket);
                        nethackBehaviour.done();
                        myAgent.removeBehaviour(nethackBehaviour);
                        reader.close();
                        requestSocket.close();
                    }
                    else{
                        reader.close();
                        requestSocket.close();
                    }

                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JAXBException e) {
            e.printStackTrace();
        }

    }
}
