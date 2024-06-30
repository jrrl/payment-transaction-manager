package ph.safibank.paymenttransactionmanager.core.usecase

import ph.safibank.paymenttransactionmanager.core.transaction.CustomerFeeUpdateResult

interface HandleCustomerFeeUpdateUseCase {
    suspend fun invoke(customerFeeUpdate: CustomerFeeUpdateResult)
}
