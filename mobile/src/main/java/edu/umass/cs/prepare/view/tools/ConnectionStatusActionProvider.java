package edu.umass.cs.prepare.view.tools;

import android.content.Context;
import android.graphics.PorterDuff;
import android.support.v4.view.ActionProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.util.HashMap;

import edu.umass.cs.prepare.R;

/**
 * A connection status action provider is a specialized tool which displays device
 * connectivity status in the main toolbar.
 */
public class ConnectionStatusActionProvider extends ActionProvider {

    /** The circular progress bar indicating a connection attempt. **/
    private View progressBar;

    /** The icon representing the device. **/
    private View deviceIcon;

    /** The action provider view. **/
    private View view;

    private HashMap<CONNECTION_STATUS, Integer> connectionStatusMap;

    public enum CONNECTION_STATUS {
        DISABLED,
        DISCONNECTED,
        CONNECTED,
        ERROR,
        DEFAULT
    }

    /**
     * Context for accessing resources.
     */
    private final Context mContext;

    public ConnectionStatusActionProvider(Context context) {
        super(context);
        mContext = context;
        connectionStatusMap = new HashMap<>();
    }

    @Override
    public View onCreateActionView() {
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        view = layoutInflater.inflate(R.layout.action_provider_connection_status, null);
        progressBar = view.findViewById(R.id.circularProgressBar);
        deviceIcon = view.findViewById(R.id.deviceIcon);
        setStatus(CONNECTION_STATUS.DISABLED);

        int color = 0x00002200; //TODO: More appropriate color
        ((ProgressBar)progressBar).getIndeterminateDrawable().setColorFilter(color, PorterDuff.Mode.LIGHTEN);

        return view;
    }

    public void setStatus(CONNECTION_STATUS status){
        Integer resourceId = connectionStatusMap.get(status);
        if (resourceId == null)
            resourceId = connectionStatusMap.get(CONNECTION_STATUS.DEFAULT);
        if (deviceIcon != null){
            if (resourceId != null)
                ((ImageView) deviceIcon).setImageResource(resourceId);
            if (status == CONNECTION_STATUS.DISCONNECTED || status == CONNECTION_STATUS.DISABLED) {
                deviceIcon.setAlpha(0.4f);
            } else {
                deviceIcon.setAlpha(1f);
            }
        }
    }

    public void hide(){
        view.setVisibility(View.GONE);
    }

    public void show(){
        view.setVisibility(View.VISIBLE);
    }

    public void setDrawable(CONNECTION_STATUS status, int resourceId){
        connectionStatusMap.put(status, resourceId);
    }
}