package edu.umass.cs.shared.MHLClient;

public interface MHLConnectionStateHandler {
    void onConnected();
    void onConnectionFailed();
}