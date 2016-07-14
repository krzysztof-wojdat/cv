package bothack.agents.authentication;

import java.util.Random;

/**
 * Created by administrator on 12/22/14.
 */
public class CookieManager {
    /*
    Creates a random number out of the given seed.
    Is used for authentication fo messages between the LoginAgent and the botAgent
     */
    private final Random r;

    public CookieManager(){
        r = new Random(433494437);
    }

    public String getCookie(){
        return new Integer(r.nextInt()).toString();
    }
}
