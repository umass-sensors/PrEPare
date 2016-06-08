package edu.umass.cs.prepare.metawear;

import android.content.Context;
import android.content.Intent;

import edu.umass.cs.prepare.constants.Constants;
import edu.umass.cs.shared.BroadcastInterface;
import edu.umass.cs.shared.SharedConstants;

/**
 * Created by snoran on 6/8/16.
 */
public class Broadcaster implements BroadcastInterface {

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
