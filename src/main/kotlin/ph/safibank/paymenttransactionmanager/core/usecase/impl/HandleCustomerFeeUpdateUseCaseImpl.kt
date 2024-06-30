package ph.safibank.paymenttransactionmanager.core.usecase.impl

import jakarta.inject.Singleton
import ph.safibank.paymenttransactionmanager.core.transaction.CustomerFeeUpdateResult
import ph.safibank.paymenttransactionmanager.core.usecase.HandleCustomerFeeUpdateUseCase

@Singleton
class HandleCustomerFeeUpdateUseCaseImpl : HandleCustomerFeeUpdateUseCase {
    override suspend fun invoke(customerFeeUpdate: CustomerFeeUpdateResult) {
        TODO("Not yet implemented")
    }
}
