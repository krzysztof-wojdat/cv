package bothack.agents.behaviours;

import bothack.agents.NethackAgent;
import bothack.agents.messages.InteractMessage;
import bothack.agents.messages.QuitMessage;
import bothack.agents.messages.SetupMessage;

import java.io.StringReader;
import java.lang.*;

import bothack.classes.NethackChoice;
import bothack.classes.NethackDirectionObject;
import bothack.classes.NethackMenuChoice;
import bothack.classes.NethackStringObject;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

/**
 * Created by krito on 11/23/14.
 *
 * <p>This behaviour is used to receive messages sent from a Client application to a NethackAgent through the JADE Platform </p>
 */
public class NethackJadeMessageAcceptingBehaviour extends CyclicBehaviour {

    /* message template used to recognize request messages, only messages of this type are procssed by this behaviour*/
    private final MessageTemplate request = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);

    /* a reference to the behaviour responsible for communication with nethack-lisp process*/
    private final NethackBehaviour nethackBehaviour;

    /* simple constructor */
    public NethackJadeMessageAcceptingBehaviour(NethackBehaviour nb){
        super();
        nethackBehaviour = nb;
    }

    /**
     * an override of JADE behaviour lifecycle method
     */
    @Override
    public void action() {
        /* message receive check */
        ACLMessage msg = myAgent.receive(request);
        if(msg != null)
        {
            try {
                /* System message used for debbuging the prototype, should be saved to a log, maybe even removed entirely*/
                System.out.println("Message received");

                /* a new context used for converting the xml content of the message into an object used by the system*/
                JAXBContext context = JAXBContext.newInstance(
                    QuitMessage.class, InteractMessage.class, SetupMessage.class, NethackMenuChoice.class, NethackChoice.class,bothack.interfaces.Command.class,
                        NethackDirectionObject.class, NethackStringObject.class);
                Unmarshaller unmarshaller = context.createUnmarshaller();

                if(myAgent instanceof NethackAgent){
                    /* timestamp of the message is saved in the agent's field, used for an auto-timeout*/
                    ((NethackAgent) myAgent).setLastMessageAccepted(System.currentTimeMillis());

                    /* anouther status message*/
                    System.out.println("NethackAgent : Request message received");

                    Object o = unmarshaller.unmarshal(new StringReader(msg.getContent()));

                    /* after unmarshalling if any of the messages is recognized then an appropriate response is triggered on the NethackBehaviour
                    * first authorization is performed, if authorization is passed, then a method is called
                    * */
                    //TODO perhaps a separate response should be written to handle a situation when the content is neither of those, apart from the JAXBException, if not at least the exception should be forwarded to the client app
                    if(o instanceof SetupMessage ){
                        if(((NethackAgent) myAgent).cookieChecker(((SetupMessage) o).getCookie(),null,msg.getSender())){
                            nethackBehaviour.setup((SetupMessage)o,msg.getSender(),null);
                        }
                    }
                    else if(o instanceof InteractMessage){
                        if(((NethackAgent) myAgent).cookieChecker(((InteractMessage) o).getCookie(),null,msg.getSender())) {
                            nethackBehaviour.processInteractMessage((InteractMessage) o, msg.getSender(), null);
                        }
                    }
                    else if(o instanceof QuitMessage){
                        if(((NethackAgent) myAgent).cookieChecker(((QuitMessage) o).getCookie(),null,msg.getSender())) {
                            nethackBehaviour.quit((QuitMessage) o, msg.getSender(), null);
                            nethackBehaviour.done();
                            myAgent.removeBehaviour(nethackBehaviour);
                        }
                    }

                }
            } catch (JAXBException e) {
                e.printStackTrace();
            }
        }
        else{
            /* if the message doesn't fit the template the behaviour does nothing */
            block();
        }

    }
}
