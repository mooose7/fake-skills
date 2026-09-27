package com.fakeskills;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.util.Text;

public class YappingTracker
{
    private static final double XP_PER_CHARACTER = 1.0;

    private final Client client;

    public YappingTracker(Client client)
    {
        this.client = client;
    }

    public double processMessage(ChatMessage event)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return 0.0;
        }

        if (event == null)
        {
            return 0.0;
        }

        ChatMessageType type =
                event.getType();

        if (!isPlayerChatType(type))
        {
            return 0.0;
        }

        /*
         * PRIVATECHATOUT is specifically an
         * outgoing private message, so we
         * already know it belongs to us.
         */
        if (type != ChatMessageType.PRIVATECHATOUT)
        {
            if (!isOurMessage(event))
            {
                return 0.0;
            }
        }

        String message =
                event.getMessage();

        if (message == null)
        {
            return 0.0;
        }

        /*
         * Remove RuneLite/Jagex formatting
         * tags before counting characters.
         *
         * Spaces and punctuation remain.
         */
        String cleanMessage =
                Text.removeTags(message);

        if (cleanMessage == null
                || cleanMessage.isEmpty())
        {
            return 0.0;
        }

        /*
         * Count Unicode characters rather
         * than raw Java storage units.
         *
         * This means normal letters,
         * numbers, punctuation, spaces,
         * and most emoji count naturally.
         */
        int characterCount =
                cleanMessage.codePointCount(
                        0,
                        cleanMessage.length()
                );

        return characterCount
                * XP_PER_CHARACTER;
    }

    private boolean isPlayerChatType(
            ChatMessageType type
    )
    {
        switch (type)
        {
            case PUBLICCHAT:
            case MODCHAT:
            case PRIVATECHATOUT:
            case FRIENDSCHAT:
            case CLAN_CHAT:
            case CLAN_GUEST_CHAT:
            case CLAN_GIM_CHAT:
                return true;

            default:
                return false;
        }
    }

    private boolean isOurMessage(
            ChatMessage event
    )
    {
        Player player =
                client.getLocalPlayer();

        if (player == null
                || player.getName() == null)
        {
            return false;
        }

        String eventName =
                event.getName();

        if (eventName == null)
        {
            return false;
        }

        String ourName =
                normalizeName(
                        player.getName()
                );

        String messageName =
                normalizeName(
                        eventName
                );

        return ourName.equalsIgnoreCase(
                messageName
        );
    }

    private String normalizeName(
            String name
    )
    {
        return Text.removeTags(name)
                .replace('\u00A0', ' ')
                .trim();
    }
}