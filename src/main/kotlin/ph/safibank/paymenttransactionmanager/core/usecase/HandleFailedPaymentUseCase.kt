package ph.safibank.paymenttransactionmanager.core.usecase

import java.util.UUID

interface HandleFailedPaymentUseCase {
    suspend fun invoke(transactionID: UUID)
}
