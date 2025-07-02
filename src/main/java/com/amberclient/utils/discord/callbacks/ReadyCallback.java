package com.amberclient.utils.discord.callbacks;

import com.amberclient.utils.discord.DiscordUser;
import com.sun.jna.Callback;

public interface ReadyCallback extends Callback {
    void apply(final DiscordUser p0);
}
