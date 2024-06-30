package ph.safibank.paymenttransactionmanager.core.fee

import java.math.BigDecimal

data class VendorFee(
    val amount: BigDecimal,
    val currency: String,
    val postingId: String? = null,
    val id: String? = null,
) {
    companion object {
        val ZERO_PHP = VendorFee(
            amount = BigDecimal.ZERO,
            currency = "PHP",
        )
    }

    operator fun plus(other: VendorFee) = VendorFee(
        amount = amount.plus(other.amount),
        currency = currency,
    )
}
