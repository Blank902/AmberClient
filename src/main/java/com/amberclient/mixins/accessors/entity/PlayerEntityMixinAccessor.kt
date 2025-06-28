package com.amberclient.mixins.accessors.entity

interface PlayerEntityMixinAccessor {
    fun isMurder(): Boolean
    fun hasBow(): Boolean
    fun isRealPlayer(): Boolean
    fun isDeadSpectator(): Boolean
}