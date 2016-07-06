package edu.umass.cs.prepare.permissions;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;

import edu.umass.cs.shared.constants.SharedConstants;

public class PermissionsActivity extends Activity {
    private final String[] permissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                                      Manifest.permission.BLUETOOTH,
                                                      Manifest.permission.BLUETOOTH_ADMIN,
                                                      Manifest.permission.VIBRATE};
    private final int PERMISSION_REQUEST = 5;

    /**
     * Check the specified permissions
     * @param permissions list of Strings indicating permissions
     * @return true if ALL permissions are granted, false otherwise
     */
    private boolean hasPermissionsGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @TargetApi(23)
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        if (!hasPermissionsGranted(permissions)) {
            ActivityCompat.requestPermissions(PermissionsActivity.this, permissions, PERMISSION_REQUEST);
            finish();
        }
        requestPermissions(permissions, PERMISSION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST: {
                boolean permissionGranted = true;
                //If the request is cancelled, the result array is empty.
                if (grantResults.length == 0) {
                    permissionGranted = false;
                }
                for (int i = 0; i < permissions.length; i++){
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED){
                        permissionGranted = false;
                    }
                }
                Intent data = new Intent();
                data.putExtra("permission-granted", permissionGranted);
                setResult(RESULT_OK, data);
                if (permissionGranted)
                    startBeaconService();
                finish();
            }
        }
    }

    public void startBeaconService(){
        Intent startServiceIntent = new Intent(this, edu.umass.cs.prepare.metawear.BeaconService.class);
        startServiceIntent.setAction(SharedConstants.ACTIONS.START_SERVICE);
        startService(startServiceIntent);
    }
}
