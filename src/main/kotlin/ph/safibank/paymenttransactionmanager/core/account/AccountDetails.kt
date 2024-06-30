package ph.safibank.paymenttransactionmanager.core.account

import ph.safibank.paymenttransactionmanager.core.exception.ValidationError
import ph.safibank.paymenttransactionmanager.core.exception.ValidationException
import java.math.BigDecimal
import java.util.UUID

data class AccountDetails(
    val id: UUID,
    val customerId: UUID,
    val accountNumber: AccountNumber,
    val active: Boolean,
    val balance: BigDecimal,
    val currency: String,
)

@JvmInline
value class AccountNumber private constructor(val value: String) {
    companion object {
        private val ACCOUNT_NUMBER_REGEX = Regex("""^\d{10}$""")

        operator fun invoke(value: String): AccountNumber {
            if (ACCOUNT_NUMBER_REGEX.matches(value)) {
                return AccountNumber(value)
            }
            throw ValidationException(
                listOf(ValidationError("Illegal account number format"))
            )
        }
    }
}
