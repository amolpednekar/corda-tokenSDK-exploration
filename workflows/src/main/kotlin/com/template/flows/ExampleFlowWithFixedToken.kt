package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import net.corda.core.contracts.Amount
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker


@StartableByRPC
class ExampleFlowWithFixedToken(val amount: Long, val recipient: Party) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        /*
        (val currency: String, val amount: Long, val recipient: Party)
           val token = FiatCurrency.getInstance(currency)
        // Starts a new flow session.
        return subFlow(IssueTokens(listOf(amount of token issuedBy ourIdentity heldBy recipient)))
         */

        /* Create an instance of the NDC token */
        val ndcToken = TokenType("NDC",0)

        /* Create an instance of IssuedTokenType for the NDC */
        val issuedTokenType = IssuedTokenType(ourIdentity, ndcToken)

        /* Create an instance of FungibleToken for the NDC to be issued */
        val fungibleToken = FungibleToken(Amount(amount, issuedTokenType), recipient)

        // Starts a new flow session.
        return subFlow(IssueTokens(listOf(fungibleToken)))
    }
}
