# 💰 Fintrack KMP — The Ultimate SMS-Powered Financial Intelligence App

**Fintrack KMP** is a high-performance, **cross-platform personal finance ecosystem** built with **Kotlin Multiplatform (KMP)**. It transforms the way you track money by combining the power of **automated SMS parsing** with a polished, native experience on both **Android** and **iOS**.

Shared business logic powers the engine, while **Compose Multiplatform** and **SwiftUI** deliver a top-tier UI. Fintrack isn't just a ledger; it's a financial companion that learns from your spending patterns.

> 🚀 **90% Complete:** Currently in active development, integrating deeply with the [Fintrack Ktor Backend](https://github.com/markgithinji/Fintrack-ktor).

---

## 📸 Screenshot

<div align="center">
  
![Fintrack KMP App Preview](assets/android/Fintrack_preview.png)
*Professional dashboard featuring real-time balance tracking, automated insights, and smart budgets.*

</div>

---

## 🔥 Professional Financial Features

### 📡 Smart SMS Detection & Parsing (Android)
*   **Zero-Effort Tracking**: Real-time **M-Pesa** and **Equity Bank** transaction tracking via SMS.
*   **Intelligent Graph Extraction**: High-precision Regex engine handles complex financial events like **Reversals**, **Loan Approvals**, **Agent Withdrawals**, and **Merchant Till Payments** natively.
*   **Intra-Account Awareness**: Smart logic to detect and skip transfers between your own linked accounts (e.g., Equity to M-Pesa) to prevent double-counting your net worth.
*   **Real-time Balance Reconciliation**: Automatically extracts balance data from SMS headers to ensure the app's local state matches your bank/M-Pesa state perfectly.
*   **Immediate Sync**: Provides **immediate UI notifications** upon receipt and uses **Expedited WorkManager** for guaranteed background synchronization.

### 📊 Advanced Financial Analytics & Summaries
*   **Predictive Financial Health**: Forecasting logic that predicts budget depletion and identifies spending volatility using backend-driven machine logic.
*   **Essential Spend Ratio**: Automatically calculates your financial health based on the **50/30/20 rule**, identifying "Essential" (Needs) vs. "Lifestyle" (Wants) spending.
*   **Historical Look-back**: Automatically discovers and displays your last active month's trends if the current month is empty. Includes dual-level comparisons (Monthly vs. Weekly) with context-aware wording.
*   **Financial Highlights**: Key insights showing top expenses, savings rate, and category correlations (e.g., "When you spend more on Dining Out, your Grocery spending drops").
*   **Interactive Charts**: Beautiful visualizations for spending patterns, income trends, and budget compliance.
*   **Total Amount Logic**: Professional-grade math that handles **Transaction Fees** natively to ensure your net balance is always accurate.

### 🔔 Intelligent Notification & Alarm System
*   **Smart Budget Planning**: Set monthly budgets with category-wise limits. Includes **real-time threshold alerts** (50%, 80%, 100%) to keep your spending in check.
*   **Automated Bill Reminders**: Intelligent backend engine that detects recurring subscriptions and bills and automatically schedules local reminders before they are due.
*   **Daily & Weekly Briefs**: Stay informed with scheduled push notifications summarizing your previous day's or week's total spending.

### 🛡️ Enterprise-Grade Security
*   **Step-up Biometric Authentication**: Identity verification required for sensitive operations (deleting accounts, clearing history) to ensure maximum data integrity.
*   **Biometric Vault**: Protect your data with **Fingerprint, FaceID, or TouchID** lock, ensuring privacy even if your device is unlocked.
*   **Military-Grade Encryption**: Sensitive tokens are encrypted using **Google Tink (AES-256 GCM)** and stored securely in **Jetpack DataStore**.
*   **Hardware-Backed Storage**: Master keys are managed by the **Android Keystore** and **iOS Keychain**.
*   **Secure Authentication**: Encrypted token storage with automated token refresh and validation.

---

## 💡 Core Features

- **Multi-Account Management**: Track balances across checking, savings, credit cards, and cash in a single unified view.
- **Income & Expense Tracking**: Monitor cash flow with daily, weekly, and monthly period views.
- **Cloud-Synced Categorization**: Real-time synchronization of merchant keywords and category rules from the backend for high-accuracy transaction mapping.
- **Category-based Analytics**: Visual breakdown of spending by categories (food, transportation, entertainment, etc.).
- **Transaction History**: Comprehensive ledger with search, filter, and export capabilities.
- **Flexible Data Export**: Export your transaction history in multiple formats (**CSV, PDF, or JSON**) with support for **custom date ranges**. Choose your preferred format in settings for a personalized export experience.
- **Global Currency Preferences**: Seamlessly switch between multiple currencies (USD, KES, EUR, etc.) with real-time UI updates across the entire application.
- **Secure Password Management**: Update your account password directly from the app with real-time validation and secure backend synchronization.
- **Daily Reminders**: Never forget to log an expense with customizable daily push notifications scheduled via high-reliability background receivers.

---

## ✨ Polished UI & Animations

Fintrack KMP prioritizes a fluid user experience with high-quality animations and transitions:

- **Shared Element Transitions**: Seamlessly morph UI elements (like transaction cards and headers) across screens using the latest Compose Shared Transition API for a continuous visual flow.
- **Interactive Visualizations**: Animated donut charts and progress indicators that respond to user selection with smooth, interpolated transitions.
- **Dynamic Theming**: Real-time UI color shifts between Income (Green) and Expense (Pink) modes, providing immediate visual context during data entry.
- **Reactive Currency Switching**: Instantly update all financial data displays across the app using Compose `CompositionLocal` and reactive flows when preferences change.
- **Intelligent Loading States**: Custom shimmer effects and staggered item animations that prevent layout shifts and provide a premium feel during data fetching.
- **Micro-interactions**: Subtle scale animations on chips, bouncy button presses, and `AnimatedContent` for text changes that make the app feel alive and responsive.

---

## 🚀 Tech Stack

**Cross-Platform Core:**
- **Kotlin Multiplatform**: 100% shared Business Logic & Networking.
- **KMP Architecture**: Clean Architecture + MVVM + Repository Pattern.
- **Networking**: Ktor Client with high-reliability retry logic.
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
| **UI Test Coverage** | 🚧 40% | KMP-level UI Testing |

---

## ⚙️ Development Setup

1. **Backend**: Ensure [Fintrack Ktor Backend](https://github.com/markgithinji/Fintrack-ktor) is running.
2. **IP Configuration**: Update `ApiConfig.kt` with your local machine's IP.
3. **Environment**: 
   - **Android**: Studio Koala+
   - **iOS**: Xcode 15+
4. **Quick Start (Data Seeding)**: To quickly test the app's features with realistic data without manual entry, use the **Portfolio Seeding** tool. 
   - Go to **Profile** > **Settings** > **Seed Dummy Data**.
   - This will generate 6 months of historical transactions to populate all charts and analytics.

To connect the app to a local instance of the backend during development:

1. **Find your Local IP**: On your host machine (laptop), find your local IP address (e.g., `192.168.x.x`).
2. **Update API Config**: 
   - Open `shared/src/commonMain/kotlin/com/fintrack/shared/feature/core/data/remote/ApiConfig.kt`.
   - Update the `DEVELOPMENT` environment URL with your IP:
     ```kotlin
     Environment.DEVELOPMENT -> "http://192.168.100.96:8080" // Replace with your laptop IP
     ```
3. **Android Network Security**:
   - Open `androidApp/src/main/res/xml/network_security_config.xml`.
   - Ensure your IP is listed under the `<domain-config>` to allow cleartext traffic:
     ```xml
     <domain includeSubdomains="true">192.168.100.96</domain>
     ```

---

## 🔗 Backend Integration

This app connects to the [Fintrack Ktor Backend](https://github.com/markgithinji/Fintrack-ktor) which provides:
- **RESTful API** for all financial operations.
- **JWT Authentication** with secure token management.
- **PostgreSQL Database** for data persistence.
- **Smart Recurring Engine**: Automated pattern detection for recurring payments and bill prediction.

---

## 🤝 Contribution & Feedback

Fintrack KMP is a showcase of what modern Kotlin Multiplatform can achieve. If you're interested in how we handle SMS parsing in KMP or professional-grade financial logic, feel free to explore the code!

---

**Built with ❤️**
