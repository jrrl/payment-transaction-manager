package ph.safibank.paymenttransactionmanager.core.account

interface AccountService {
    suspend fun getAccountDetails(accountNumber: AccountNumber): AccountDetails?
}
