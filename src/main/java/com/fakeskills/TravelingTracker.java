package com.fakeskills;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

public class TravelingTracker
{
    /*
     * Traveling XP:
     *
     * Normal:
     * 1.5 XP per tile
     *
     * With follower:
     * 3.0 XP per tile
     */
    private static final double XP_PER_TILE = 1.5;

    /*
     * Walking = normally 1 tile between ticks.
     * Running = normally up to 2.
     *
     * Anything larger is rejected so teleports
     * don't become Traveling XP.
     */
    private static final int MAX_NORMAL_MOVEMENT = 2;

    private final Client client;

    private WorldPoint previousLocation;

    public TravelingTracker(Client client)
    {
        this.client = client;
    }

    public double update()
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            reset();
            return 0.0;
        }

        Player player =
                client.getLocalPlayer();

        if (player == null)
        {
            reset();
            return 0.0;
        }

        WorldPoint currentLocation =
                player.getWorldLocation();

        /*
         * First observation.
         */
        if (previousLocation == null)
        {
            previousLocation =
                    currentLocation;

            return 0.0;
        }

        /*
         * Plane changed.
         *
         * Don't count this as normal
         * Traveling movement.
         */
        if (
                currentLocation.getPlane()
                        != previousLocation.getPlane()
        )
        {
            previousLocation =
                    currentLocation;

            return 0.0;
        }

        int distance =
                previousLocation.distanceTo(
                        currentLocation
                );

        /*
         * Remember our new position.
         */
        previousLocation =
                currentLocation;

        /*
         * Didn't move.
         */
        if (distance <= 0)
        {
            return 0.0;
        }

        /*
         * Too large to consider ordinary
         * walking/running.
         */
        if (distance > MAX_NORMAL_MOVEMENT)
        {
            return 0.0;
        }

        double multiplier = 1.0;

        /*
         * Your follower/pet doubles
         * Traveling XP.
         */
        if (client.getFollower() != null)
        {
            multiplier = 2.0;
        }

        return distance
                * XP_PER_TILE
                * multiplier;
    }

    public void reset()
    {
        previousLocation = null;
    }
}