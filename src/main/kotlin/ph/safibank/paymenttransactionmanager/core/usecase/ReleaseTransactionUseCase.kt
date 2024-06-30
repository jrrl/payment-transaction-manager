package ph.safibank.paymenttransactionmanager.core.usecase

import ph.safibank.paymenttransactionmanager.core.transaction.ReleaseTransactionResult

interface ReleaseTransactionUseCase {
    suspend fun invoke(result: ReleaseTransactionResult)
}
