package ph.safibank.paymenttransactionmanager.core.usecase

import ph.safibank.paymenttransactionmanager.core.transaction.TransactionResult

interface CreateBillPaymentUseCase {
    suspend fun invoke(request: BillPaymentRequest): TransactionResult
}
