package ph.safibank.paymenttransactionmanager.core.usecase

import ph.safibank.paymenttransactionmanager.core.transaction.TransactionResult

interface CreateAirtimeLoadUseCase {
    suspend fun invoke(request: AirtimeLoadRequest): TransactionResult?
}
