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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrack.shared.feature.core.ui.SelectableFilterChip
import com.fintrack.shared.feature.navigation.ui.toCurrencyString
import com.fintrack.shared.feature.planning.domain.calculator.CalculatorEngine
import com.fintrack.shared.feature.planning.domain.calculator.FinancialCalculatorType
import com.fintrack.shared.ui.theme.GreenIncome

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialCalculatorsHubSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var activeCalculator by remember { mutableStateOf(FinancialCalculatorType.MMF_COMPOUND) }

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
                            text = "Interactive MMF, FIRE, FX, Mortgage & Savings Tools",
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

            // Calculator Type Chips Row
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(FinancialCalculatorType.entries) { type ->
                    val label = when (type) {
                        FinancialCalculatorType.MMF_COMPOUND -> "📈 MMF Compound"
                        FinancialCalculatorType.SAVINGS_RATE_FREEDOM -> "🎯 Savings Rate & Freedom"
                        FinancialCalculatorType.CURRENCY_HEDGE -> "💱 USD Currency Shield"
                        FinancialCalculatorType.COAST_FIRE -> "🌴 Coast FIRE"
                        FinancialCalculatorType.MORTGAGE_SAVER -> "🏠 Mortgage Saver"
                        FinancialCalculatorType.EDUCATION_FUND -> "🎓 College Fund"
                        FinancialCalculatorType.CAR_OWNERSHIP -> "🚗 Car Ownership"
                        FinancialCalculatorType.DEBT_ACCELERATOR -> "⚡ Debt Accelerator"
                        FinancialCalculatorType.BUY_VS_RENT -> "🏘️ Buy vs Rent"
                        FinancialCalculatorType.TIP_SPLITTER -> "🍽️ Tip & Split"
                        FinancialCalculatorType.LOAN_AMORTIZATION -> "💳 Loan Interest"
                        FinancialCalculatorType.EMERGENCY_FUND -> "🛡️ Emergency Reserve"
                        FinancialCalculatorType.FIRE_RETIREMENT -> "🔥 FIRE Freedom"
                    }

                    SelectableFilterChip(
                        selected = activeCalculator == type,
                        label = label,
                        onClick = { activeCalculator = type }
                    )
                }
            }

            // Selected Calculator Form & Results
            when (activeCalculator) {
                FinancialCalculatorType.MMF_COMPOUND -> MmfCalculatorView()
                FinancialCalculatorType.SAVINGS_RATE_FREEDOM -> SavingsRateFreedomCalculatorView()
                FinancialCalculatorType.CURRENCY_HEDGE -> CurrencyHedgeCalculatorView()
                FinancialCalculatorType.COAST_FIRE -> CoastFireCalculatorView()
                FinancialCalculatorType.MORTGAGE_SAVER -> MortgageSaverCalculatorView()
                FinancialCalculatorType.EDUCATION_FUND -> EducationFundCalculatorView()
                FinancialCalculatorType.CAR_OWNERSHIP -> CarOwnershipCalculatorView()
                FinancialCalculatorType.DEBT_ACCELERATOR -> DebtAcceleratorCalculatorView()
                FinancialCalculatorType.BUY_VS_RENT -> BuyVsRentCalculatorView()
                FinancialCalculatorType.TIP_SPLITTER -> TipSplitterCalculatorView()
                FinancialCalculatorType.LOAN_AMORTIZATION -> LoanCalculatorView()
                FinancialCalculatorType.EMERGENCY_FUND -> EmergencyFundCalculatorView()
                FinancialCalculatorType.FIRE_RETIREMENT -> FireCalculatorView()
            }
        }
    }
}

@Composable
private fun SavingsRateFreedomCalculatorView() {
    var incomeText by remember { mutableStateOf("150000") }
    var expenseText by remember { mutableStateOf("90000") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = incomeText,
                onValueChange = { incomeText = it },
                label = { Text("Monthly Net Income (KES)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = expenseText,
                onValueChange = { expenseText = it },
                label = { Text("Monthly Expenses (KES)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        val income = incomeText.toDoubleOrNull() ?: 0.0
        val expense = expenseText.toDoubleOrNull() ?: 0.0

        val result = remember(income, expense) {
            CalculatorEngine.calculateSavingsRateFreedom(income, expense)
        }

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
private fun CurrencyHedgeCalculatorView() {
    var kesAmountText by remember { mutableStateOf("1000000") }
    var fxRateText by remember { mutableStateOf("130") }
    var yearsText by remember { mutableStateOf("3") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = kesAmountText,
            onValueChange = { kesAmountText = it },
            label = { Text("Investment Capital (KES)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = fxRateText,
                onValueChange = { fxRateText = it },
                label = { Text("USD/KES Rate") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = yearsText,
                onValueChange = { yearsText = it },
                label = { Text("Years") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(0.8f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        val kes = kesAmountText.toDoubleOrNull() ?: 0.0
        val fx = fxRateText.toDoubleOrNull() ?: 130.0
        val years = yearsText.toIntOrNull() ?: 3

        val result = remember(kes, fx, years) {
            CalculatorEngine.calculateCurrencyHedge(kes, fx, years)
        }

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
                Text("$years-Year USD Hedging Shield Outcome", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("USD Hedged Portfolio (KES Value)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(result.usdHedgedOutcomeInKes.toCurrencyString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GreenIncome)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Local KES MMF Outcome", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(result.localKesOutcome.toCurrencyString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

                Text(
                    text = if (result.isUsdHedgeSuperior) {
                        "USD Hedging creates ${result.hedgingShieldDifferenceKes.toCurrencyString()} MORE net wealth over $years years due to currency devaluation defense (@5% p.a.)."
                    } else {
                        "Local KES MMF yield (@11.5%) creates ${result.hedgingShieldDifferenceKes.toCurrencyString()} MORE net wealth over $years years."
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
private fun CoastFireCalculatorView() {
    var ageText by remember { mutableStateOf("30") }
    var retireAgeText by remember { mutableStateOf("60") }
    var currentInvestmentsText by remember { mutableStateOf("2500000") }
    var annualExpenseText by remember { mutableStateOf("720000") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = ageText,
                onValueChange = { ageText = it },
                label = { Text("Current Age") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = retireAgeText,
                onValueChange = { retireAgeText = it },
                label = { Text("Retire Age") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        OutlinedTextField(
            value = currentInvestmentsText,
            onValueChange = { currentInvestmentsText = it },
            label = { Text("Current Investments Portfolio (KES)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = annualExpenseText,
            onValueChange = { annualExpenseText = it },
            label = { Text("Annual Expenses in Retirement (KES)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        val age = ageText.toIntOrNull() ?: 30
        val retireAge = retireAgeText.toIntOrNull() ?: 60
        val currentInv = currentInvestmentsText.toDoubleOrNull() ?: 0.0
        val expenses = annualExpenseText.toDoubleOrNull() ?: 0.0

        val result = remember(age, retireAge, currentInv, expenses) {
            CalculatorEngine.calculateCoastFire(age, retireAge, currentInv, expenses)
        }

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
                        "Your current KES ${result.currentInvestments.toCurrencyString()} portfolio will compound to ${result.projectedBalanceAtRetirement.toCurrencyString()} by age $retireAge at 8% p.a. without adding a single extra shilling! You can spend 100% of your income."
                    } else {
                        "Saving KES ${result.monthlySavingsNeededToCoast.toCurrencyString()}/mo for the next ${retireAge - age} years will reach Coast FIRE autopilot readiness."
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
private fun MortgageSaverCalculatorView() {
    var principalText by remember { mutableStateOf("10000000") }
    var rateText by remember { mutableStateOf("14.0") }
    var tenureText by remember { mutableStateOf("20") }
    var extraPaymentText by remember { mutableStateOf("10000") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = principalText,
            onValueChange = { principalText = it },
            label = { Text("Mortgage Principal (KES)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = rateText,
                onValueChange = { rateText = it },
                label = { Text("APR %") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(0.8f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = tenureText,
                onValueChange = { tenureText = it },
                label = { Text("Tenure (Yrs)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(0.8f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = extraPaymentText,
                onValueChange = { extraPaymentText = it },
                label = { Text("+Extra Principal/mo") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1.2f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        val principal = principalText.toDoubleOrNull() ?: 0.0
        val rate = rateText.toDoubleOrNull() ?: 0.0
        val tenure = tenureText.toIntOrNull() ?: 20
        val extra = extraPaymentText.toDoubleOrNull() ?: 0.0

        val result = remember(principal, rate, tenure, extra) {
            CalculatorEngine.calculateMortgageSaver(principal, rate, tenure, extra)
        }

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
private fun EducationFundCalculatorView() {
    var childAgeText by remember { mutableStateOf("2") }
    var annualTuitionText by remember { mutableStateOf("300000") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = childAgeText,
                onValueChange = { childAgeText = it },
                label = { Text("Child Current Age") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = annualTuitionText,
                onValueChange = { annualTuitionText = it },
                label = { Text("Today Annual Tuition (KES)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1.5f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        val age = childAgeText.toIntOrNull() ?: 2
        val tuition = annualTuitionText.toDoubleOrNull() ?: 300000.0

        val result = remember(age, tuition) {
            CalculatorEngine.calculateEducationFund(age, currentAnnualTuition = tuition)
        }

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
private fun MmfCalculatorView() {
    var initialDepositText by remember { mutableStateOf("100000") }
    var monthlyContributionText by remember { mutableStateOf("10000") }
    var annualRateText by remember { mutableStateOf("11.5") }
    var yearsText by remember { mutableStateOf("3") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = initialDepositText,
            onValueChange = { initialDepositText = it },
            label = { Text("Initial Deposit (KES)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = monthlyContributionText,
                onValueChange = { monthlyContributionText = it },
                label = { Text("Monthly Deposit") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = annualRateText,
                onValueChange = { annualRateText = it },
                label = { Text("MMF Rate % p.a.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = yearsText,
                onValueChange = { yearsText = it },
                label = { Text("Years") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(0.8f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        val initial = initialDepositText.toDoubleOrNull() ?: 0.0
        val monthly = monthlyContributionText.toDoubleOrNull() ?: 0.0
        val rate = annualRateText.toDoubleOrNull() ?: 0.0
        val years = yearsText.toIntOrNull() ?: 1

        val result = remember(initial, monthly, rate, years) {
            CalculatorEngine.calculateMmfYield(initial, monthly, rate, years)
        }

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
private fun CarOwnershipCalculatorView() {
    var priceText by remember { mutableStateOf("2500000") }
    var runningCostText by remember { mutableStateOf("30000") }
    var insuranceText by remember { mutableStateOf("100000") }
    var yearsText by remember { mutableStateOf("3") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = priceText,
            onValueChange = { priceText = it },
            label = { Text("Car Purchase Price (KES)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = runningCostText,
                onValueChange = { runningCostText = it },
                label = { Text("Fuel & Maint/mo") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = insuranceText,
                onValueChange = { insuranceText = it },
                label = { Text("Insurance/yr") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = yearsText,
                onValueChange = { yearsText = it },
                label = { Text("Years") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(0.8f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        val price = priceText.toDoubleOrNull() ?: 0.0
        val running = runningCostText.toDoubleOrNull() ?: 0.0
        val insurance = insuranceText.toDoubleOrNull() ?: 0.0
        val years = yearsText.toIntOrNull() ?: 3

        val result = remember(price, running, insurance, years) {
            CalculatorEngine.calculateCarOwnership(price, running, insurance, years)
        }

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
private fun DebtAcceleratorCalculatorView() {
    var principalText by remember { mutableStateOf("400000") }
    var rateText by remember { mutableStateOf("16.0") }
    var minPaymentText by remember { mutableStateOf("18000") }
    var extraBoostText by remember { mutableStateOf("7000") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = principalText,
            onValueChange = { principalText = it },
            label = { Text("Total Debt Principal (KES)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = rateText,
                onValueChange = { rateText = it },
                label = { Text("APR %") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(0.8f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = minPaymentText,
                onValueChange = { minPaymentText = it },
                label = { Text("Min Pay/mo") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = extraBoostText,
                onValueChange = { extraBoostText = it },
                label = { Text("+Extra Boost") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        val principal = principalText.toDoubleOrNull() ?: 0.0
        val rate = rateText.toDoubleOrNull() ?: 0.0
        val minPay = minPaymentText.toDoubleOrNull() ?: 0.0
        val boost = extraBoostText.toDoubleOrNull() ?: 0.0

        val result = remember(principal, rate, minPay, boost) {
            CalculatorEngine.calculateDebtAccelerator(principal, rate, minPay, boost)
        }

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
            }
        }
    }
}

@Composable
private fun BuyVsRentCalculatorView() {
    var priceText by remember { mutableStateOf("12000000") }
    var rentText by remember { mutableStateOf("60000") }
    var yearsText by remember { mutableStateOf("10") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = priceText,
            onValueChange = { priceText = it },
            label = { Text("Property Buy/Build Price (KES)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = rentText,
                onValueChange = { rentText = it },
                label = { Text("Monthly Rent (KES)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1.2f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = yearsText,
                onValueChange = { yearsText = it },
                label = { Text("Years") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(0.8f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        val price = priceText.toDoubleOrNull() ?: 0.0
        val rent = rentText.toDoubleOrNull() ?: 0.0
        val years = yearsText.toIntOrNull() ?: 10

        val result = remember(price, rent, years) {
            CalculatorEngine.calculateBuyVsRent(price, rent, years)
        }

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
private fun TipSplitterCalculatorView() {
    var billText by remember { mutableStateOf("5000") }
    var tipPercentText by remember { mutableStateOf("10") }
    var peopleText by remember { mutableStateOf("4") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = billText,
            onValueChange = { billText = it },
            label = { Text("Total Bill Amount (KES)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = tipPercentText,
                onValueChange = { tipPercentText = it },
                label = { Text("Tip %") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = peopleText,
                onValueChange = { peopleText = it },
                label = { Text("People Split") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        val bill = billText.toDoubleOrNull() ?: 0.0
        val tipPct = tipPercentText.toDoubleOrNull() ?: 0.0
        val people = peopleText.toIntOrNull() ?: 1

        val result = remember(bill, tipPct, people) {
            CalculatorEngine.calculateTipAndSplit(bill, tipPct, people)
        }

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
private fun LoanCalculatorView() {
    var principalText by remember { mutableStateOf("500000") }
    var rateText by remember { mutableStateOf("14.0") }
    var monthsText by remember { mutableStateOf("24") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = principalText,
            onValueChange = { principalText = it },
            label = { Text("Loan Principal Amount (KES)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = rateText,
                onValueChange = { rateText = it },
                label = { Text("Interest Rate % p.a.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = monthsText,
                onValueChange = { monthsText = it },
                label = { Text("Tenure (Months)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        val principal = principalText.toDoubleOrNull() ?: 0.0
        val rate = rateText.toDoubleOrNull() ?: 0.0
        val months = monthsText.toIntOrNull() ?: 12

        val result = remember(principal, rate, months) {
            CalculatorEngine.calculateLoanAmortization(principal, rate, months)
        }

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Estimated Monthly Loan Payment", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f))
                Text(result.monthlyPayment.toCurrencyString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)

                HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Interest Cost", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(result.totalInterestPaid.toCurrencyString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Amount Repaid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(result.totalAmountRepaid.toCurrencyString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmergencyFundCalculatorView() {
    var expenseText by remember { mutableStateOf("60000") }
    var monthsText by remember { mutableStateOf("6") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = expenseText,
                onValueChange = { expenseText = it },
                label = { Text("Monthly Expenses (KES)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1.2f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = monthsText,
                onValueChange = { monthsText = it },
                label = { Text("Buffer (Months)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(0.8f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        val expense = expenseText.toDoubleOrNull() ?: 0.0
        val months = monthsText.toIntOrNull() ?: 6

        val result = remember(expense, months) {
            CalculatorEngine.calculateEmergencyFund(expense, months)
        }

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Target Emergency Reserve Required", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                Text(result.targetFundRequired.toCurrencyString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Provides 100% cash coverage for ${result.cushionMonths} months of living expenses without taking on debt.", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
private fun FireCalculatorView() {
    var annualExpenseText by remember { mutableStateOf("720000") }
    var currentInvestmentsText by remember { mutableStateOf("500000") }
    var monthlySavingsText by remember { mutableStateOf("30000") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = annualExpenseText,
            onValueChange = { annualExpenseText = it },
            label = { Text("Annual Living Expenses (KES)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = currentInvestmentsText,
                onValueChange = { currentInvestmentsText = it },
                label = { Text("Current Investments") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = monthlySavingsText,
                onValueChange = { monthlySavingsText = it },
                label = { Text("Monthly Savings") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        val annualExp = annualExpenseText.toDoubleOrNull() ?: 0.0
        val currentInv = currentInvestmentsText.toDoubleOrNull() ?: 0.0
        val savings = monthlySavingsText.toDoubleOrNull() ?: 0.0

        val result = remember(annualExp, currentInv, savings) {
            CalculatorEngine.calculateFireTarget(annualExp, currentInv, savings)
        }

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
