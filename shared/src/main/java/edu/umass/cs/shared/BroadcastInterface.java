package edu.umass.cs.shared;

/**
 * The BroadcastInterface defines the component-level communication protocol. More precisely, subclasses
 * must handle concrete communications between application components. From a wearable device,
 * the implementation may involve sending data through the Google Play services data layer API to
 * persist notifications the mobile device; from the phone itself, this may be as simple as
 * broadcasting an intent.
 *
 * @author Sean Noran
 * @affiliation University of Massachusetts Amherst
 *
 */
public interface BroadcastInterface {
    void broadcastMessage(int message);
}
