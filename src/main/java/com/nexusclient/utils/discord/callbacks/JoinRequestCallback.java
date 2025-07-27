package com.nexusclient.utils.discord.callbacks;

import com.nexusclient.utils.discord.DiscordUser;
import com.sun.jna.Callback;

public interface JoinRequestCallback extends Callback {
    void apply(final DiscordUser p0);
}
