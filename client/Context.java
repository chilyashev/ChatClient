package client;

import java.util.HashMap;

/**
 * Used to pass data between the controllers of different views.
 * I know a global context is not the best practice, but screw it.
 * For the current goals it will do well enough.
 * Date: 4/20/14 4:23 AM
 *
 * @author Mihail Chilyashev
 */
public class Context {
    private final static Context instance = new Context();

    /**
     * Holds the data needed to start the model.
     */
    private HashMap<String, Object> data = new HashMap<>();
    private HashMap<String, Object> clients = new HashMap<>();

    public synchronized static Context getInstance() {
        return instance;
    }

    public synchronized HashMap<String, Object> getClients() {
        return clients;
    }

    public synchronized void set(String key, Object value) {
        data.put(key, value);
    }

    public synchronized void removeClient(String key) {
        clients.remove(key);
    }

    public synchronized Object get(String key) {
        return data.get(key);
    }

    public synchronized Integer getInteger(String key) {
        return (Integer) data.get(key);
    }
}
