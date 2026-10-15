# Equity Bank SMS Parsing & Automation Specification

This document outlines the strict SMS parsing, sender filtering, and category inference rules for Equity Bank transactions in FinTrack.

---

## 1. Strict Sender Filtering Rules
To ensure zero cross-contamination between M-Pesa transactions and Equity Bank account ledgers, all Equity SMS processing is locked strictly to official Equity Bank senders:

* **Target Sender ID**: `EquityBank` (`EquityBank` or `%EquityBank%`).
* **Ignored Senders**: `MPESA`, `M-PESA`, `EQUITY` (when acting as M-Pesa relay notifications).
* **Code Implementation**:
  - History Importer: [`EquityImporter.kt`](file:///C:/Users/Mark/AndroidStudioProjects/fintrack-production/shared/src/androidMain/kotlin/com/fintrack/shared/feature/transaction/service/importer/EquityImporter.kt#L90-L105)
  - Broadcast Receiver: [`SmsReceiver.kt`](file:///C:/Users/Mark/AndroidStudioProjects/fintrack-production/shared/src/androidMain/kotlin/com/fintrack/shared/feature/transaction/service/automation/SmsReceiver.kt#L65-L72)

---

## 2. Supported Card & Account Confirmatory SMS Types
Only official transaction notifications sent by `EquityBank` are parsed. The engine supports:

| Transaction Type | Example SMS Pattern | Parsed Behavior |
| :--- | :--- | :--- |
| **Merchant Payments** | `Confirmed. Payment of KES 1,500.00 to JAVA HOUSE on 14/10/2026 14:30. Ref: EQ12345.` | Expense, Merchant `"JAVA HOUSE"`, Category inferred via `CategoryMatcher` |
| **Account Debits** | `Dear Customer, KES 2,500.00 has been debited from account... for payment to NAIVAS on 14-Oct-2026. Ref: EQ987654.` | Expense, Merchant `"NAIVAS"`, Category inferred via `CategoryMatcher` |
| **Card Authorizations** | `CONFIRMED. KES 350.00, Auth for card 4000****1234 at JAVA HOUSE on 2026-10-14 12:00:00 Ref: EQ554433.` | Expense, Merchant `"JAVA HOUSE"`, Category inferred via `CategoryMatcher` |
| **Account Credits / Deposits** | `Your account... has been credited with KES 10,000.00 from MARY JANE on 14-Oct-2026. Ref: EQ789012.` | Income, Sender `"MARY JANE"`, Category `"Other Income"` or matched rule |
| **ATM Withdrawals** | `Withdrawal of KES 5,000.00 from ATM... Ref: EQ112233.` | Expense, Category `"Transfer"`, Description `"ATM Withdrawal"` |

---

## 3. Category Inference & Fallback Architecture
In [`EquityParser.kt`](file:///C:/Users/Mark/AndroidStudioProjects/fintrack-production/shared/src/androidMain/kotlin/com/fintrack/shared/feature/transaction/util/EquityParser.kt):

* **CategoryMatcher Resolution**: The merchant/sender name is passed to `CategoryMatcher.resolveCategory`.
* **Unmapped Expense Fallback**: Unmapped merchant expenses resolve to `Category.UncategorizedExpense` (`"Uncategorized"`).
* **Unmapped Income Fallback**: Unmapped credits/deposits resolve to `Category.UncategorizedIncome` (`"Other Income"`).
* **Rule**: Unmapped merchant purchases **NEVER** default to `"Transfer"`. The `"Transfer"` category is reserved exclusively for ATM cash withdrawals and explicitly labeled bank transfers.

---

## 4. Non-Capturing Regex Architecture
All component regex constants (`CURRENCY`, `AMOUNT_VAL`, `DATE_TEXT`, `DATE_DASH`, `TIME`) in `EquityParser.kt` use non-capturing groups `(?:...)` to prevent capture group index shifting across Kotlin releases.

* Reference codes match both colon (`Ref: EQ12345`) and dot (`Ref. EQ12345`) variants: `Ref[:.]?\s*(\w+)`.
* Unit Test Suite: [`EquityParserTest.kt`](file:///C:/Users/Mark/AndroidStudioProjects/fintrack-production/shared/src/androidUnitTest/kotlin/com/fintrack/shared/feature/transaction/util/EquityParserTest.kt).
