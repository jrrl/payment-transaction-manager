package ph.safibank.paymenttransactionmanager.core.exception

data class ValidationException(val errors: List<ValidationError>) : Exception() {
    companion object {
        fun singleError(message: String) = ValidationException(listOf(ValidationError(message)))
    }
}

@JvmInline
value class ValidationError(val value: String)
