package ph.safibank.paymenttransactionmanager.core.usecase

import ph.safibank.paymenttransactionmanager.core.transaction.ReserveTransactionRequest

interface ReserveAmountSuccessUseCase {
    suspend fun invoke(request: ReserveTransactionRequest)
}
