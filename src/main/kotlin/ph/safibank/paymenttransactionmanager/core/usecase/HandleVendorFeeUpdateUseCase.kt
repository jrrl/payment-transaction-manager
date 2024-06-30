package ph.safibank.paymenttransactionmanager.core.usecase

import ph.safibank.paymenttransactionmanager.core.transaction.VendorFeeUpdateResult

interface HandleVendorFeeUpdateUseCase {
    suspend fun invoke(vendorFeeUpdate: VendorFeeUpdateResult)
}
