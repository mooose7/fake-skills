package com.fakeskills;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;

public class PrayingTracker
{
    /*
     * Base XP for each Prayer point lost,
     * multiplied by the number of prayers
     * currently active.
     */
    private static final double XP_PER_PRAYER_POINT = 4.5;

    /*
     * Maximum number of simultaneously
     * active prayers that can multiply XP.
     */
    private static final int MAX_ACTIVE_PRAYERS = 15;

    /*
     * Prayer-point losses larger than this
     * are rejected completely.
     */
    private static final int MAX_QUALIFYING_DRAIN = 15;

    private final Client client;

    /*
     * -1 means we haven't recorded the
     * player's Prayer points yet.
     */
    private int previousPrayerPoints = -1;

    public PrayingTracker(Client client)
    {
        this.client = client;
    }

    /*
     * Called once per GameTick.
     */
    public double update()
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            reset();
            return 0.0;
        }

        int currentPrayerPoints =
                client.getBoostedSkillLevel(
                        Skill.PRAYER
                );

        /*
         * First observation.
         *
         * Establish the starting value
         * without awarding XP.
         */
        if (previousPrayerPoints < 0)
        {
            previousPrayerPoints =
                    currentPrayerPoints;

            return 0.0;
        }

        int prayerPointsLost =
                previousPrayerPoints
                        - currentPrayerPoints;

        /*
         * Remember the current value for
         * our next GameTick.
         */
        previousPrayerPoints =
                currentPrayerPoints;

        /*
         * No Prayer points were consumed.
         *
         * This also prevents Prayer restores,
         * altars, potions, etc. from
         * awarding XP.
         */
        if (prayerPointsLost <= 0)
        {
            return 0.0;
        }

        /*
         * Reject unusually large Prayer
         * losses.
         */
        if (
                prayerPointsLost
                        > MAX_QUALIFYING_DRAIN
        )
        {
            return 0.0;
        }

        /*
         * Count how many prayers are
         * currently active.
         */
        int activePrayers =
                countActivePrayers();

        /*
         * Prayer points disappeared but
         * no prayers are active.
         *
         * Do not award Fake Praying XP.
         */
        if (activePrayers <= 0)
        {
            return 0.0;
        }

        /*
         * Cap the multiplier at 15 active
         * prayers.
         */
        if (activePrayers > MAX_ACTIVE_PRAYERS)
        {
            activePrayers =
                    MAX_ACTIVE_PRAYERS;
        }

        /*
         * FINAL FORMULA:
         *
         * Prayer points lost
         * ×
         * active prayers
         * ×
         * 4.5 XP
         *
         * Examples:
         *
         * 1 point × 1 prayer × 4.5
         * = 4.5 XP
         *
         * 1 point × 3 prayers × 4.5
         * = 13.5 XP
         *
         * 2 points × 3 prayers × 4.5
         * = 27 XP
         *
         * 1 point × 10 prayers × 4.5
         * = 45 XP
         */
        return prayerPointsLost
                * activePrayers
                * XP_PER_PRAYER_POINT;
    }

    /*
     * Count every prayer RuneLite reports
     * as currently active.
     */
    @SuppressWarnings("deprecation")
    private int countActivePrayers()
    {
        int count = 0;

        for (Prayer prayer : Prayer.values())
        {
            if (client.isPrayerActive(prayer))
            {
                count++;
            }
        }

        return count;
    }

    public void reset()
    {
        previousPrayerPoints = -1;
    }
}