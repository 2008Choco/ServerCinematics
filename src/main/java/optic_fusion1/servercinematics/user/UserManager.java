package optic_fusion1.servercinematics.user;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

public class UserManager {

    private static final HashMap<UUID, User> USERS = new HashMap<>();

    public Collection<User> getUsers() {
        return Collections.unmodifiableCollection(USERS.values());
    }

    public User getUser(UUID uniqueId) {
        return USERS.get(uniqueId);
    }

    public void addUser(User user) {
        USERS.putIfAbsent(user.getUniqueID(), user);
    }

    public boolean containsUser(UUID uniqueId) {
        return USERS.containsKey(uniqueId);
    }

    public void removeUser(UUID uniqueId) {
        USERS.remove(uniqueId);
    }
}
