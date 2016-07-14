package bothack.agents.behaviours;

import bothack.agents.LoginAgent;
import bothack.agents.authentication.NethackCallbackHandler;
import bothack.agents.messages.ErrorMessage;
import bothack.agents.messages.LoginMessage;
import bothack.classes.*;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.StaleProxyException;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * <p>Created by administrator on 12/22/14.</p>
 * <p>this behaviour processes a message which was accepted by the LoginAgent through JADE platform, it validates whether the message was of the correct type,
 * whether the correct credentials were passed and if the user was authenticated it launches a new instance of the dungeon</p>
 */
public class JadeRequestProcessingBehaviour extends OneShotBehaviour{
    private final ACLMessage request;

    /**
     * a simple constructor
     * @param request the ACLMessage accepted by the LoginAgent through JadeMessageProcessingBehaviour
     */
    public JadeRequestProcessingBehaviour(ACLMessage request){
        super();
        this.request = request;
    }

    /**
     * this method is responsible for starting a new NethackAgent on the JADE platform;
     * as arguments it receives the new dungeon name a cookie to authenticate the player,
     * the new prospective dungeonName is placed in the queue which the login agent will be monitoring, when LoginAgent receives the data of the new socket of the Nethack Agent
     * he will remove the QueueElement form the Queue and pass the data to the client
     *
     * @param name the name of the sender of the message, will be used in the new dungeons name
     * @param requestJade the AID of the requesting agent
     * @return returns null if everything went correctly or an Error object detailing what went wrong
     */
    bothack.classes.Error startNewDungeon(String name, AID requestJade){
        String dungeon = "dungeon_"+name;
        String cookie = "";
        Integer portNumber =0;
        AID dungeonAddress = new AID(dungeon,AID.ISLOCALNAME);
        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchSender(dungeonAddress), MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL));
        cookie = ((LoginAgent)myAgent).getCookieManager().getCookie();
        Object[] args = {cookie,dungeon,myAgent.getLocalName()};
        QueueElement queueElement = new QueueElement(dungeon,requestJade, null);
        ((LoginAgent) myAgent).addToQueue(queueElement);
        //start the dungeon
        try {
            myAgent.getContainerController().createNewAgent(dungeon,"bothack.agents.NethackAgent",args).start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
            return new bothack.classes.Error(145,"Failed to create new agent");
        }
        /*bothack.classes.Error error = new bothack.classes.Error(128,"Unable to connect with the dungeon");
        //wait for the port number
        ACLMessage msg = myAgent.receive(mt);
        if(msg != null){
            try {
                Object o = msg.getContentObject();
                if(o instanceof Address){
                    return (Address)o;
                }
                else if(o instanceof Error){
                    error = (bothack.classes.Error)o;
                }
            } catch (UnreadableException e) {
                e.printStackTrace();
            }
        }
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setError(error);
        return errorMessage;*/
        return null;
    }

    /**
     * <p>this is an override of the JADE framework Behaviour lifecycle method</p>
     * <p>this method extracts the data form the message content, only login messages are accepted,
     * if the user is authenticated a new instance of nethack Dungeon is started through the startNewDungeon</p>
     */
    //TODO consider creating a new dungeon through a factory
    @Override
    public void action() {
        try {
            /**
             * the contect of the message is extracted form the message
             * */
            JAXBContext jaxbContext = JAXBContext.newInstance(LoginMessage.class, Address.class, bothack.classes.Error.class, ErrorMessage.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            Marshaller marshaller = jaxbContext.createMarshaller();
            StringWriter writer = new StringWriter();
            StringReader reader = new StringReader(request.getContent());
            Object o = unmarshaller.unmarshal(reader);
            /**
             * the object is checked whether it's an LoginMessage
             */
            if(o instanceof LoginMessage){
                //if so the user is authenticated
                System.out.println(myAgent.getName() + " : processing a login request form : " + request.getSender().getName());
                String name = ((LoginMessage) o).getName();
                String password = ((LoginMessage) o).getPassword();
                LoginContext lc = new LoginContext("Test", new NethackCallbackHandler(name,password));
                lc.login();
                //if authentication ends successfully, a new dungeon is instantiated
                bothack.classes.Error err = startNewDungeon(name,request.getSender());
                if(err != null ){
                    /**
                    any errors are reported back to the client
                     */
                    marshaller.marshal(err, writer);
                    ACLMessage reply = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                    reply.addReceiver(request.getSender());
                    reply.setContent(writer.toString());
                    myAgent.send(reply);
                    return;
                }
              /*  ACLMessage reply = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                reply.addReceiver(request.getSender());
                reply.setContent(writer.toString());
                myAgent.send(reply);*/
                System.out.println(myAgent.getName() + " : finished processing a login request form : " + request.getSender().getName());
            }
            else{
                /**
                 * as stated before, only LoginMessages are processed by this behaviour
                 */
                bothack.classes.Error err = new bothack.classes.Error(126,"Message not recognized");
                marshaller.marshal(err, writer);
                ACLMessage reply = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                reply.addReceiver(request.getSender());
                reply.setContent(writer.toString());
                myAgent.send(reply);
            }
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (LoginException e) {
            e.printStackTrace();
            if(e instanceof FailedLoginException){
                /**
                 * here, a failure to login is reported back to the client
                 */
                //TODO consider moving the error reporting to a separate service, i.e get an client identifier, a method of communication and then hace a static method which would accept the deired error message ???
                bothack.classes.Error err = new bothack.classes.Error(127,"Failed login exception");
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setError(err);
                try {
                    JAXBContext jaxbContext = JAXBContext.newInstance(bothack.classes.Error.class,ErrorMessage.class);
                    Marshaller m = jaxbContext.createMarshaller();
                    StringWriter writer = new StringWriter();
                    m.marshal(errorMessage,writer);
                    ACLMessage reply = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                    reply.addReceiver(request.getSender());
                    reply.setContent(writer.toString());
                    myAgent.send(reply);

                } catch (JAXBException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
}
