package ph.safibank.paymenttransactionmanager.core.exception

class InvalidProviderException(
    private val code: String,
    message: String = "Invalid provider found for $code"
) : Exception(message)
