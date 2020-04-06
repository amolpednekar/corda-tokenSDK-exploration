package com.template

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import com.r3.corda.lib.tokens.workflows.utilities.heldTokenAmountCriteria
import net.corda.core.contracts.Amount
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.singleIdentity
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeHandle
import net.corda.testing.driver.driver
import net.corda.testing.node.TestCordapp
import net.corda.testing.node.User
import org.slf4j.LoggerFactory

/**
 * Allows you to run your nodes through an IDE (as opposed to using deployNodes). Do not use in a production
 * environment.
 */
val log = LoggerFactory.getLogger("NodeDriver.main")

fun main(args: Array<String>) {

    val rpcUsers = listOf(User("user1", "test", permissions = setOf("ALL")))

    driver(DriverParameters(startNodesInProcess = true,
            waitForAllNodesToFinish = true,
            cordappsForAllNodes = listOf(
                    TestCordapp.findCordapp("com.template.flows"),
                    TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts"),
                    TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows"),
                    TestCordapp.findCordapp("com.r3.corda.lib.tokens.selection").withConfig(
                            mapOf("stateSelection.inMemory.enabled" to "true",
                                    "stateSelection.inMemory.indexingStrategies" to "[\"EXTERNAL_ID\"]",
                                    "stateSelection.inMemory.cacheSize" to "1024"))
            ))) {

        val issuer = startNode(providedName = CordaX500Name("PartyA", "London", "GB"), rpcUsers = rpcUsers).getOrThrow()
        val other = startNode(providedName = CordaX500Name("PartyB", "New York", "US"), rpcUsers = rpcUsers).getOrThrow()
        val observer = startNode(providedName = CordaX500Name("Observer", "New York", "US"), rpcUsers = rpcUsers).getOrThrow()


        val issuerParty = issuer.nodeInfo.singleIdentity()
        val otherParty = other.nodeInfo.singleIdentity()
        val observerParty = observer.nodeInfo.singleIdentity()

//        val flow = partyAHandle.rpc.startFlowDynamic(ExampleFlowWithFixedToken::class.java, listOf("100", partyBIdentity)).returnValue
//        flow.toCompletableFuture()


        // ISSUER = 100, OTHER = 100
        val customToken = TokenType("CUSTOM_TOKEN", 3)
        val issuedType = IssuedTokenType(issuerParty, customToken)
        val amountToIssue = Amount(100, issuedType)
        val tokenToIssueToIssuer = FungibleToken(amountToIssue, issuerParty)
        val tokenToIssueToOther = FungibleToken(amountToIssue, otherParty)
        issuer.rpc.startFlowDynamic(IssueTokens::class.java, listOf(tokenToIssueToIssuer, tokenToIssueToOther), listOf(observer)).returnValue.getOrThrow()
        queryByPartyAndToken(issuer, customToken)

        // ISSUER = 50, OTHER = 150
        var amount = Amount(50, customToken)
        var partyAndAmount = PartyAndAmount(otherParty, amount)
        issuer.rpc.startFlowDynamic(MoveFungibleTokens::class.java, listOf(partyAndAmount)).returnValue.getOrThrow()
        queryByPartyAndToken(issuer, customToken)
        queryByPartyAndToken(other, customToken)

        // ISSUER = 175, OTHER = 25
        amount = Amount(125, customToken)
        partyAndAmount = PartyAndAmount(issuerParty, amount)
        other.rpc.startFlowDynamic(MoveFungibleTokens::class.java, listOf(partyAndAmount)).returnValue.getOrThrow()
        queryByPartyAndToken(issuer, customToken)
        queryByPartyAndToken(other, customToken)

        // ISSUER = 175, OTHER = -100
        // Exception - com.r3.corda.lib.tokens.selection.InsufficientBalanceException: Insufficient spendable states identified for 0.125 TokenType
        /*amount = Amount(125, customToken)
        partyAndAmount = PartyAndAmount(issuerParty, amount)
        other.rpc.startFlowDynamic(MoveFungibleTokens::class.java, listOf(partyAndAmount)).returnValue.getOrThrow()
        queryByPartyAndToken(other, customToken)*/

        amount = Amount(125, customToken)
        partyAndAmount = PartyAndAmount(issuerParty, amount)
        other.rpc.startFlowDynamic(MoveFungibleTokens::class.java, listOf(partyAndAmount)).returnValue.getOrThrow()
        queryByPartyAndToken(other, customToken)


    }
}

fun queryByPartyAndToken(nodeHandle: NodeHandle, tokenType: TokenType): Vault.Page<FungibleToken> {
    val queryResult = nodeHandle.rpc.vaultQueryByCriteria(heldTokenAmountCriteria(tokenType, nodeHandle.nodeInfo.singleIdentity()), FungibleToken::class.java)
    log.info("Query: $queryResult")
    return queryResult
}