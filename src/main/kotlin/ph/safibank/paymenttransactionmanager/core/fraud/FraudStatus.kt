package ph.safibank.paymenttransactionmanager.core.fraud

enum class FraudStatus {
    UNKNOWN,
    REJECTED,
    APPROVED,
    STEP_UP_LEVEL1,
    STEP_UP_LEVEL2,
    STEP_UP_LEVEL3,
    STEP_UP_LEVEL4,
    MANUAL_APPROVAL,
}
