package com.template.flows

import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import net.corda.core.serialization.SerializationWhitelist

class TokenSerializationWhitelist: SerializationWhitelist {
        override val whitelist = listOf(PartyAndAmount::class.java)
}