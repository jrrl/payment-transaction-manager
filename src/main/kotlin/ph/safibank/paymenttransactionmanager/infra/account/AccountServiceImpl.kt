package ph.safibank.paymenttransactionmanager.infra.account

import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ph.safibank.accountmanager.client.api.MainAccountApi
import ph.safibank.accountmanager.client.model.AccountStateV2Dto
import ph.safibank.paymenttransactionmanager.core.account.AccountDetails
import ph.safibank.paymenttransactionmanager.core.account.AccountNumber
import ph.safibank.paymenttransactionmanager.core.account.AccountService

@Singleton
class AccountServiceImpl(
    private val accountApi: MainAccountApi,
) : AccountService {
    override suspend fun getAccountDetails(accountNumber: AccountNumber): AccountDetails? = withContext(Dispatchers.IO) {
        val result = accountApi.getMainAccountByAccountNumber(accountNumber.value)
        if (result != null && result.mainAccount != null) {
            AccountDetails(
                id = result.id,
                customerId = result.ownerId,
                balance = result.mainAccount!!.balances.available.amount,
                active = result.accountState == AccountStateV2Dto.ACTIVE,
                accountNumber = AccountNumber(result.mainAccount!!.accountNumber),
                currency = result.mainAccount!!.balances.available.currency,
            )
        } else null
    }
}
