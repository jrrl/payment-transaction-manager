package ph.safibank.paymenttransactionmanager.infra.fee

import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ph.safibank.common.utils.GlobalUtils.getLogger
import ph.safibank.feemanager.client.api.FeeManagerApi
import ph.safibank.feemanager.client.model.FeeCalculationRequestDto
import ph.safibank.feemanager.client.model.TransactionTypeDto
import ph.safibank.paymenttransactionmanager.core.fee.CustomerFee
import ph.safibank.paymenttransactionmanager.core.fee.FeeService
import ph.safibank.paymenttransactionmanager.core.fee.VendorFee
import ph.safibank.paymenttransactionmanager.core.transaction.Transaction
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionType
import ph.safibank.productmanager.client.api.SubscriptionOperationUsageApi
import ph.safibank.productmanager.client.model.RevertSubscriptionOperationDto
import ph.safibank.productmanager.client.model.SubscriptionOperationType
import ph.safibank.productmanager.client.model.SubscriptionOperationUsageRequestDtoV2
import java.util.UUID

@Singleton
class FeeServiceImpl(
    private val subscriptionApi: SubscriptionOperationUsageApi,
    private val feeManagerApi: FeeManagerApi,
) : FeeService {
    private val log = getLogger()

    override suspend fun calculateCustomerFee(transaction: Transaction): CustomerFee = withContext(Dispatchers.IO) {
        log.info("Calculating customer fee for transaction {}", transaction.id)
        val request = SubscriptionOperationUsageRequestDtoV2(
            transaction.senderDetails.customerId,
            SubscriptionOperationType.TRANSFER,
            1,
            transaction.amount,
            transaction.currency,
        )

        val result = subscriptionApi.commitV4(UUID.randomUUID(), request) // TODO update idempotency
        log.info("Customer fee: $result")
        CustomerFee(
            amount = result.fee,
            currency = result.currency,
            subscriptionId = result.id,
        )
    }

    override suspend fun calculateVendorFee(transaction: Transaction): VendorFee = withContext(Dispatchers.IO) {
        log.info("Calculating vendor fee for transaction ${transaction.id}")

        val request = FeeCalculationRequestDto(
            transaction.amount,
            transaction.currency,
            transaction.senderDetails.customerId,
            convertToTransactionTypeDto(transaction.type),
            transaction.providerDetails.merchantCode,
        )

        val result = feeManagerApi.calculateVendorFees(request)
        log.info("Vendor fee: $result")
        VendorFee(
            amount = result.fee,
            currency = result.feeCurrency,
        )
    }

    override suspend fun revertCustomerFee(customerFee: CustomerFee) {
        log.info("Reverting customerFee with subscription ID ${customerFee.subscriptionId}")
        val subscriptionId = customerFee.subscriptionId
        revertFee(subscriptionId?.let { RevertSubscriptionOperationDto(subscriptionId) })
    }

    private suspend fun revertFee(revertSubscriptionOperation: RevertSubscriptionOperationDto?): Unit = withContext(Dispatchers.IO) {
        launch {
            if (revertSubscriptionOperation != null) {
                subscriptionApi.revertV3(UUID.randomUUID(), revertSubscriptionOperation)
            }
        }
    }

    private fun convertToTransactionTypeDto(type: TransactionType): TransactionTypeDto {
        return when (type) {
            TransactionType.BILL_PAYMENT -> TransactionTypeDto.BILL_PAYMENT
            TransactionType.AIRTIME_LOAD -> TransactionTypeDto.AIRTIME_LOAD
        }
    }
}
