package ph.safibank.paymenttransactionmanager.core.exception

import ph.safibank.paymenttransactionmanager.core.fraud.FraudStatus

data class StepUpException(val stepUpLevel: FraudStatus) : Exception("Step-up error: $stepUpLevel")
