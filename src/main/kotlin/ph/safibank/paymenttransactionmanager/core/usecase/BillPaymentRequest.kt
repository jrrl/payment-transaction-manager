package ph.safibank.paymenttransactionmanager.core.usecase

import ph.safibank.paymenttransactionmanager.core.account.AccountNumber
import java.math.BigDecimal
import java.util.UUID

data class BillPaymentRequest(
    val id: UUID,
    val accountNumber: AccountNumber,
    val amount: BigDecimal,
    val currency: String,
    val billerCode: String,
)
