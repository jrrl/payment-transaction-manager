package ph.safibank.paymenttransactionmanager.core.merchant

import java.math.BigDecimal

interface MerchantService {
    suspend fun getMerchantDetails(merchantCode: String): MerchantDetails?
    suspend fun getProduct(product: String): ProductDetails?
}

data class MerchantDetails(
    val name: String,
    val code: String,
    val minimumLimit: BigDecimal,
    val maximumLimit: BigDecimal? = null,
)

data class ProductDetails(
    val name: String,
    val code: String,
    val sellingPrice: BigDecimal,
    val provideName: String
)
