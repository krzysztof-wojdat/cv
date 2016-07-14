package bothack.agents.behaviours;

import bothack.agents.NethackAgent;
import bothack.agents.messages.*;
import bothack.classes.Nethack;
import bothack.classes.NethackMap;
import bothack.classes.NethackMessageFactory;
import bothack.classes.PlayerCharacter;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;

import java.net.Socket;

/**
 * Created by administrator on 12/4/14.
 * <p>This is the behaviour which runs the Nethack instance.</p>
 */
public class NethackBehaviour extends SimpleBehaviour {
    /* the player which owns this Agent*/
    private String owner;

    /* an instance of the Nethack game wrapped in all manner of methods, to enable communication with it*/
    private Nethack dungeon;

    //TODO check what this actually does, it gives output to the player, but i am not sure
    private final NethackMessageFactory factory;

    /* switch for setting the verbose mode*/
    private boolean verbose;

    /* tells, the behaviour and the Agent should they start terminating themselves*/
    private boolean end = false;



    public NethackBehaviour(String owner){
        dungeon = new Nethack(owner);
        this.owner = owner;
        verbose = true;
        factory = new NethackMessageFactory(dungeon,this);

    }
    @Override
    public void action() {

    }


    public void setup(SetupMessage msg,AID recipient, Socket requestSocket){
        if(msg.getRandom()){
            dungeon.setup();
            myAgent.addBehaviour(new visualizationUpdateBehaviour(owner,dungeon.getAvatar(),dungeon.getTheMap()));
            myAgent.addBehaviour(new ObjectSendingBehaviour(requestSocket,recipient,factory.getMessage()));

        }
        else{
            dungeon.setup(msg.getRole(),msg.getRace(),msg.getGender(),msg.getAlignment());
            myAgent.addBehaviour(new visualizationUpdateBehaviour(owner,dungeon.getAvatar(),dungeon.getTheMap()));
            myAgent.addBehaviour(new ObjectSendingBehaviour(requestSocket,recipient,factory.getMessage()));
        }
    }

    public void processInteractMessage(InteractMessage message,AID recipient, Socket socket){
        if(message.isMapRequest()){
            NethackMap map = dungeon.getTheMap();
            myAgent.addBehaviour(new ObjectSendingBehaviour(socket,recipient,map));
        }
        else if(message.isAvatarRequest())
        {
            PlayerCharacter avatar = dungeon.getAvatar();
            myAgent.addBehaviour(new ObjectSendingBehaviour(socket,recipient,avatar));
        }
        else if(message.getAction() != null){
            if(dungeon.action(message.getAction())){
                myAgent.addBehaviour(new ObjectSendingBehaviour(socket,recipient,factory.getMessage()));
                if (dungeon.isUpdate()) {
                    myAgent.addBehaviour(new visualizationUpdateBehaviour(owner, dungeon.getAvatar(), dungeon.getTheMap()));
                    dungeon.setUpdate();
                }
            }
            else{
                myAgent.addBehaviour(new ObjectSendingBehaviour(socket,recipient,factory.getErrorMessage()));
            }
        }
        else if(message.getChoice() != null){
            if(dungeon.action(message.getChoice())){
                myAgent.addBehaviour(new ObjectSendingBehaviour(socket,recipient,factory.getMessage()));
                if (dungeon.isUpdate()) {
                    myAgent.addBehaviour(new visualizationUpdateBehaviour(owner, dungeon.getAvatar(), dungeon.getTheMap()));
                    dungeon.setUpdate();
                }
            }
            else{
                myAgent.addBehaviour(new ObjectSendingBehaviour(socket,recipient,factory.getErrorMessage()));
            }
        }
        else if(message.getDirection() != null){
            if(dungeon.action(message.getDirection())){
                myAgent.addBehaviour(new ObjectSendingBehaviour(socket,recipient,factory.getMessage()));
                if (dungeon.isUpdate()) {
                    myAgent.addBehaviour(new visualizationUpdateBehaviour(owner, dungeon.getAvatar(), dungeon.getTheMap()));
                    dungeon.setUpdate();
                }
            }
            else{
                myAgent.addBehaviour(new ObjectSendingBehaviour(socket,recipient,factory.getErrorMessage()));
            }
        }
        else if(message.getMenuOption() != null){
            if(dungeon.action(message.getMenuOption())){
                myAgent.addBehaviour(new ObjectSendingBehaviour(socket,recipient,factory.getMessage()));
                if (dungeon.isUpdate()) {
                    myAgent.addBehaviour(new visualizationUpdateBehaviour(owner, dungeon.getAvatar(), dungeon.getTheMap()));
                    dungeon.setUpdate();
                }
            }
            else{
                myAgent.addBehaviour(new ObjectSendingBehaviour(socket,recipient,factory.getErrorMessage()));
            }
        }
        else if(message.getText() != null){
            if(dungeon.action(message.getText())){
                myAgent.addBehaviour(new ObjectSendingBehaviour(socket,recipient,factory.getMessage()));
                if (dungeon.isUpdate()) {
                    myAgent.addBehaviour(new visualizationUpdateBehaviour(owner, dungeon.getAvatar(), dungeon.getTheMap()));
                    dungeon.setUpdate();
                }
            }
            else{
                myAgent.addBehaviour(new ObjectSendingBehaviour(socket,recipient,factory.getErrorMessage()));
            }
        }
    }

    public void quit(QuitMessage quitMessage,AID sender,Socket socket)
    {
        try {
            dungeon.quitFast();
            if(myAgent instanceof NethackAgent){
                for(AID address : ((NethackAgent) myAgent).getVisualizers()){
                    myAgent.addBehaviour(new ObjectSendingBehaviour(null,address,new QuitMessage(myAgent)));
                }
                myAgent.doDelete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean done() {
        return true;
    }
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Nethack getDungeon() {
        return dungeon;
    }

    public void setDungeon(Nethack dungeon) {
        this.dungeon = dungeon;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}
