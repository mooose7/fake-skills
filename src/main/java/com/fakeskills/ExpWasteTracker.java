package com.fakeskills;

import java.util.EnumMap;
import java.util.Map;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuOptionClicked;

public class ExpWasteTracker
{
    private static final double XP_PER_SECOND = 1.0;

    private static final long ONE_SECOND_NANOS =
            1_000_000_000L;

    /*
     * More than two still ticks are required.
     *
     * Tick 1 = no
     * Tick 2 = no
     * Tick 3 = eligible to begin idle timing.
     */
    private static final int REQUIRED_STILL_TICKS = 3;

    private final Client client;

    private final Map<Skill, Integer> previousXp =
            new EnumMap<>(Skill.class);

    private WorldPoint previousLocation = null;

    private int stillTicks = 0;

    private boolean initialized = false;

    private long idleStartTime = -1L;
    private long lastWasteAwardTime = -1L;

    private boolean gameplayActionThisTick = false;

    /*
     * If ANY of our other Fake Skills awards XP,
     * FakeSkillsPlugin tells us here.
     *
     * That means the player is not currently
     * "wasting XP".
     */
    private boolean fakeXpEarned = false;

    public ExpWasteTracker(Client client)
    {
        this.client = client;
    }

    // =====================================================
    // OTHER FAKE SKILL XP
    // =====================================================

    public void notifyFakeXpEarned()
    {
        fakeXpEarned = true;

        /*
         * Reset immediately rather than waiting
         * for the next game tick.
         */
        stillTicks = 0;
        resetIdleTimer();
    }

    // =====================================================
    // MENU ACTIONS
    // =====================================================

    public void onMenuOptionClicked(
            MenuOptionClicked event
    )
    {
        if (
                client.getGameState()
                        != GameState.LOGGED_IN
        )
        {
            return;
        }

        if (event == null)
        {
            return;
        }

        String option =
                event.getMenuOption();

        if (option == null)
        {
            return;
        }

        option =
                option.trim();

        /*
         * Examine remains glorious XP waste.
         */
        if (
                option.equalsIgnoreCase(
                        "examine"
                )
        )
        {
            return;
        }

        if (
                option.equalsIgnoreCase(
                        "cancel"
                )
        )
        {
            return;
        }

        /*
         * Actual movement is detected by tile,
         * so clicking Walk Here doesn't need
         * to count separately.
         */
        if (
                option.equalsIgnoreCase(
                        "walk here"
                )
        )
        {
            return;
        }

        gameplayActionThisTick = true;
    }

    // =====================================================
    // UPDATE
    // =====================================================

    public double update()
    {
        if (
                client.getGameState()
                        != GameState.LOGGED_IN
        )
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

        long now =
                System.nanoTime();

        WorldPoint currentLocation =
                player.getWorldLocation();

        if (!initialized)
        {
            initialize(
                    currentLocation
            );

            return 0.0;
        }

        // =================================================
        // OTHER FAKE SKILL XP
        // =================================================

        if (fakeXpEarned)
        {
            fakeXpEarned = false;

            stillTicks = 0;
            resetIdleTimer();

            previousLocation =
                    currentLocation;

            gameplayActionThisTick = false;

            return 0.0;
        }

        // =================================================
        // REAL OSRS XP
        // =================================================

        boolean realXpGained =
                checkForRealXpGain();

        if (realXpGained)
        {
            stillTicks = 0;
            resetIdleTimer();

            previousLocation =
                    currentLocation;

            gameplayActionThisTick = false;

            return 0.0;
        }

        // =================================================
        // MOVEMENT
        // =================================================

        boolean moved =
                previousLocation == null
                        || !currentLocation.equals(
                        previousLocation
                );

        previousLocation =
                currentLocation;

        if (moved)
        {
            stillTicks = 0;
            resetIdleTimer();

            gameplayActionThisTick = false;

            return 0.0;
        }

        // =================================================
        // GAMEPLAY INTERACTION
        // =================================================

        if (gameplayActionThisTick)
        {
            stillTicks = 0;
            resetIdleTimer();

            gameplayActionThisTick = false;

            return 0.0;
        }

        gameplayActionThisTick = false;

        // =================================================
        // ACTIVE CHARACTER ANIMATION
        // =================================================

        if (player.getAnimation() != -1)
        {
            stillTicks = 0;
            resetIdleTimer();

            return 0.0;
        }

        // =================================================
        // ACTUALLY IDLE
        // =================================================

        stillTicks++;

        if (
                stillTicks
                        < REQUIRED_STILL_TICKS
        )
        {
            resetIdleTimer();
            return 0.0;
        }

        /*
         * We have just become truly idle.
         * Start the one-second timer.
         */
        if (idleStartTime < 0L)
        {
            idleStartTime = now;
            lastWasteAwardTime = now;

            return 0.0;
        }

        long elapsed =
                now - lastWasteAwardTime;

        long fullSeconds =
                elapsed / ONE_SECOND_NANOS;

        if (fullSeconds <= 0)
        {
            return 0.0;
        }

        lastWasteAwardTime +=
                fullSeconds
                        * ONE_SECOND_NANOS;

        return fullSeconds
                * XP_PER_SECOND;
    }

    // =====================================================
    // INITIALIZATION
    // =====================================================

    private void initialize(
            WorldPoint currentLocation
    )
    {
        previousXp.clear();

        for (Skill skill : Skill.values())
        {
            if (skill == Skill.OVERALL)
            {
                continue;
            }

            previousXp.put(
                    skill,
                    client.getSkillExperience(
                            skill
                    )
            );
        }

        previousLocation =
                currentLocation;

        stillTicks = 0;

        idleStartTime = -1L;
        lastWasteAwardTime = -1L;

        gameplayActionThisTick = false;
        fakeXpEarned = false;

        initialized = true;
    }

    // =====================================================
    // REAL XP CHECK
    // =====================================================

    private boolean checkForRealXpGain()
    {
        boolean gainedXp = false;

        for (Skill skill : Skill.values())
        {
            if (skill == Skill.OVERALL)
            {
                continue;
            }

            int currentXp =
                    client.getSkillExperience(
                            skill
                    );

            Integer oldXp =
                    previousXp.get(
                            skill
                    );

            if (
                    oldXp != null
                            && currentXp > oldXp
            )
            {
                gainedXp = true;
            }

            previousXp.put(
                    skill,
                    currentXp
            );
        }

        return gainedXp;
    }

    private void resetIdleTimer()
    {
        idleStartTime = -1L;
        lastWasteAwardTime = -1L;
    }

    // =====================================================
    // FULL RESET
    // =====================================================

    public void reset()
    {
        previousXp.clear();

        previousLocation = null;

        stillTicks = 0;

        idleStartTime = -1L;
        lastWasteAwardTime = -1L;

        gameplayActionThisTick = false;
        fakeXpEarned = false;

        initialized = false;
    }
}