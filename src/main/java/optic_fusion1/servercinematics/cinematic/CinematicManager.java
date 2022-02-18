package optic_fusion1.servercinematics.cinematic;

import java.util.HashMap;
import java.util.Map;

public final class CinematicManager {

    private final Map<String, CinematicPath> cinematicPaths = new HashMap<>();

    public void addCinematic(CinematicPath path) {
        this.cinematicPaths.put(path.getId(), path);
    }

    public void removeCinematic(CinematicPath path) {
        this.cinematicPaths.remove(path.getId());
    }

    public CinematicPath removeCinematic(String id) {
        return cinematicPaths.remove(id);
    }

    public CinematicPath getCinematic(String id) {
        return cinematicPaths.get(id);
    }

    public void clearCinematics() {
        this.cinematicPaths.clear();
    }

}
