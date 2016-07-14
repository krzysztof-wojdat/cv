package bothack.agents.authentication;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.util.Map;

/**
 * Created by krito on 20.01.16.
 */
public class TestLoginModule implements LoginModule {
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        System.out.println("TestLoginModule: initialized");
    }

    @Override
    public boolean login() throws LoginException {
        System.out.println("TestLoginModule: login preformed");
        return true;
    }

    @Override
    public boolean commit() throws LoginException {
        System.out.println("TestLoginModule: commit performed");
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        System.out.print("TestLoginModule: abort performed");
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        System.out.println("TestLoginModule: logout performed");
        return true;
    }
}
