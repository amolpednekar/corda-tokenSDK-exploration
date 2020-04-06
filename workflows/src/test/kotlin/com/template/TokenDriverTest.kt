package com.template

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.r3.corda.lib.tokens.workflows.utilities.heldTokenAmountCriteria
import net.corda.core.contracts.Amount
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.BOC_NAME
import net.corda.testing.core.DUMMY_BANK_A_NAME
import net.corda.testing.core.singleIdentity
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import net.corda.testing.driver.internal.incrementalPortAllocation
import net.corda.testing.node.NotarySpec
import net.corda.testing.node.TestCordapp
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert
import org.junit.Test

class TokenDriverTest {

    @Test
    fun `should allow issuance of inline defined token`() {
        driver(DriverParameters(
//                portAllocation = incrementalPortAllocation(),
                startNodesInProcess = false,
                cordappsForAllNodes = listOf(
                        TestCordapp.findCordapp("com.template.flows"),
                        TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts"),
                        TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows"),
                        TestCordapp.findCordapp("com.r3.corda.lib.tokens.selection")
                ),
                notarySpecs = listOf(NotarySpec(CordaX500Name("Notary","PSL","IN"), validating = false)))) {
            val (issuer, otherNode) = listOf(startNode(providedName = BOC_NAME, customOverrides = mapOf("p2pPort" to "10050")),
                    startNode(providedName = DUMMY_BANK_A_NAME, customOverrides = mapOf("p2pPort" to "10055"))).map { it.getOrThrow() }

            val issuerParty = issuer.nodeInfo.singleIdentity()
            val otherParty = otherNode.nodeInfo.singleIdentity()

            val customToken = TokenType("CUSTOM_TOKEN", 3)
            val issuedType = IssuedTokenType(issuerParty, customToken)
            val amountToIssue = Amount(100, issuedType)
            val tokenToIssueToIssuer = FungibleToken(amountToIssue, issuerParty)
            val tokenToIssueToOther = FungibleToken(amountToIssue, otherParty)

            issuer.rpc.startFlowDynamic(IssueTokens::class.java, listOf(tokenToIssueToIssuer, tokenToIssueToOther), emptyList<Party>()).returnValue.getOrThrow()
            val queryResult = issuer.rpc.vaultQueryByCriteria(heldTokenAmountCriteria(customToken, issuerParty), FungibleToken::class.java)

            Assert.assertThat(queryResult.states.size, `is`(1))
            Assert.assertThat(queryResult.states.first().state.data.holder, `is`(equalTo((issuerParty as AbstractParty))))

        }
    }
}