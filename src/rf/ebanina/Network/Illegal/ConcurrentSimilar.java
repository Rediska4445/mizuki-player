package rf.ebanina.Network.Illegal;

import rf.ebanina.Network.IConcurrentSimilar;
import rf.ebanina.ebanina.Player.Track;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ConcurrentSimilar
        implements IConcurrentSimilar
{
    protected final ExecutorService executor = Executors.newSingleThreadExecutor();
    protected List<Future<?>> currentSimilarTasks = new ArrayList<>();

    public void updateSimilar(Track track) {

    }

    @Override
    public void kill() {
        for (Future<?> f : currentSimilarTasks) {
            if (!f.isDone()) {
                f.cancel(true);
            }
        }

        currentSimilarTasks.clear();
    }
}
