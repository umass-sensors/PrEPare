package edu.umass.cs.prepare.view.tools;

import android.annotation.SuppressLint;
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
 *
 * @author Sean Noran
 * @affiliation University of Massachusetts Amherst
 *
 * @see ActionProvider
 * @see BatteryStatusActionProvider
 */
public class ConnectionStatusActionProvider extends ActionProvider implements View.OnClickListener {

    /** The content description of the connection status icon, used for improved accessibility. **/
    private String contentDescription;

    /** The icon representing the device. **/
    private View deviceIcon;

    /** The action provider view. **/
    private View view;

    /** A mapping from connection states to drawable resource IDs. **/
    private HashMap<CONNECTION_STATUS, Integer> connectionStatusMap;

    /** The current connection state. **/
    private CONNECTION_STATUS status = CONNECTION_STATUS.DISCONNECTED;

    public interface OnClickListener {
        void onClick(CONNECTION_STATUS state);
    }

    private OnClickListener onClickListener;

    /**
     * Set of connection states. One of {@link CONNECTION_STATUS#DISABLED},
     * {@link CONNECTION_STATUS#DISCONNECTED}, {@link CONNECTION_STATUS#CONNECTED},
     * {@link CONNECTION_STATUS#ERROR} or {@link CONNECTION_STATUS#DEFAULT}.
     */
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

    @SuppressLint("InflateParams")
    @Override
    public View onCreateActionView() {
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        view = layoutInflater.inflate(R.layout.action_provider_connection_status, null);
        view.setOnClickListener(this);

        View progressBar = view.findViewById(R.id.circularProgressBar);
        deviceIcon = view.findViewById(R.id.deviceIcon);
        deviceIcon.setContentDescription(contentDescription);
        updateDrawable();

        int color = 0x00002200;
        ((ProgressBar) progressBar).getIndeterminateDrawable().setColorFilter(color, PorterDuff.Mode.LIGHTEN);

        return view;
    }

    /**
     * Sets the current connection status and updates the icon in the toolbar.
     * @param status the connection state
     */
    public void setStatus(CONNECTION_STATUS status){
        this.status = status;
        updateDrawable();
    }

    /**
     * Updates the action provider image view, depending on the current connection status.
     * If there is no corresponding drawable resource then the resource associated with
     * {@link CONNECTION_STATUS#DEFAULT} will be displayed. The resource corresponding to
     * {@link CONNECTION_STATUS#DISCONNECTED} will have alpha 0.4.
     */
    private void updateDrawable(){
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

    /**
     * Removes the action provider view from the toolbar, shifting any other action providers as necessary.
     */
    public void hide(){
        view.setVisibility(View.GONE);
    }

    /**
     * Shows the action provider view in the toolbar, shifting any other action providers as necessary.
     */
    public void show(){
        view.setVisibility(View.VISIBLE);
    }

    /**
     * Sets the drawable resource ID for the given connection status.
     * @param status the connection state
     * @param resourceId reference to a drawable resource
     */
    public void setDrawable(CONNECTION_STATUS status, int resourceId){
        connectionStatusMap.put(status, resourceId);
    }

    /**
     * Sets the listener for handling click events.
     * @param onClickListener defines how the click events should be handled.
     */
    public void setOnClickListener(final OnClickListener onClickListener){
        this.onClickListener = onClickListener;
    }

    /**
     * Sets the content description of the connection status icon and updates the view immediately if available.
     * @param contentDescription describes the view content.
     */
    public void setContentDescription(String contentDescription){
        this.contentDescription = contentDescription;
        if (deviceIcon != null)
            deviceIcon.setContentDescription(contentDescription);
    }

    @Override
    public void onClick(View view) {
        if (onClickListener != null)
            onClickListener.onClick(status);
    }
}