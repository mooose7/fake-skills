package com.fakeskills;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcDespawned;
import net.runelite.client.util.Text;

public class DoublingDownTracker
{
    private static final double FIRST_PAIR_XP = 4.5;
    private static final double SECOND_PAIR_XP = 9.0;

    private final Client client;

    private final Set<NPC> trackedNpcs =
            new HashSet<>();

    /*
     * Combat still uses its own sequential pair.
     */
    private String waitingMonsterName = null;

    /*
     * General actions now have independent pending pairs.
     *
     * Example:
     *
     * examine|orange
     * chop down|willow
     * open|door
     *
     * can all be waiting simultaneously.
     */
    private final Set<String> waitingActions =
            new LinkedHashSet<>();

    private boolean doubleRewardNext = false;

    private double pendingXp = 0.0;

    public DoublingDownTracker(Client client)
    {
        this.client = client;
    }

    // =====================================================
    // COMBAT
    // =====================================================

    public void onInteractingChanged(
            InteractingChanged event
    )
    {
        if (
                client.getGameState()
                        != GameState.LOGGED_IN
        )
        {
            return;
        }

        Player localPlayer =
                client.getLocalPlayer();

        if (localPlayer == null)
        {
            return;
        }

        if (event.getSource() != localPlayer)
        {
            return;
        }

        if (!(event.getTarget() instanceof NPC))
        {
            return;
        }

        NPC npc =
                (NPC) event.getTarget();

        if (npc.getName() == null)
        {
            return;
        }

        trackedNpcs.add(npc);
    }

    public void onNpcDespawned(
            NpcDespawned event
    )
    {
        if (
                client.getGameState()
                        != GameState.LOGGED_IN
        )
        {
            return;
        }

        NPC npc =
                event.getNpc();

        if (!trackedNpcs.remove(npc))
        {
            return;
        }

        if (!npc.isDead())
        {
            return;
        }

        registerNpcKill(npc);
    }

    public double update()
    {
        if (
                client.getGameState()
                        != GameState.LOGGED_IN
        )
        {
            return collectPendingXp();
        }

        Set<NPC> deadNpcs =
                new HashSet<>();

        for (NPC npc : trackedNpcs)
        {
            if (npc == null)
            {
                continue;
            }

            if (!npc.isDead())
            {
                continue;
            }

            deadNpcs.add(npc);
            registerNpcKill(npc);
        }

        trackedNpcs.removeAll(
                deadNpcs
        );

        return collectPendingXp();
    }

    private void registerNpcKill(
            NPC npc
    )
    {
        String npcName =
                cleanText(
                        npc.getName()
                );

        if (
                npcName == null
                        || npcName.isEmpty()
        )
        {
            return;
        }

        registerKill(
                npcName
        );
    }

    private void registerKill(
            String monsterName
    )
    {
        if (waitingMonsterName == null)
        {
            waitingMonsterName =
                    monsterName;

            System.out.println(
                    "Doubling Down combat: first "
                            + monsterName
                            + " kill recorded."
            );

            return;
        }

        if (
                waitingMonsterName.equals(
                        monsterName
                )
        )
        {
            double reward =
                    awardCompletedPair();

            System.out.println(
                    "Doubling Down combat: "
                            + monsterName
                            + " pair completed! +"
                            + reward
                            + " XP"
            );

            waitingMonsterName = null;
            return;
        }

        waitingMonsterName =
                monsterName;

        System.out.println(
                "Doubling Down combat: now waiting for another "
                        + monsterName
                        + "."
        );
    }

    // =====================================================
    // GENERAL ACTIONS
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
                cleanText(
                        event.getMenuOption()
                );

        String target =
                cleanText(
                        event.getMenuTarget()
                );

        MenuAction menuAction =
                event.getMenuAction();

        if (
                option == null
                        || option.isEmpty()
        )
        {
            return;
        }

        if (
                shouldIgnoreAction(
                        menuAction,
                        option
                )
        )
        {
            return;
        }

        /*
         * We still need a named target.
         *
         * This prevents empty interface clicks
         * from becoming meaningless pairs.
         */
        if (
                target == null
                        || target.isEmpty()
        )
        {
            return;
        }

        String actionKey =
                buildActionKey(
                        option,
                        target
                );

        registerAction(
                actionKey,
                option,
                target
        );
    }

    private String buildActionKey(
            String option,
            String target
    )
    {
        /*
         * RuneScape often presents item-on-item and
         * item-on-object interactions as "Use".
         *
         * The target text usually carries the useful
         * identity of that interaction, so preserving
         * option + target gives us a stable pair such as:
         *
         * use|knife -> logs
         *
         * or whatever exact normalized target RuneScape
         * supplies for that interaction.
         */
        return option
                + "|"
                + target;
    }

    private void registerAction(
            String actionKey,
            String option,
            String target
    )
    {
        /*
         * If this exact action is already armed,
         * this is occurrence #2.
         */
        if (
                waitingActions.remove(
                        actionKey
                )
        )
        {
            double reward =
                    awardCompletedPair();

            System.out.println(
                    "Doubling Down action: "
                            + option
                            + " -> "
                            + target
                            + " pair completed! +"
                            + reward
                            + " XP"
            );

            return;
        }

        /*
         * Otherwise this exact action becomes
         * independently armed at 1/2.
         *
         * It DOES NOT erase any other pending action.
         */
        waitingActions.add(
                actionKey
        );

        System.out.println(
                "Doubling Down action: "
                        + option
                        + " -> "
                        + target
                        + " armed at 1/2."
        );
    }

    private boolean shouldIgnoreAction(
            MenuAction menuAction,
            String option
    )
    {
        if (menuAction == null)
        {
            return true;
        }

        /*
         * Movement itself is not an action pair.
         */
        if (menuAction == MenuAction.WALK)
        {
            return true;
        }

        if (menuAction == MenuAction.CANCEL)
        {
            return true;
        }

        /*
         * RuneLite UI/plugin actions do not count.
         */
        switch (menuAction)
        {
            case RUNELITE:
            case RUNELITE_WIDGET:
            case RUNELITE_HIGH_PRIORITY:
            case RUNELITE_LOW_PRIORITY:
            case RUNELITE_OVERLAY:
            case RUNELITE_OVERLAY_CONFIG:
            case RUNELITE_PLAYER:
            case RUNELITE_INFOBOX:
                return true;

            default:
                break;
        }

        /*
         * Combat clicks themselves do not count.
         * Confirmed NPC deaths handle combat.
         */
        if (
                option.equalsIgnoreCase(
                        "attack"
                )
        )
        {
            return true;
        }

        /*
         * EXAMINE IS INTENTIONALLY ALLOWED.
         *
         * Orange enthusiasts rejoice.
         */
        return false;
    }

    // =====================================================
    // SHARED REWARD SEQUENCE
    // =====================================================

    private double awardCompletedPair()
    {
        double reward;

        if (doubleRewardNext)
        {
            reward =
                    SECOND_PAIR_XP;
        }
        else
        {
            reward =
                    FIRST_PAIR_XP;
        }

        pendingXp += reward;

        doubleRewardNext =
                !doubleRewardNext;

        return reward;
    }

    private double collectPendingXp()
    {
        double xp =
                pendingXp;

        pendingXp = 0.0;

        return xp;
    }

    // =====================================================
    // TEXT CLEANUP
    // =====================================================

    private String cleanText(
            String text
    )
    {
        if (text == null)
        {
            return null;
        }

        String cleaned =
                Text.removeTags(text);

        if (cleaned == null)
        {
            return null;
        }

        return cleaned
                .replace('\u00A0', ' ')
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    // =====================================================
    // TEMPORARY NPC STATE
    // =====================================================

    public void clearTemporaryNpcTracking()
    {
        trackedNpcs.clear();
        pendingXp = 0.0;
    }

    // =====================================================
    // PERSISTENT COMBAT STATE
    // =====================================================

    public String getWaitingMonsterName()
    {
        return waitingMonsterName;
    }

    public void setWaitingMonsterName(
            String monsterName
    )
    {
        if (
                monsterName == null
                        || monsterName.trim().isEmpty()
        )
        {
            waitingMonsterName = null;
            return;
        }

        waitingMonsterName =
                cleanText(
                        monsterName
                );
    }

    // =====================================================
    // PERSISTENT ACTION STATE
    // =====================================================

    /*
     * To preserve compatibility with the plugin file
     * we already built, all pending actions are stored
     * as one String separated by newline characters.
     */
    public String getWaitingAction()
    {
        if (waitingActions.isEmpty())
        {
            return null;
        }

        return String.join(
                "\n",
                waitingActions
        );
    }

    public void setWaitingAction(
            String savedActions
    )
    {
        waitingActions.clear();

        if (
                savedActions == null
                        || savedActions.trim().isEmpty()
        )
        {
            return;
        }

        String[] actions =
                savedActions.split(
                        "\\n"
                );

        for (String action : actions)
        {
            if (
                    action == null
                            || action.trim().isEmpty()
            )
            {
                continue;
            }

            waitingActions.add(
                    action.trim()
                            .toLowerCase(
                                    Locale.ROOT
                            )
            );
        }
    }

    // =====================================================
    // PERSISTENT REWARD STATE
    // =====================================================

    public boolean isDoubleRewardNext()
    {
        return doubleRewardNext;
    }

    public void setDoubleRewardNext(
            boolean doubleRewardNext
    )
    {
        this.doubleRewardNext =
                doubleRewardNext;
    }
}