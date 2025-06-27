package com.amberclient.utils.murdererfinder.access

interface PlayerEntityMixinAccess {
    fun isMurder(): Boolean
    fun hasBow(): Boolean
    fun isRealPlayer(): Boolean
    fun isDeadSpectator(): Boolean
}