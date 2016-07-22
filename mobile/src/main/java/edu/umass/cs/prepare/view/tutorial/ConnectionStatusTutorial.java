package edu.umass.cs.prepare.view.tutorial;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.TextView;

import edu.umass.cs.prepare.R;

/**
 * Created by snoran on 7/21/16.
 */
public class ConnectionStatusTutorial extends StandardTutorial {

    private int connectedIconId, disconnectedIconId, disabledIconId;

    /**
     * Initializes a tutorial sequence.
     * @param UI the user interface where the tutorial should be displayed
     */
    public ConnectionStatusTutorial(final Activity UI, @NonNull final View view){
        super(UI, view);
        this.setLayout(R.layout.tutorial_connection_status);
    }

    public ConnectionStatusTutorial setConnectedIcon(int resourceId){
        this.connectedIconId = resourceId;
        return this;
    }

    public ConnectionStatusTutorial setDisconnectedIcon(int resourceId){
        this.disconnectedIconId = resourceId;
        return this;
    }

    public ConnectionStatusTutorial setDisabledIcon(int resourceId){
        this.disabledIconId = resourceId;
        return this;
    }

    @Override
    public void onLayoutInflated(View view) {
        final TextView disabledIcon = (TextView) view.findViewById(R.id.txtDisabledIcon);
        final TextView disconnectedIcon = (TextView) view.findViewById(R.id.txtDisconnectedIcon);
        final TextView connectedIcon = (TextView) view.findViewById(R.id.txtConnectedIcon);

        disabledIcon.setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(UI, disabledIconId), null, null);
        disconnectedIcon.setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(UI, disconnectedIconId), null, null);
        connectedIcon.setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(UI, connectedIconId), null, null);

        super.onLayoutInflated(view);
    }
}
