package com.virgilsecurity.android.e3kit_test_target.utils

import com.virgilsecurity.android.ethree.interaction.EThree

/**
 * EThreeHolder
 */
object EThreeHolder {

    private var ethree: EThree? = null

    fun setEthree(ethree: EThree?) {
        this.ethree = ethree
    }

    fun getEthree(): EThree? = ethree

    fun getEthreeNotNull(): EThree = ethree ?: error("Ethree is not initialized")
}
