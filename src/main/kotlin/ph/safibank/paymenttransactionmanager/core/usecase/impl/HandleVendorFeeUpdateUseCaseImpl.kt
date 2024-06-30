package ph.safibank.paymenttransactionmanager.core.usecase.impl

import jakarta.inject.Singleton
import ph.safibank.paymenttransactionmanager.core.transaction.VendorFeeUpdateResult
import ph.safibank.paymenttransactionmanager.core.usecase.HandleVendorFeeUpdateUseCase

@Singleton
class HandleVendorFeeUpdateUseCaseImpl : HandleVendorFeeUpdateUseCase {
    override suspend fun invoke(vendorFeeUpdate: VendorFeeUpdateResult) {
        TODO("Not yet implemented")
    }
}
