package edu.umass.cs.prepare.MHLClient;

public interface MHLConnectionStateHandler {
    void onConnected();
    void onConnectionFailed();
}