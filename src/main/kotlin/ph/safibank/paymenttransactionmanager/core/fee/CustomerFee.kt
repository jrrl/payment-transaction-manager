package ph.safibank.paymenttransactionmanager.core.fee

import java.math.BigDecimal
import java.util.UUID

data class CustomerFee(
    val amount: BigDecimal,
    val currency: String,
    val subscriptionId: UUID? = null,
    val postingId: String? = null,
    val id: String? = null,
) {
    companion object {
        val ZERO_PHP = CustomerFee(
            amount = BigDecimal.ZERO,
            currency = "PHP"
        )
    }

    operator fun plus(other: CustomerFee) = CustomerFee(
        amount = amount.plus(other.amount),
        currency = currency,
        subscriptionId = null,
    )
}
