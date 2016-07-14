package bothack.agents.behaviours;

import bothack.agents.LoginAgent;
import bothack.agents.messages.ErrorMessage;
import bothack.agents.messages.LoginMessage;
import bothack.classes.Address;
import bothack.classes.QueueElement;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;

/**
 * <p>Created by administrator on 12/28/14.</p>
 * <p>this is a behaviour of the LoginAgent, it extends the functionality of a JADE Cyclic Behaviour.
 * It's purpose is to wait for a message from a newly created NethackAgent with details on how the client application is to communicate with it.
 * Most notably, the port number of the Socket instantiated by the NethackAgent if he's in SocketMode</p>
 */
public class AddressAcceptingBehaviour extends CyclicBehaviour {
    /**
     * the template of containing th Performative of the message which will contain a response form the nethack agent,
     * it is used for filtering unwanted messages
     */
    private static final MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
    /**
     * for decoding the content of the message
     */
    private Unmarshaller unmarshaller;
    /**
     * a simple constructor
     */
    public AddressAcceptingBehaviour(){
        super();
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(LoginMessage.class, Address.class, bothack.classes.Error.class, ErrorMessage.class);
            unmarshaller = jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    /**
     * <p>an override of a JADE Framework behaviour lifecycle method</p>
     * <p>if a message with a correct performative is accepted by the LoginAgent, and it contains the correct object i.e:
     * instance of Adress class then, an object with a matching sender is taken form the waiting queue and a response is sent to  him
     * using the preferred method of communication</p>
     */
    //TODO WARNING a situation in which no match is found in the queue is not handled in this implementation needs to be fixed
    @Override
    public void action() {
        ACLMessage msg = myAgent.receive(mt);
        QueueElement elementToRemove = null;
        if(msg != null){
            try {
                StringReader reader = new StringReader(msg.getContent());
                Object o = unmarshaller.unmarshal(reader);
                if(o instanceof Address){
                    ArrayList<QueueElement> queueElements = ((LoginAgent)myAgent).getWaitingQueue();
                    for(QueueElement element : queueElements){
                        if(element.getOperation().equals("address") && msg.getSender().getLocalName().equals(element.getDungeon())){

                            if(element.getRecipientAgent()!=null){
                                ACLMessage reply = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                                reply.addReceiver(element.getRecipientAgent());
                                reply.setContent(msg.getContent());
                                myAgent.send(reply);
                                elementToRemove = element;
                            }
                            else if(element.getRecipientSocket() != null){
                                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(element.getRecipientSocket().getOutputStream()));
                                writer.write(msg.getContent());
                                writer.newLine();
                                writer.close();
                                element.getRecipientSocket().close();
                                elementToRemove = element;
                            }
                        }
                    }
                    ((LoginAgent)myAgent).removeFromQueue(elementToRemove);
                }
            } catch (JAXBException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }catch(NullPointerException e){
                e.printStackTrace();
            }

        }
        else{
            block();
        }
    }
}
