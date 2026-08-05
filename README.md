# 💰 Fintrack KMP, The Ultimate SMS-Powered Financial Intelligence App

**Fintrack KMP** is a high-performance, personal finance app built with **Kotlin Multiplatform (KMP)**. It changes how you track money by combining automated SMS parsing with a sleek experience on both **Android** and **iOS**.

Shared business logic powers the engine, while **Compose Multiplatform** and **SwiftUI** create an excellent user interface. Fintrack isn't just a ledger; it's a financial companion that learns from your spending habits.

> 🚀 **95% Complete:** We are currently developing it, integrating closely with the [Fintrack Ktor Backend](https://github.com/markgithinji/Fintrack-ktor).

---

## 📸 Screenshot

<div align="center">
  
![Fintrack KMP App Preview](assets/android/Fintrack_preview.png)
*Professional dashboard featuring real-time balance tracking, automated insights, and smart budgets.*

</div>

---

## 🔥 Professional Financial Features

### 📡 Smart SMS Detection & Parsing (Android)
* **Zero-Effort Tracking**: Real-time tracking of **M-Pesa** and **Equity Bank** transactions via SMS.
* **Intelligent Graph Extraction**: A precise Regex engine handles complex financial events like **Reversals**, **Loan Approvals**, **Agent Withdrawals**, and **Merchant Till Payments**.
* **Intra-Account Awareness**: Smart logic detects and skips transfers between your linked accounts (e.g., Equity to M-Pesa) to avoid double-counting your net worth.
* **Real-time Balance Reconciliation**: Automatically pulls balance information from SMS headers to ensure the app's state matches your bank/M-Pesa state.
* **Immediate Sync**: Provides **immediate UI notifications** when received and uses **Expedited WorkManager** for guaranteed background sync.

### 📊 Advanced Financial Analytics & Summaries
* **Predictive Financial Health**: Forecasting logic predicts budget depletion and identifies spending volatility using backend logic.
* **Essential Spend Ratio**: Automatically calculates your financial health based on the **50/30/20 rule**, distinguishing "Essential" (Needs) vs. "Lifestyle" (Wants) spending.
* **Historical Look-back**: Automatically finds and shows your last active month's trends if the current month is empty. Includes comparisons (Monthly vs. Weekly) with context-aware wording.
* **Financial Highlights**: Key insights display top expenses, savings rates, and category correlations (e.g., "When you spend more on Dining Out, your Grocery spending drops").
* **Interactive Charts**: Clear visuals for spending patterns, income trends, and budget compliance.
* **Total Amount Logic**: Sophisticated math that handles **Transaction Fees** to ensure your net balance is accurate.

### 🔔 Intelligent Notification & Alarm System
* **Smart Budget Planning**: Set monthly budgets with category limits. Includes **real-time threshold alerts** (50%, 80%, 100%) to manage spending.
* **Automated Bill Reminders**: An intelligent backend detects recurring subscriptions and bills, automatically scheduling local reminders ahead of due dates.
* **Daily & Weekly Briefs**: Receive scheduled push notifications summarizing your total spending from the previous day or week.

### 🛡️ Enterprise-Grade Security
* **Step-up Biometric Authentication**: Verification is needed for sensitive operations (deleting accounts, clearing history) to ensure data integrity.
* **Biometric Vault**: Protect your data with **Fingerprint, FaceID, or TouchID**, maintaining privacy even if your device is unlocked.
* **Military-Grade Encryption**: Sensitive tokens are protected using **Google Tink (AES-256 GCM)** and stored securely in **Jetpack DataStore**.
* **Hardware-Backed Storage**: Master keys are managed by the **Android Keystore** and **iOS Keychain**.
* **Secure Authentication**: Encrypted token storage combined with automated token refresh and validation.

---

## 💡 Core Features

- **Multi-Account Management**: Track balances across checking, savings, credit cards, and cash in one place.
- **Income & Expense Tracking**: Monitor cash flow with daily, weekly, and monthly views.
- **Cloud-Synced Categorization**: Real-time synchronization of merchant keywords and category rules from the backend for accurate transaction mapping.
- **Category-based Analytics**: Visual breakdown of spending by categories (food, transportation, entertainment, etc.).
- **Transaction History**: Detailed ledger with search, filter, and export options.
- **Flexible Data Export**: Export your transaction history in several formats (**CSV, PDF, or JSON**) with support for **custom date ranges**. Choose your preferred format in settings for a personalized export experience.
- **Global Currency Preferences**: Easily switch between multiple currencies (USD, KES, EUR, etc.) with real-time updates across the app.
- **Secure Password Management**: Update your account password within the app with real-time validation and secure backend sync.
- **Daily Reminders**: Always remember to log an expense with customizable daily notifications scheduled through high-reliability background receivers.

---

## ✨ Polished UI & Animations

Fintrack KMP focuses on a smooth user experience with high-quality animations and transitions:

- **Shared Element Transitions**: Seamlessly adjust UI elements (like transaction cards and headers) across screens using the latest Compose Shared Transition API for a continuous visual flow.
- **Interactive Visualizations**: Animated donut charts and progress indicators respond to user selection with smooth transitions.
- **Dynamic Theming**: Real-time UI color changes between Income (Green) and Expense (Pink) modes, providing immediate visual context during data entry.
- **Reactive Currency Switching**: Instantly update all financial data displays throughout the app when preferences change.
- **Intelligent Loading States**: Custom shimmer effects and staggered item animations that prevent layout shifts and create a premium feel during data fetching.
- **Micro-interactions**: Subtle scale animations on chips, bouncy button presses, and `AnimatedContent` for text changes that make the app feel lively and responsive.

---

## 🚀 Tech Stack

**Cross-Platform Core:**
- **Kotlin Multiplatform**: 100% shared Business Logic & Networking.
- **KMP Architecture**: Clean Architecture + MVVM + Repository Pattern.
- **Networking**: Ktor Client with reliable retry logic.
- **Dependency Injection**: Koin (Multiplatform).
- **Serialization**: Kotlinx.Serialization (JSON & BigDecimal support).
- **Asynchronous**: Kotlin Coroutines + Flow.
- **Pagination**: Paging 3 for smooth ledger scrolling.

**Platform Specifics:**
- **Android**: Jetpack Compose, WorkManager, AlarmManager, Material 3, **Jetpack DataStore**, **Google Tink**.
- **iOS**: Compose Multiplatform (UI Sharing) + SwiftUI (Entry), LocalAuthentication.
- **Storage**: **Jetpack DataStore (Android)**, Multiplatform Settings + SQLDelight (Local Cache).

---

## 🏗️ Project Progress & Roadmap

| Feature Group | Status | Component |
| :--- | :--- | :--- |
| **SMS Parsing Engine** | ✅ Complete | M-Pesa & Equity (Android) |
| **Financial Summaries** | ✅ Complete | Monthly/Weekly Highlights |
| **Transaction Fee Logic** | ✅ Complete | Shared Domain Model |
| **Data Export (PDF/CSV)** | ✅ Complete | Multi-format Exporter |
| **Budgeting System** | ✅ Complete | Real-time Threshold Alerts |
| **iOS Native Hooks** | 🚧 70% | Background Sync WIP |
| **UI Test Coverage** | 🚧 10% | KMP-level UI Testing |

---

## ⚙️ Development Setup

1. **Backend**: Make sure the [Fintrack Ktor Backend](https://github.com/markgithinji/Fintrack-ktor) is running.
2. **IP Configuration**: Update `ApiConfig.kt` with your local machine's IP.
3. **Environment**: 
   - **Android**: Studio Koala+
   - **iOS**: Xcode 15+
4. **Quick Start (Data Seeding)**: To quickly test the app's features with realistic data without manual entry, use the **Portfolio Seeding** tool. 
   - Go to **Profile** > **Settings** > **Seed Dummy Data**.
   - This will generate 6 months of historical transactions to populate all charts and analytics.

To connect the app to a local instance of the backend during development:

1. **Find your Local IP**: On your laptop, find your local IP address (e.g., `192.168.x.x`).
2. **Update API Config**: 
   - Open `shared/src/commonMain/kotlin/com/fintrack/shared/feature/core/data/remote/ApiConfig.kt`.
   - Update the `DEVELOPMENT` environment URL with your IP:
     ```kotlin
     Environment.DEVELOPMENT -> "http://192.168.100.96:8080" // Replace with your laptop IP
     ```
3. **Android Network Security**:
   - Open `androidApp/src/main/res/xml/network_security_config.xml`.
   - Make sure your IP is under the `<domain-config>` to allow cleartext traffic:
     ```xml
     <domain includeSubdomains="true">192.168.100.96</domain>
     ```

---

## 🔗 Backend Integration

This app connects to the [Fintrack Ktor Backend](https://github.com/markgithinji/Fintrack-ktor) which provides:
- **RESTful API** for all financial operations.
- **JWT Authentication** with secure token management.
- **PostgreSQL Database** for data storage.
- **Smart Recurring Engine**: Automated detection for recurring payments and bill prediction.

---

## 🤝 Contribution & Feedback

Fintrack KMP showcases what modern Kotlin Multiplatform can achieve. If you're interested in our approach to SMS parsing in KMP or financial logic, feel free to check out the code!

---

**Built with Love ❤️**