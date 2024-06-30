package ph.safibank.paymenttransactionmanager.core.usecase

import ph.safibank.paymenttransactionmanager.core.transaction.ReserveTransactionRequest

interface ReserveAmountFailedUseCase {
    suspend fun invoke(request: ReserveTransactionRequest)
}
