package edu.umass.cs.prepare;

import android.content.Context;
import android.content.Intent;

import edu.umass.cs.shared.BroadcastInterface;
import edu.umass.cs.shared.SharedConstants;

/**
 * Specifies how a wearable service should notify the other application components of important events,
 * e.g. the service started/stopped.
 * <br><br>
 * This specific implementation sends data via the {@link DataClient} from the wearable to the
 * mobile application.
 *
 * @author Sean Noran
 * @affiliation University of Massachusetts Amherst
 *
 * @see BroadcastInterface
 * @see Context
 * @see DataClient
 */
public class Broadcaster implements BroadcastInterface {

    private Context context;
    private DataClient client;

    public Broadcaster(Context context){
        this.context = context;
    }

    @Override
    public void broadcastMessage(int message) {
        if (client == null)
            client = DataClient.getInstance(context);
        client.sendMessage(message);
    }
}
