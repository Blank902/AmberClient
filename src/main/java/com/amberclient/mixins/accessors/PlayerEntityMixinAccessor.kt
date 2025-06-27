package com.amberclient.mixins.accessors

interface PlayerEntityMixinAccessor {
    fun isMurder(): Boolean
    fun hasBow(): Boolean
    fun isRealPlayer(): Boolean
    fun isDeadSpectator(): Boolean
}