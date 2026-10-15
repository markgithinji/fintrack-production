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
Only official transaction notifications sent by `EquityBank` that represent real bank account debits or deposits are parsed. The engine supports:

| Transaction Type | Example SMS Pattern | Parsed Behavior |
| :--- | :--- | :--- |
| **Account Deposits** | `Dear MARK GITHINJI NGOTHI, Your transaction of KES. 15,000.00 has successfully been deposited to Equity Account in favor of MARK GITHINJI NGOTHI Ref. Number TJBCO740DU.` | Income (`isIncome = true`), Sender `"PROCHIC SUPPLIES"` / `"MARK NGOTHI"`, Category `"Other Income"` |
| **Account Credits** | `Your transaction of Kshs. 37,000.0 has been credited to 254729097750 MARK NGOTHI. Ref: EQA0645E7D353D2` | Income (`isIncome = true`), Category `"Other Income"` |
| **Merchant Payments** | `Confirmed. Payment of KES 1,500.00 to JAVA HOUSE on 14/10/2026 14:30. Ref: EQ12345.` | Expense, Merchant `"JAVA HOUSE"`, Category inferred via `CategoryMatcher` |
| **Account Debits** | `Dear Customer, KES 2,500.00 has been debited from account... for payment to NAIVAS on 14-Oct-2026. Ref: EQ987654.` | Expense, Merchant `"NAIVAS"`, Category inferred via `CategoryMatcher` |
| **Card Authorizations** | `CONFIRMED. KES 350.00, Auth for card 4000****1234 at JAVA HOUSE on 2026-10-14 12:00:00 Ref: EQ554433.` | Expense, Merchant `"JAVA HOUSE"`, Category inferred via `CategoryMatcher` |
| **ATM Withdrawals** | `Withdrawal of KES 5,000.00 from ATM... Ref: EQ112233.` | Expense, Category `"Transfer"`, Description `"ATM Withdrawal"` |

---

## 3. Category Inference & Fallback Architecture
In [`EquityParser.kt`](file:///C:/Users/Mark/AndroidStudioProjects/fintrack-production/shared/src/androidMain/kotlin/com/fintrack/shared/feature/transaction/util/EquityParser.kt):

* **CategoryMatcher Resolution**: The merchant/sender name is passed to `CategoryMatcher.resolveCategory`.
* **Unmapped Expense Fallback**: Unmapped merchant expenses resolve to `Category.UncategorizedExpense` (`"Uncategorized"`).
* **Unmapped Income Fallback**: Unmapped credits/deposits resolve to `Category.OtherIncome` (`"Other Income"`).
* **Rule**: Unmapped merchant purchases **NEVER** default to `"Transfer"`. The `"Transfer"` category is reserved exclusively for ATM cash withdrawals and explicitly labeled bank transfers.

---

## 4. Non-Capturing Regex Architecture
All component regex constants (`CURRENCY`, `AMOUNT_VAL`, `DATE_TEXT`, `DATE_DASH`, `TIME`) in `EquityParser.kt` use non-capturing groups `(?:...)` to prevent capture group index shifting across Kotlin releases.

* Reference codes match both colon (`Ref: EQ12345`) and dot (`Ref. EQ12345`) variants: `Ref[:.]?\s*(\w+)`.
* Unit Test Suite: [`EquityParserTest.kt`](file:///C:/Users/Mark/AndroidStudioProjects/fintrack-production/shared/src/androidUnitTest/kotlin/com/fintrack/shared/feature/transaction/util/EquityParserTest.kt).

---

## 5. Explicitly Ignored Messages & System Boundaries

To prevent duplicate entries, false financial expenses, or internal movement pollution, the parser explicitly rejects these non-transactional SMS types:

| Message Type | Ignored Pattern Example | Reason for Ignoring |
| :--- | :--- | :--- |
| **M-Pesa Till Payment Confirmations** | `Confirmed. Payment of KES. 1,000.00 to YARD PARK PHARMACY Till No. 0791201005 has been received.` | M-Pesa Till payment confirmation; handled under M-Pesa account, ignored during Equity parsing |
| **M-Pesa Paybill Confirmations** | `Confirmed, Bill payment to AVECHI SERVICES of KES. 43,000.00 for account 636636...` | M-Pesa Paybill confirmation; handled under M-Pesa account, ignored during Equity parsing |
| **Intra-Account Masked Self-Transfers** | `200.00 KES has been successfully sent from 1********9426 to 7********4608. Ref. AA20AF904B36B...` | Internal transfer between user's own masked bank accounts; neither income nor expense |
| **Failed Transactions** | `Your card no... payment to NETFLIX.COM of KES 700.00 has failed due to insufficient funds.` | Prevent failed/aborted card or bank purchases from recording as real expenses |
| **M-Pesa Relay Notifications** | `EQUITY BANK has sent Ksh10,000.00 to your MPESA. The MPESA receipt number is...` | Handled exclusively by `MpesaParser` under M-Pesa account to prevent double-counting |
| **Feedback & Experience Surveys** | `Dear Member, we value your feedback! Please share your experience...` | Informational survey link, non-transactional |
| **Marketing & Loan Promos** | `Dial *247*6#, use Equitel or Equity Mobile App to get an instant loan...` | Marketing promotion, non-transactional |
| **Bill Payment Due Notices** | `Your bill for account... is due in 3 days.` | Due notice, non-transactional |
