package ph.safibank.paymenttransactionmanager.core.fraud

import ph.safibank.paymenttransactionmanager.core.transaction.Transaction

interface FraudService {
    suspend fun determineFraudStatus(transaction: Transaction): FraudStatus
}
