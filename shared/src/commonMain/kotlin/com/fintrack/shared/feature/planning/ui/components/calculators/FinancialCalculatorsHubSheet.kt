package com.fintrack.shared.feature.planning.ui.components.calculators

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrack.shared.feature.core.ui.SelectableFilterChip
import com.fintrack.shared.feature.navigation.ui.LocalCurrency
import com.fintrack.shared.feature.navigation.ui.toCurrencyString
import com.fintrack.shared.feature.planning.domain.calculator.CalculatorEngine
import com.fintrack.shared.feature.planning.domain.calculator.DebtStrategyType
import com.fintrack.shared.feature.planning.domain.calculator.FinancialCalculatorType
import com.fintrack.shared.feature.planning.domain.calculator.FinancialCalculatorsState
import com.fintrack.shared.ui.theme.GreenIncome

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialCalculatorsHubSheet(
    state: FinancialCalculatorsState = FinancialCalculatorsState(),
    onUpdateState: ((FinancialCalculatorsState) -> FinancialCalculatorsState) -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tabLazyListState = rememberLazyListState()

    val activeIndex = remember(state.activeCalculator) {
        FinancialCalculatorType.entries.indexOf(state.activeCalculator).coerceAtLeast(0)
    }

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            tabLazyListState.animateScrollToItem(activeIndex)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Financial Calculators",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Interactive MMF, FIRE, Mortgage & Savings Tools",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Close")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

            // Scrollable Tab Control Panel Container
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyRow(
                    state = tabLazyListState,
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(FinancialCalculatorType.entries) { type ->
                        val isSelected = state.activeCalculator == type
                        val label = when (type) {
                            FinancialCalculatorType.MMF_COMPOUND -> "📈 MMF Compound"
                            FinancialCalculatorType.SAVINGS_RATE_FREEDOM -> "🎯 Savings & Freedom"
                            FinancialCalculatorType.DEBT_ACCELERATOR -> "⚡ Debt Accelerator"
                            FinancialCalculatorType.BUY_VS_RENT -> "🏠 Buy vs Rent"
                            FinancialCalculatorType.COAST_FIRE -> "🌴 Coast FIRE"
                            FinancialCalculatorType.MORTGAGE_SAVER -> "🏠 Mortgage Saver"
                            FinancialCalculatorType.EDUCATION_FUND -> "🎓 College Fund"
                            FinancialCalculatorType.CAR_OWNERSHIP -> "🚗 Car Ownership"
                            FinancialCalculatorType.TIP_SPLITTER -> "🍽️ Tip & Split"
                            FinancialCalculatorType.FIRE_RETIREMENT -> "🔥 FIRE Freedom"
                        }

                        Surface(
                            onClick = { onUpdateState { it.copy(activeCalculator = type) } },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Selected Calculator Form & Results
            when (state.activeCalculator) {
                FinancialCalculatorType.MMF_COMPOUND -> MmfCalculatorView(state, onUpdateState)
                FinancialCalculatorType.SAVINGS_RATE_FREEDOM -> SavingsRateFreedomCalculatorView(state, onUpdateState)
                FinancialCalculatorType.DEBT_ACCELERATOR -> DebtAcceleratorCalculatorView(state, onUpdateState)
                FinancialCalculatorType.BUY_VS_RENT -> BuyVsRentCalculatorView(state, onUpdateState)
                FinancialCalculatorType.COAST_FIRE -> CoastFireCalculatorView(state, onUpdateState)
                FinancialCalculatorType.MORTGAGE_SAVER -> MortgageSaverCalculatorView(state, onUpdateState)
                FinancialCalculatorType.EDUCATION_FUND -> EducationFundCalculatorView(state, onUpdateState)
                FinancialCalculatorType.CAR_OWNERSHIP -> CarOwnershipCalculatorView(state, onUpdateState)
                FinancialCalculatorType.TIP_SPLITTER -> TipSplitterCalculatorView(state, onUpdateState)
                FinancialCalculatorType.FIRE_RETIREMENT -> FireCalculatorView(state, onUpdateState)
            }
        }
    }
}

@Composable
private fun SavingsRateFreedomCalculatorView(
    state: FinancialCalculatorsState,
    onUpdateState: ((FinancialCalculatorsState) -> FinancialCalculatorsState) -> Unit
) {
    val currencySymbol = LocalCurrency.current.symbol

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.savingsRateIncome,
                onValueChange = { newValue -> onUpdateState { it.copy(savingsRateIncome = newValue) } },
                label = { Text("Monthly Net Income ($currencySymbol)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = state.savingsRateExpense,
                onValueChange = { newValue -> onUpdateState { it.copy(savingsRateExpense = newValue) } },
                label = { Text("Monthly Expenses ($currencySymbol)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        val income = state.savingsRateIncome.toDoubleOrNull() ?: 0.0
        val expense = state.savingsRateExpense.toDoubleOrNull() ?: 0.0

        val result = CalculatorEngine.calculateSavingsRateFreedom(income, expense)

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Your Savings Rate %", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        Text("${result.savingsRatePercentage}%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = GreenIncome)
                    }

                    if (result.estimatedYearsToFreedom != null) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Years to Complete FI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("~${result.estimatedYearsToFreedom} Yrs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Monthly Savings: ${result.monthlySavings.toCurrencyString()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text("Target FIRE Number: ${result.targetFireNumber.toCurrencyString()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun CoastFireCalculatorView(
    state: FinancialCalculatorsState,
    onUpdateState: ((FinancialCalculatorsState) -> FinancialCalculatorsState) -> Unit
) {
    val currencySymbol = LocalCurrency.current.symbol

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
                Text(
                    text = "What is Coast FIRE?: It means you have saved enough early in life that compound interest will grow it to your full retirement goal on autopilot. You can stop saving for retirement and spend 100% of your income today!",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.coastFireAge,
                onValueChange = { newValue -> onUpdateState { it.copy(coastFireAge = newValue) } },
                label = { Text("Current Age") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = state.coastFireRetireAge,
                onValueChange = { newValue -> onUpdateState { it.copy(coastFireRetireAge = newValue) } },
                label = { Text("Retire Age") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        OutlinedTextField(
            value = state.coastFireCurrentInvestments,
            onValueChange = { newValue -> onUpdateState { it.copy(coastFireCurrentInvestments = newValue) } },
            label = { Text("Current Investments Portfolio ($currencySymbol)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = state.coastFireAnnualExpenses,
            onValueChange = { newValue -> onUpdateState { it.copy(coastFireAnnualExpenses = newValue) } },
            label = { Text("Annual Expenses in Retirement ($currencySymbol)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        val age = state.coastFireAge.toIntOrNull() ?: 30
        val retireAge = state.coastFireRetireAge.toIntOrNull() ?: 60
        val currentInv = state.coastFireCurrentInvestments.toDoubleOrNull() ?: 0.0
        val expenses = state.coastFireAnnualExpenses.toDoubleOrNull() ?: 0.0

        val result = CalculatorEngine.calculateCoastFire(age, retireAge, currentInv, expenses)

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (result.isCoastFireReached) "🌴 Coast FIRE Achieved!" else "Coast FIRE Target Today",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )

                Text(
                    text = if (result.isCoastFireReached) "Autopilot Ready!" else result.coastFireTargetNumberToday.toCurrencyString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = GreenIncome
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

                Text(
                    text = if (result.isCoastFireReached) {
                        "Your current ${result.currentInvestments.toCurrencyString()} portfolio will compound to ${result.projectedBalanceAtRetirement.toCurrencyString()} by age $retireAge at 8% p.a. without adding a single extra shilling! You can spend 100% of your income."
                    } else {
                        "Saving ${result.monthlySavingsNeededToCoast.toCurrencyString()}/mo for the next ${retireAge - age} years will reach Coast FIRE autopilot readiness."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun MortgageSaverCalculatorView(
    state: FinancialCalculatorsState,
    onUpdateState: ((FinancialCalculatorsState) -> FinancialCalculatorsState) -> Unit
) {
    val currencySymbol = LocalCurrency.current.symbol

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = state.mortgagePrincipal,
            onValueChange = { newValue -> onUpdateState { it.copy(mortgagePrincipal = newValue) } },
            label = { Text("Mortgage Principal ($currencySymbol)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.mortgageRate,
                onValueChange = { newValue -> onUpdateState { it.copy(mortgageRate = newValue) } },
                label = { Text("APR %") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(0.8f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = state.mortgageTenureYears,
                onValueChange = { newValue -> onUpdateState { it.copy(mortgageTenureYears = newValue) } },
                label = { Text("Tenure (Yrs)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(0.8f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = state.mortgageExtraPrincipal,
                onValueChange = { newValue -> onUpdateState { it.copy(mortgageExtraPrincipal = newValue) } },
                label = { Text("+Extra Principal/mo") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1.2f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        val principal = state.mortgagePrincipal.toDoubleOrNull() ?: 0.0
        val rate = state.mortgageRate.toDoubleOrNull() ?: 0.0
        val tenure = state.mortgageTenureYears.toIntOrNull() ?: 20
        val extra = state.mortgageExtraPrincipal.toDoubleOrNull() ?: 0.0

        val result = CalculatorEngine.calculateMortgageSaver(principal, rate, tenure, extra)

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Extra Principal Mortgage Interest Savings", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                Text("+${result.totalInterestSaved.toCurrencyString()} Saved", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = GreenIncome)

                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Accelerated Payoff Tenure", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${result.acceleratedTenureMonths / 12} yrs ${result.acceleratedTenureMonths % 12} mo (Knocks off ${(result.monthsSaved / 12)} yrs!)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GreenIncome)
                    }
                }
            }
        }
    }
}

@Composable
private fun EducationFundCalculatorView(
    state: FinancialCalculatorsState,
    onUpdateState: ((FinancialCalculatorsState) -> FinancialCalculatorsState) -> Unit
) {
    val currencySymbol = LocalCurrency.current.symbol

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.educationChildAge,
                onValueChange = { newValue -> onUpdateState { it.copy(educationChildAge = newValue) } },
                label = { Text("Child Current Age") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = state.educationAnnualTuition,
                onValueChange = { newValue -> onUpdateState { it.copy(educationAnnualTuition = newValue) } },
                label = { Text("Today Annual Tuition ($currencySymbol)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1.5f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        val age = state.educationChildAge.toIntOrNull() ?: 2
        val tuition = state.educationAnnualTuition.toDoubleOrNull() ?: 300000.0

        val result = CalculatorEngine.calculateEducationFund(age, currentAnnualTuition = tuition)

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Required Monthly MMF Deposit Today", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                Text("${result.requiredMonthlyMmfDeposit.toCurrencyString()}/mo", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Years Until College (Age 18)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${result.yearsUntilCollege} Years", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Inflated 4-Yr College Cost (@8% inflation)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(result.total4YearFutureTuitionCost.toCurrencyString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GreenIncome)
                    }
                }
            }
        }
    }
}

@Composable
private fun MmfCalculatorView(
    state: FinancialCalculatorsState,
    onUpdateState: ((FinancialCalculatorsState) -> FinancialCalculatorsState) -> Unit
) {
    val currencySymbol = LocalCurrency.current.symbol

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = state.mmfInitialDeposit,
            onValueChange = { newValue -> onUpdateState { it.copy(mmfInitialDeposit = newValue) } },
            label = { Text("Initial Deposit ($currencySymbol)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.mmfMonthlyContribution,
                onValueChange = { newValue -> onUpdateState { it.copy(mmfMonthlyContribution = newValue) } },
                label = { Text("Monthly Deposit") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = state.mmfAnnualRate,
                onValueChange = { newValue -> onUpdateState { it.copy(mmfAnnualRate = newValue) } },
                label = { Text("MMF Rate % p.a.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = state.mmfYears,
                onValueChange = { newValue -> onUpdateState { it.copy(mmfYears = newValue) } },
                label = { Text("Years") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(0.8f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        val initial = state.mmfInitialDeposit.toDoubleOrNull() ?: 0.0
        val monthly = state.mmfMonthlyContribution.toDoubleOrNull() ?: 0.0
        val rate = state.mmfAnnualRate.toDoubleOrNull() ?: 0.0
        val years = state.mmfYears.toIntOrNull() ?: 1

        val result = CalculatorEngine.calculateMmfYield(initial, monthly, rate, years)

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Projected Portfolio After $years Years",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )

                Text(
                    text = result.finalMmfBalance.toCurrencyString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = GreenIncome
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Principal Invested", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(result.totalInvested.toCurrencyString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Net Compound Interest", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("+${result.netInterestEarned.toCurrencyString()}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GreenIncome)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Monthly Net Passive Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("+${result.monthlyPassiveYieldNet.toCurrencyString()}/mo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GreenIncome)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("15% Withholding Tax", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("-${result.withholdingTax.toCurrencyString()}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun CarOwnershipCalculatorView(
    state: FinancialCalculatorsState,
    onUpdateState: ((FinancialCalculatorsState) -> FinancialCalculatorsState) -> Unit
) {
    val currencySymbol = LocalCurrency.current.symbol

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = state.carPrice,
            onValueChange = { newValue -> onUpdateState { it.copy(carPrice = newValue) } },
            label = { Text("Car Purchase Price ($currencySymbol)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.carRunningCost,
                onValueChange = { newValue -> onUpdateState { it.copy(carRunningCost = newValue) } },
                label = { Text("Fuel & Maint/mo") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = state.carInsurance,
                onValueChange = { newValue -> onUpdateState { it.copy(carInsurance = newValue) } },
                label = { Text("Insurance/yr") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = state.carYears,
                onValueChange = { newValue -> onUpdateState { it.copy(carYears = newValue) } },
                label = { Text("Years") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(0.8f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        val price = state.carPrice.toDoubleOrNull() ?: 0.0
        val running = state.carRunningCost.toDoubleOrNull() ?: 0.0
        val insurance = state.carInsurance.toDoubleOrNull() ?: 0.0
        val years = state.carYears.toIntOrNull() ?: 3

        val result = CalculatorEngine.calculateCarOwnership(price, running, insurance, years)

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Effective Monthly Ownership Cost", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                Text("${result.effectiveMonthlyCost.toCurrencyString()}/mo", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("$years-Yr Resale Value", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(result.estimatedResaleValue.toCurrencyString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GreenIncome)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Depreciation Loss", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("-${result.totalDepreciationLoss.toCurrencyString()}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun DebtAcceleratorCalculatorView(
    state: FinancialCalculatorsState,
    onUpdateState: ((FinancialCalculatorsState) -> FinancialCalculatorsState) -> Unit
) {
    val currencySymbol = LocalCurrency.current.symbol

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Payoff Strategy",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectableFilterChip(
                    selected = state.debtStrategy == DebtStrategyType.AVALANCHE_INTEREST,
                    label = "Avalanche (Highest Interest First)",
                    onClick = { onUpdateState { it.copy(debtStrategy = DebtStrategyType.AVALANCHE_INTEREST) } },
                    modifier = Modifier.weight(1f)
                )

                SelectableFilterChip(
                    selected = state.debtStrategy == DebtStrategyType.SNOWBALL_BALANCE,
                    label = "Snowball (Smallest Balance First)",
                    onClick = { onUpdateState { it.copy(debtStrategy = DebtStrategyType.SNOWBALL_BALANCE) } },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        OutlinedTextField(
            value = state.debtPrincipal,
            onValueChange = { newValue -> onUpdateState { it.copy(debtPrincipal = newValue) } },
            label = { Text("Total Debt Principal ($currencySymbol)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.debtRate,
                onValueChange = { newValue -> onUpdateState { it.copy(debtRate = newValue) } },
                label = { Text("APR %") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(0.8f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = state.debtMinPayment,
                onValueChange = { newValue -> onUpdateState { it.copy(debtMinPayment = newValue) } },
                label = { Text("Min Pay/mo") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = state.debtExtraBoost,
                onValueChange = { newValue -> onUpdateState { it.copy(debtExtraBoost = newValue) } },
                label = { Text("+Extra Boost") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        val principal = state.debtPrincipal.toDoubleOrNull() ?: 0.0
        val rate = state.debtRate.toDoubleOrNull() ?: 0.0
        val minPay = state.debtMinPayment.toDoubleOrNull() ?: 0.0
        val boost = state.debtExtraBoost.toDoubleOrNull() ?: 0.0

        val result = CalculatorEngine.calculateDebtAccelerator(principal, rate, minPay, boost, state.debtStrategy)

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Accelerated Payoff Savings", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                Text("+${result.totalInterestSaved.toCurrencyString()} Saved", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = GreenIncome)

                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Payoff Duration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${result.monthsWithBoost} mo (cut by ${result.monthsSaved} mo!)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GreenIncome)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Original Duration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${result.monthsWithoutBoost} mo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }

                if (boost == 0.0) {
                    Text(
                        text = "💡 Tip: Enter an extra monthly payment boost above your minimum payment (e.g. +$currencySymbol 5,000/mo) to calculate how many months and interest you save by accelerating your debt payoff!",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                } else if (state.debtStrategy == DebtStrategyType.AVALANCHE_INTEREST && result.avalancheSavingsVsSnowball > 0) {
                    Text(
                        text = "🌋 Avalanche Advantage: Tackling highest interest rates first saves an additional ${result.avalancheSavingsVsSnowball.toCurrencyString()} in interest over Snowball!",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else if (state.debtStrategy == DebtStrategyType.SNOWBALL_BALANCE) {
                    Text(
                        text = "❄️ Snowball Advantage: Ticking off smallest debt balances first gives rapid psychological momentum and quick wins!",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun BuyVsRentCalculatorView(
    state: FinancialCalculatorsState,
    onUpdateState: ((FinancialCalculatorsState) -> FinancialCalculatorsState) -> Unit
) {
    val currencySymbol = LocalCurrency.current.symbol

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = state.buyVsRentPrice,
            onValueChange = { newValue -> onUpdateState { it.copy(buyVsRentPrice = newValue) } },
            label = { Text("Property Buy/Build Price ($currencySymbol)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.buyVsRentRent,
                onValueChange = { newValue -> onUpdateState { it.copy(buyVsRentRent = newValue) } },
                label = { Text("Monthly Rent ($currencySymbol)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1.2f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = state.buyVsRentYears,
                onValueChange = { newValue -> onUpdateState { it.copy(buyVsRentYears = newValue) } },
                label = { Text("Years") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(0.8f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        val price = state.buyVsRentPrice.toDoubleOrNull() ?: 0.0
        val rent = state.buyVsRentRent.toDoubleOrNull() ?: 0.0
        val years = state.buyVsRentYears.toIntOrNull() ?: 10

        val result = CalculatorEngine.calculateBuyVsRent(price, rent, years)

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("$years-Year Net Worth Projection Comparison", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Property Value (Option A)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(result.propertyValueFuture.toCurrencyString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GreenIncome)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Rent + Invest MMF (Option B)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(result.rentAndInvestNetWorthOutcome.toCurrencyString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

                Text(
                    text = if (result.isBuyingFavorable) {
                        "Buying/Building creates ${result.netWorthDifference.toCurrencyString()} MORE net worth than Renting + MMF investing over $years years."
                    } else {
                        "Renting + MMF Investing creates ${result.netWorthDifference.toCurrencyString()} MORE net worth than buying property over $years years."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun TipSplitterCalculatorView(
    state: FinancialCalculatorsState,
    onUpdateState: ((FinancialCalculatorsState) -> FinancialCalculatorsState) -> Unit
) {
    val currencySymbol = LocalCurrency.current.symbol

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = state.tipBillAmount,
            onValueChange = { newValue -> onUpdateState { it.copy(tipBillAmount = newValue) } },
            label = { Text("Total Bill Amount ($currencySymbol)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.tipPercentage,
                onValueChange = { newValue -> onUpdateState { it.copy(tipPercentage = newValue) } },
                label = { Text("Tip %") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = state.tipPeopleSplit,
                onValueChange = { newValue -> onUpdateState { it.copy(tipPeopleSplit = newValue) } },
                label = { Text("People Split") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        val bill = state.tipBillAmount.toDoubleOrNull() ?: 0.0
        val tipPct = state.tipPercentage.toDoubleOrNull() ?: 0.0
        val people = state.tipPeopleSplit.toIntOrNull() ?: 1

        val result = CalculatorEngine.calculateTipAndSplit(bill, tipPct, people)

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Share Per Person (${result.numberOfPeople} People)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(result.perPersonShare.toCurrencyString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Bill + Tip", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(result.totalBillWithTip.toCurrencyString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Original Bill: ${result.billAmount.toCurrencyString()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Tip (${result.tipPercentage}%): ${result.tipAmount.toCurrencyString()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = GreenIncome)
                }
            }
        }
    }
}

@Composable
private fun FireCalculatorView(
    state: FinancialCalculatorsState,
    onUpdateState: ((FinancialCalculatorsState) -> FinancialCalculatorsState) -> Unit
) {
    val currencySymbol = LocalCurrency.current.symbol

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
                Text(
                    text = "What is FIRE?: FIRE stands for Financial Independence, Retire Early. The 25x Rule calculates the portfolio target needed so your passive investment returns pay for 100% of your living expenses forever.",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedTextField(
            value = state.fireAnnualExpenses,
            onValueChange = { newValue -> onUpdateState { it.copy(fireAnnualExpenses = newValue) } },
            label = { Text("Annual Living Expenses ($currencySymbol)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.fireCurrentInvestments,
                onValueChange = { newValue -> onUpdateState { it.copy(fireCurrentInvestments = newValue) } },
                label = { Text("Current Investments") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = state.fireMonthlySavings,
                onValueChange = { newValue -> onUpdateState { it.copy(fireMonthlySavings = newValue) } },
                label = { Text("Monthly Savings") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        val annualExp = state.fireAnnualExpenses.toDoubleOrNull() ?: 0.0
        val currentInv = state.fireCurrentInvestments.toDoubleOrNull() ?: 0.0
        val savings = state.fireMonthlySavings.toDoubleOrNull() ?: 0.0

        val result = CalculatorEngine.calculateFireTarget(annualExp, currentInv, savings)

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("FIRE Freedom Number (25x Rule)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        Text(result.targetFireNumber.toCurrencyString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = GreenIncome)
                    }

                    if (result.estimatedYearsToFire != null) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Estimated Years to FI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("~${result.estimatedYearsToFire} yrs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Current Investments: ${result.currentInvestments.toCurrencyString()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Remaining Needed: ${result.remainingAmountNeeded.toCurrencyString()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
