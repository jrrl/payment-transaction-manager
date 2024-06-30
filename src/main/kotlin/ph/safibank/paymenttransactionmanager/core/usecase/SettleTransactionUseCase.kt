package ph.safibank.paymenttransactionmanager.core.usecase

import ph.safibank.paymenttransactionmanager.core.transaction.SettleTransactionResult

interface SettleTransactionUseCase {
    suspend fun invoke(result: SettleTransactionResult)
}
