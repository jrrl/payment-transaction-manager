package ph.safibank.paymenttransactionmanager.core.usecase

import java.util.UUID

interface HandleSuccessfulPaymentUseCase {
    suspend fun invoke(transactionID: UUID)
}
