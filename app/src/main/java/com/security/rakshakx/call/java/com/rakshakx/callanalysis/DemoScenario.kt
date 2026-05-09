package com.rakshakx.callanalysis

import com.rakshakx.R

enum class DemoRiskLevel { LOW, MEDIUM, HIGH_EN, HIGH_MULTI }

data class DemoScenario(
    val id: DemoRiskLevel,
    val title: String,
    val rawResId: Int
)

val demoScenarios = listOf(
    DemoScenario(DemoRiskLevel.LOW,       "Scenario 1 (Low)",        R.raw.scenario1_low_safe),
    DemoScenario(DemoRiskLevel.MEDIUM,    "Scenario 2 (Medium)",     R.raw.scenario2_medium),
    DemoScenario(DemoRiskLevel.HIGH_EN,   "Scenario 3 (High EN)",    R.raw.scenario3_high_en),
    DemoScenario(DemoRiskLevel.HIGH_MULTI,"Scenario 4 (High Multi)", R.raw.scenario4_high_multi)
)
