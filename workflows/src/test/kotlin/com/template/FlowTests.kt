package com.template

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.template.flows.ExampleFlowWithFixedToken
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.r3.corda.lib.tokens.workflows.utilities.*
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.slf4j.LoggerFactory

class FlowTests {
    lateinit var mockNetwork: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode
    lateinit var aIdentity: Party
    lateinit var bIdentity: Party

    val log = LoggerFactory.getLogger(this::class.java)

    @Before
    fun setup() {
        mockNetwork = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("com.template.flows"),
                TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts"),
                TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows"),
                TestCordapp.findCordapp("com.r3.corda.lib.tokens.selection").withConfig(
                        mapOf("stateSelection.inMemory.enabled" to "true",
                                "stateSelection.inMemory.indexingStrategies" to "[\"EXTERNAL_ID\"]",
                                "stateSelection.inMemory.cacheSize" to "1024")
                )
        ),
                threadPerNode = true))

        a = mockNetwork.createNode()
        b = mockNetwork.createNode()
        aIdentity = a.info.singleIdentity()
        bIdentity = b.info.singleIdentity()

        mockNetwork.startNodes()
    }

    @After
    fun tearDown() = mockNetwork.stopNodes()

    @Test
    fun `Issue a fixed token`() {
        val flow = a.startFlow(ExampleFlowWithFixedToken(500, bIdentity))
        flow.toCompletableFuture()

        val token = TokenType("INR", 0)
        val tokenQueryCriteria = tokenAmountWithIssuerCriteria(token, bIdentity)
        val resultA = a.services.vaultService.queryBy<FungibleToken>().states
        val resultB = b.services.vaultService.queryBy<FungibleToken>().states

        log.info("ResultA: $resultA")
        log.info("ResultB: $resultB")

    }
}