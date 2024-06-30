package ph.safibank.paymenttransactionmanager.core.usecase

import ph.safibank.paymenttransactionmanager.core.account.AccountNumber
import java.math.BigDecimal
import java.util.UUID

data class AirtimeLoadRequest(
    val id: UUID,
    val accountNumber: AccountNumber,
    val amount: BigDecimal,
    val currency: String,
    val product: String,
    val recipientName: String,
    val recipientMobile: String,
)
