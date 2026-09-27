package com.fakeskills;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.GroundObject;
import net.runelite.api.NPC;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.api.coords.WorldPoint;

public class BankStandingTracker
{
    /*
     * =========================
     * BANK STANDING RULES
     * =========================
     *
     * Player must:
     *
     * 1. Be completely stationary.
     *
     * 2. Be within 3 tiles of an approved
     *    banker NPC OR banking object.
     *
     * 3. Remain there for 4 game ticks.
     *
     * Award:
     *
     * +4.5 Bank Standing XP
     * every 4 qualifying game ticks.
     */

    private static final double XP_PER_AWARD = 2.0;

    private static final int BANK_RANGE = 3;

    private static final int TICKS_PER_AWARD = 5;

    private final Client client;

    private WorldPoint previousLocation;

    private int qualifyingTicks = 0;

    /*
     * =========================
     * APPROVED BANKER NPCS
     * =========================
     *
     * Stored lowercase so matching is
     * case-insensitive.
     */

    private static final Set<String> BANKER_NPCS =
            new HashSet<>(
                    Arrays.asList(
                            "banker",
                            "grand exchange clerk",
                            "'birds-eye' jack",
                            "arnold lydspor",
                            "ashuelot reis",
                            "cornelius",
                            "emerald benedict",
                            "eniola",
                            "fadli",
                            "gundai",
                            "peer the seer",
                            "standard banker",
                            "ghost banker",
                            "gnome banker",
                            "vampyre banker",
                            "tzhaar-ket banker",
                            "cave goblin banker",
                            "elf banker",
                            "dwarf banker"
                    )
            );

    /*
     * =========================
     * APPROVED BANK OBJECTS
     * =========================
     */

    private static final Set<String> BANK_OBJECTS =
            new HashSet<>(
                    Arrays.asList(
                            "bank booth",
                            "bank chest",
                            "poll booth",
                            "bank table",
                            "grand exchange booth",
                            "bank counter",
                            "bank desk",
                            "bank boat",
                            "bank chest-wreck",
                            "culinaromancer's chest",
                            "bank deposit box",
                            "bank deposit pot",
                            "deposit pool",
                            "mausoleum"
                    )
            );

    public BankStandingTracker(Client client)
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

            qualifyingTicks = 0;

            return 0.0;
        }

        /*
         * =========================
         * MOVEMENT CHECK
         * =========================
         *
         * Bank Standing requires actually
         * standing still.
         *
         * Any movement resets the counter.
         */

        if (!currentLocation.equals(previousLocation))
        {
            previousLocation =
                    currentLocation;

            qualifyingTicks = 0;

            return 0.0;
        }

        previousLocation =
                currentLocation;

        /*
         * =========================
         * BANK CHECK
         * =========================
         *
         * Either condition is enough:
         *
         * approved NPC within 3 tiles
         *
         * OR
         *
         * approved object within 3 tiles
         */

        boolean nearBank =
                isNearBankerNpc(currentLocation)
                        || isNearBankObject(currentLocation);

        if (!nearBank)
        {
            qualifyingTicks = 0;

            return 0.0;
        }

        /*
         * We are:
         *
         * stationary
         * +
         * near an approved bank source.
         */

        qualifyingTicks++;

        /*
         * Award every four qualifying
         * game ticks.
         */

        if (qualifyingTicks >= TICKS_PER_AWARD)
        {
            qualifyingTicks = 0;

            return XP_PER_AWARD;
        }

        return 0.0;
    }

    /*
     * =========================
     * NPC DETECTION
     * =========================
     */

    private boolean isNearBankerNpc(
            WorldPoint playerLocation
    )
    {
        /*
         * Current RuneLite exposes NPCs
         * through the current WorldView.
         */
        for (
                NPC npc :
                client.getTopLevelWorldView().npcs()
        )
        {
            if (npc == null)
            {
                continue;
            }

            String name =
                    npc.getName();

            if (name == null)
            {
                continue;
            }

            String normalizedName =
                    name
                            .toLowerCase(
                                    Locale.ROOT
                            )
                            .trim();

            /*
             * Not one of our approved bankers.
             */
            if (
                    !BANKER_NPCS.contains(
                            normalizedName
                    )
            )
            {
                continue;
            }

            WorldPoint npcLocation =
                    npc.getWorldLocation();

            if (npcLocation == null)
            {
                continue;
            }

            /*
             * WorldPoint.distanceTo()
             * also protects us from NPCs
             * on another plane.
             */
            int distance =
                    playerLocation.distanceTo(
                            npcLocation
                    );

            if (distance <= BANK_RANGE)
            {
                return true;
            }
        }

        return false;
    }

    /*
     * =========================
     * OBJECT DETECTION
     * =========================
     */

    private boolean isNearBankObject(
            WorldPoint playerLocation
    )
    {
        Scene scene =
                client.getScene();

        if (scene == null)
        {
            return false;
        }

        Tile[][][] tiles =
                scene.getTiles();

        if (tiles == null)
        {
            return false;
        }

        int plane =
                client.getPlane();

        int baseX =
                client.getBaseX();

        int baseY =
                client.getBaseY();

        /*
         * Search a 7x7 square centered
         * on the player:
         *
         * -3 through +3 on X
         * -3 through +3 on Y
         */

        for (
                int dx = -BANK_RANGE;
                dx <= BANK_RANGE;
                dx++
        )
        {
            for (
                    int dy = -BANK_RANGE;
                    dy <= BANK_RANGE;
                    dy++
            )
            {
                int worldX =
                        playerLocation.getX()
                                + dx;

                int worldY =
                        playerLocation.getY()
                                + dy;

                int sceneX =
                        worldX - baseX;

                int sceneY =
                        worldY - baseY;

                /*
                 * Make sure coordinates
                 * are inside the scene.
                 */

                if (
                        plane < 0
                                || plane >= tiles.length
                )
                {
                    continue;
                }

                if (
                        sceneX < 0
                                || sceneX
                                >= tiles[plane].length
                )
                {
                    continue;
                }

                if (
                        sceneY < 0
                                || sceneY
                                >= tiles[plane][sceneX].length
                )
                {
                    continue;
                }

                Tile tile =
                        tiles[plane]
                                [sceneX]
                                [sceneY];

                if (tile == null)
                {
                    continue;
                }

                /*
                 * Confirm actual tile distance.
                 *
                 * RuneScape tile distance treats
                 * diagonal neighboring tiles
                 * appropriately.
                 */

                if (
                        playerLocation.distanceTo(
                                tile.getWorldLocation()
                        ) > BANK_RANGE
                )
                {
                    continue;
                }

                if (tileContainsBankObject(tile))
                {
                    return true;
                }
            }
        }

        return false;
    }

    /*
     * =========================
     * TILE OBJECT SEARCH
     * =========================
     */

    private boolean tileContainsBankObject(
            Tile tile
    )
    {
        /*
         * Wall object.
         */

        WallObject wallObject =
                tile.getWallObject();

        if (isApprovedBankObject(wallObject))
        {
            return true;
        }

        /*
         * Ground object.
         */

        GroundObject groundObject =
                tile.getGroundObject();

        if (isApprovedBankObject(groundObject))
        {
            return true;
        }

        /*
         * Game objects.
         */

        GameObject[] gameObjects =
                tile.getGameObjects();

        if (gameObjects != null)
        {
            for (
                    GameObject gameObject :
                    gameObjects
            )
            {
                if (
                        isApprovedBankObject(
                                gameObject
                        )
                )
                {
                    return true;
                }
            }
        }

        return false;
    }

    /*
     * =========================
     * OBJECT NAME CHECK
     * =========================
     */

    private boolean isApprovedBankObject(
            TileObject object
    )
    {
        if (object == null)
        {
            return false;
        }

        ObjectComposition composition =
                client.getObjectDefinition(
                        object.getId()
                );

        if (composition == null)
        {
            return false;
        }

        /*
         * Some RuneScape objects can
         * transform into another definition.
         *
         * Use the active form when possible.
         */

        if (
                composition.getImpostorIds()
                        != null
        )
        {
            try
            {
                ObjectComposition impostor =
                        composition.getImpostor();

                if (impostor != null)
                {
                    composition =
                            impostor;
                }
            }
            catch (Exception ignored)
            {
                /*
                 * Keep original definition.
                 */
            }
        }

        String name =
                composition.getName();

        if (name == null)
        {
            return false;
        }

        String normalizedName =
                name
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .trim();

        return BANK_OBJECTS.contains(
                normalizedName
        );
    }

    /*
     * =========================
     * RESET
     * =========================
     */

    public void reset()
    {
        previousLocation = null;
        qualifyingTicks = 0;
    }
}