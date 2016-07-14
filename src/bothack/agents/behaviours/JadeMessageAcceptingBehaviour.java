package bothack.agents.behaviours;

import bothack.agents.LoginAgent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * <p>Created by administrator on 12/22/14.</p>
 * <p> this is a behaciout which accepts login requests form clients using JADE framework</p>
 */
public class JadeMessageAcceptingBehaviour extends CyclicBehaviour {
    //this behaviour accepts only REQUEST messages , this template is used to verify whether the message is correct
    private static final MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);

    /**
     * an override of JADE behaviour lifecycle method, accepts the message and instantiates the behaviour which processes it
     *
     *  @return returns void
     */
    @Override
    public void action() {
        ACLMessage msg = myAgent.receive(mt);
        if(msg !=null){
            if(myAgent instanceof LoginAgent) {
                System.out.println(myAgent.getName() + " : Received a request");
                JadeRequestProcessingBehaviour jadeRequestProcessingBehaviour = new JadeRequestProcessingBehaviour(msg);
                myAgent.addBehaviour(jadeRequestProcessingBehaviour);
            }
            else{
                System.out.println(myAgent.getName()+" has been given wrong behaviour to execute");
            }

        }
        else {
            block();
        }

    }
}
