package edu.umass.cs.prepare.metawear;

import android.content.Context;
import android.content.Intent;

import edu.umass.cs.prepare.constants.Constants;
import edu.umass.cs.shared.BroadcastInterface;
import edu.umass.cs.shared.SharedConstants;

/**
 * Specifies how a mobile service should notify the other application components of important events,
 * e.g. the service started/stopped.
 * <br><br>
 * This specific implementation simply broadcasts a message via an {@link Intent} for other
 * application components to receive.
 *
 * @author Sean Noran
 * @affiliation University of Massachusetts Amherst
 *
 * @see BroadcastInterface
 * @see Context
 * @see Intent
 */
public class Broadcaster implements BroadcastInterface {

    /** The application component from which the message is sent. **/
    private Context context;

    public Broadcaster(Context context){
        this.context = context;
    }

    @Override
    public void broadcastMessage(int message) {
        Intent intent = new Intent();
        intent.putExtra(SharedConstants.KEY.MESSAGE, message);
        intent.setAction(Constants.ACTION.BROADCAST_MESSAGE);
        context.sendBroadcast(intent);
    }
}
