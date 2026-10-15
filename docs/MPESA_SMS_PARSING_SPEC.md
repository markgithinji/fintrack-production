# M-Pesa SMS Parsing & Automation Specification

This document outlines the strict SMS parsing, sender filtering, transaction fee extraction, balance extraction, and category inference rules for M-Pesa transactions in FinTrack.

---

## 1. Strict Sender Filtering Rules
To ensure zero cross-contamination between bank accounts and M-Pesa account ledgers, all M-Pesa SMS processing is locked strictly to official Safaricom M-Pesa senders:

* **Target Sender ID**: `MPESA` or `M-PESA` (case-insensitive).
* **Code Implementation**:
  - History Importer: [`MpesaImporter.kt`](file:///C:/Users/Mark/AndroidStudioProjects/fintrack-production/shared/src/androidMain/kotlin/com/fintrack/shared/feature/transaction/service/importer/MpesaImporter.kt)
  - Broadcast Receiver: [`SmsReceiver.kt`](file:///C:/Users/Mark/AndroidStudioProjects/fintrack-production/shared/src/androidMain/kotlin/com/fintrack/shared/feature/transaction/service/automation/SmsReceiver.kt#L60-L65)

---

## 2. Supported M-Pesa Transaction Types

Only official transaction notifications containing `"Confirmed"` sent by `MPESA` / `M-PESA` are parsed. The engine supports 12 core transaction categories:

| Transaction Type | Example SMS Pattern | Parsed Behavior |
| :--- | :--- | :--- |
| **Send Money** | `QZE8901234 Confirmed. Ksh1,500.00 sent to JOHN DOE 0712345678 on 15/1/25 at 2:30 PM.` | Expense, Recipient `"JOHN DOE 0712345678"`, Fee & Balance extracted |
| **Receive Money** | `QZE8901235 Confirmed. You have received Ksh5,000.00 from MARY WANJIKU 0722001122 on 15/1/25 at 3:15 PM.` | Income, Sender `"MARY WANJIKU 0722001122"`, Balance extracted |
| **Lipa na M-Pesa (Paybill)** | `QZE8901236 Confirmed. Ksh3,200.00 paid to JAVA HOUSE WESTLANDS on 15/1/25 at 8:00 PM.` | Expense, Merchant `"JAVA HOUSE WESTLANDS"`, Category inferred |
| **Lipa na M-Pesa (Till No)** | `Confirmed. Payment of KES 500.00 to JAVA HOUSE Till No. 123456 has been received. Ref. QZE8901237 on 15/1/25 at 2:00 PM.` | Income (Merchant Till) / Expense |
| **Agent Withdrawal** | `QZE8901238 Confirmed. Ksh2,000.00 withdrawn from Agent 12345 - SAFARICOM SHOP on 15/1/25 at 10:00 AM.` | Expense, Description `"Withdrawn from Agent..."`, Fee & Balance extracted |
| **Agent Deposit** | `QZE8901239 Confirmed. Ksh5,000.00 deposited to your M-PESA account by Agent 12345.` | Income, Description `"Deposit from Agent..."`, Balance extracted |
| **Buy Airtime** | `QZE8901240 Confirmed. You bought Ksh100.00 of airtime on 15/1/25 at 1:00 PM.` | Expense, Category `"Airtime"`, Description `"Bought airtime"` |
| **Fuliza Repayment** | `QZE8901241 Confirmed. Ksh500.00 from your M-PESA has been used to fully pay your outstanding Fuliza M-PESA.` | Expense, Category `"Loans"`, Description `"Fuliza M-PESA Repayment"` |
| **M-Shwari / Loan Approval** | `QZE8901242 Confirmed. Your M-Shwari loan has been approved... and Ksh2,000.00 deposited to your M-PESA account.` | Income, Category `"Loans"`, Description `"Loan Approved"` |
| **M-Shwari / Loan Repayment** | `QZE8901243 Confirmed. Loan of Ksh2,000.00 repaid from your M-PESA account to M-Shwari.` | Expense, Category `"Loans"`, Description `"Loan Repayment"` |
| **Bank to M-Pesa Transfer** | `EQUITY BANK has sent Ksh10,000.00 to your MPESA. The MPESA receipt number is QZE8901244...` | Income, Category `"Other Income"`, Description `"Received from EQUITY BANK"` |
| **Transaction Reversal** | `QZE8901245 Confirmed. Reversal of transaction QZE8901234... Ksh1,500.00 is credited to your M-PESA account.` | Reversal handling, Description `"Reversal of QZE8901234"` |

---

## 3. Transaction Cost & Balance Extraction Architecture
In [`MpesaParser.kt`](file:///C:/Users/Mark/AndroidStudioProjects/fintrack-production/shared/src/androidMain/kotlin/com/fintrack/shared/feature/transaction/util/MpesaParser.kt):

* **Transaction Cost Extraction**:
  `costRegex = """Transaction cost[\s,]+[Kk][Ss][Hh][.\s]*$AMOUNT_VAL"""`
* **Account Balance Extraction**:
  `mpesaRegex = """(?:New\s+)?M-?PESA\s+(?:account\s+)?balance\s+(?:is|was)\s+(?:Ksh\.?\s*|KSH\s*|KES\s*)?([\d,]+\.\d{1,2})"""`
* **M-Shwari Balance Exclusion**: Explicitly ignores M-Shwari / KCB M-Pesa balance statements when parsing main M-Pesa account balance.

---

## 4. Category Inference & Fallback Architecture
* **CategoryMatcher Resolution**: Merchant/recipient string + raw SMS body is passed to `CategoryMatcher.resolveCategory`.
* **Unmapped Expense Fallback**: Unmapped merchant expenses resolve to `Category.UncategorizedExpense` (`"Uncategorized"`).
* **Unmapped Income Fallback**: Unmapped credits/deposits resolve to `Category.UncategorizedIncome` (`"Other Income"`).
* **Unit Test Suite**: [`MpesaParserTest.kt`](file:///C:/Users/Mark/AndroidStudioProjects/fintrack-production/shared/src/androidUnitTest/kotlin/com/fintrack/shared/feature/transaction/util/MpesaParserTest.kt).

---

## 5. Explicitly Ignored Messages & System Boundaries

To prevent duplicate entries or false financial expenses, the parser explicitly rejects these non-transactional SMS types:

| Message Type | Ignored Pattern Example | Reason for Ignoring |
| :--- | :--- | :--- |
| **Failed Transactions** | `Failed. Insufficient funds in your M-PESA account to send KSH1,500.00...` | Prevent failed/aborted transactions from recording as real expenses |
| **Fuliza Draw Limit Alerts** | `Confirmed. Fuliza M-PESA amount is Ksh 80.00. Total Fuliza outstanding is Ksh148.61...` | Prevent Fuliza overdraft limit notifications from double-counting with the underlying purchase |
| **Promotional & Marketing Loan Ads** | `Sort your financial needs today! Get a KCB M-Pesa loan of up to KES 6500...` | Marketing promotion, non-transactional |
| **Loan Limit Queries** | `Your loan limit is Ksh 5,000.00.` | Account inquiry, non-transactional |
| **Balance Queries** | `M-PESA account balance was Ksh1,234.56 on...` | Pure balance query, non-transactional |
