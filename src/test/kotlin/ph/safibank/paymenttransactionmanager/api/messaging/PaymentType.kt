package ph.safibank.paymenttransactionmanager.api.messaging

import ph.safibank.tm.model.transaction.BillPaymentOutboundAuthorisationECPay
import ph.safibank.tm.model.transaction.BillPaymentOutboundAuthorisationECPayRelease
import ph.safibank.tm.model.transaction.BillPaymentOutboundAuthorisationECPaySettlement
import ph.safibank.tm.model.transaction.BillPaymentOutboundAuthorisationPaynamics
import ph.safibank.tm.model.transaction.BillPaymentOutboundAuthorisationPaynamicsRelease
import ph.safibank.tm.model.transaction.BillPaymentOutboundAuthorisationPaynamicsSettlement
import ph.safibank.tm.model.transaction.DigitalGoodsOutboundAuthorisationPaynamics
import ph.safibank.tm.model.transaction.DigitalGoodsOutboundAuthorisationPaynamicsRelease
import ph.safibank.tm.model.transaction.DigitalGoodsOutboundAuthorisationPaynamicsSettlement
import kotlin.reflect.KClass

enum class PaymentType(
    val settlementClass: KClass<*>,
    val settlementListenerClass: KClass<*>,
    val releaseClass: KClass<*>,
    val releaseListenerClass: KClass<*>,
    val reserveClass: KClass<*>,
    val reserveListenerClass: KClass<*>,
) {
    BILL_PAYMENT_ECPAY(
        BillPaymentOutboundAuthorisationECPaySettlement::class,
        BillPaymentOutboundAuthorisationECPaySettlementListener::class,
        BillPaymentOutboundAuthorisationECPayRelease::class,
        BillPaymentOutboundAuthorisationECPayReleaseListener::class,
        BillPaymentOutboundAuthorisationECPay::class,
        BillPaymentOutboundAuthorisationECPayListener::class,
    ),
    BILL_PAYMENT_PAYNAMICS(
        BillPaymentOutboundAuthorisationPaynamicsSettlement::class,
        BillPaymentOutboundAuthorisationPaynamicsSettlementListener::class,
        BillPaymentOutboundAuthorisationPaynamicsRelease::class,
        BillPaymentOutboundAuthorisationPaynamicsReleaseListener::class,
        BillPaymentOutboundAuthorisationPaynamics::class,
        BillPaymentOutboundAuthorisationPaynamicsListener::class,
    ),
    DIGITAL_GOODS(
        DigitalGoodsOutboundAuthorisationPaynamicsSettlement::class,
        DigitalGoodsOutboundAuthorisationPaynamicsSettlementListener::class,
        DigitalGoodsOutboundAuthorisationPaynamicsRelease::class,
        DigitalGoodsOutboundAuthorisationPaynamicsReleaseListener::class,
        DigitalGoodsOutboundAuthorisationPaynamics::class,
        DigitalGoodsOutboundAuthorisationPaynamicsListener::class,
    ),
}
