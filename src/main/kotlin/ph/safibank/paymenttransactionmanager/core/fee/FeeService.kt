package ph.safibank.paymenttransactionmanager.core.fee

import ph.safibank.paymenttransactionmanager.core.transaction.Transaction

interface FeeService {
    suspend fun calculateCustomerFee(transaction: Transaction): CustomerFee
    suspend fun calculateVendorFee(transaction: Transaction): VendorFee
    suspend fun revertCustomerFee(customerFee: CustomerFee)
}
