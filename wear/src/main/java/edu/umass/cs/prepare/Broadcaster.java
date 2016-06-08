package edu.umass.cs.prepare;

import android.content.Context;

import edu.umass.cs.shared.BroadcastInterface;
import edu.umass.cs.shared.SharedConstants;

/**
 * Created by snoran on 6/8/16.
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
        client.sendMessage(SharedConstants.MESSAGES.BEACON_SERVICE_STARTED);
    }
}
