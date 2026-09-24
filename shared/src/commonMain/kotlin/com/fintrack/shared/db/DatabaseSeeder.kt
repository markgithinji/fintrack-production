package com.fintrack.shared.db

import com.fintrack.shared.feature.core.util.randomUUID
import com.fintrack.shared.feature.core.util.DateTimeHelper
import com.fintrack.shared.feature.category.domain.model.*
import kotlinx.datetime.Instant

class DatabaseSeeder(private val database: FintrackDatabase) {
    private val queries = database.fintrackDatabaseQueries
    private val offlineUserId = "offline_user"

    fun seedIfEmpty() {
        try {
            queries.createCategoryRuleTableIfNotExists()
            seedAccounts()
            seedCategories()
            seedCategoryRules()
        } catch (e: Exception) {
            // Prevent initialization crash if seeding fails (e.g. schema mismatch)
            println("DatabaseSeeder error: ${e.message}")
        }
    }

    private fun seedAccounts() {
        val accounts = queries.selectAllAccounts(offlineUserId).executeAsList()
        if (accounts.isEmpty()) {
            val defaultAccounts = listOf(
                AccountData("Mpesa", "MPESA", "2024-01-01T00:00:00Z", listOf("mpesa")),
                AccountData("Bank", "BANK", "2024-01-01T00:00:01Z"),
                AccountData("Wallet", "WALLET", "2024-01-01T00:00:02Z"),
                AccountData("Savings", "SAVINGS", "2024-01-01T00:00:03Z"),
                AccountData("Cash", "CASH", "2024-01-01T00:00:04Z")
            )

            defaultAccounts.forEach { account ->
                queries.insertAccount(
                    id = randomUUID(),
                    userId = offlineUserId,
                    name = account.name,
                    isDefault = 1L,
                    type = account.type,
                    balance = "0",
                    linkedSources = account.linkedSources,
                    createdAt = Instant.parse(account.createdAt),
                    lastSyncedAt = null
                )
            }
        }
    }

    private fun seedCategories() {
        val categories = queries.selectAllCategories(offlineUserId).executeAsList()
        if (categories.isEmpty()) {
            Category.allCategories.forEach { category ->
                queries.insertCategory(
                    id = category.id,
                    userId = offlineUserId,
                    name = category.name,
                    isExpense = if (category.isExpense) 1L else 0L,
                    iconName = category.iconName ?: "default",
                    isDefault = if (category.isDefault) 1L else 0L,
                    createdAt = DateTimeHelper.now()
                )
            }
        }
    }

    private fun seedCategoryRules() {
        val rulesResult = try { queries.selectAllCategoryRules(offlineUserId).executeAsList() } catch (e: Exception) { emptyList() }
        if (rulesResult.isEmpty()) {
            val globalRules = mutableListOf<Pair<String, String>>()
            
            queries.transaction {
                // Utilities
                listOf("kplc", "tokens", "power", "jajemelo", "water", "sewerage", "ncwsc", "kiwasco", "mawasco", "nyewasco", "eldowas", "mowasco", "gas", "m-gas", "mgas", "afrigas", "garbage", "waste", "trash", "m-kopa", "mkopa", "d.light", "sunking", "bboxx", "Nairobi Water").forEach {
                    globalRules.add(it to Category.Utilities.id)
                }
                
                // Internet
                listOf("zuku", "safaricom home", "poa internet", "vilcom", "faiba", "jtl", "jtlk", "wananchi", "mawingu", "starlink", "konnect", "fibre connect", "fiber connect", "airtel fibre", "telkom home", "liquid home", "liquid telecom", "data bundles", "data bundle", "offers", "tunukiwa", "internet", "bundles").forEach {
                    globalRules.add(it to Category.Internet.id)
                }
                
                // Airtime
                listOf("airtime", "tingg", "top up").forEach {
                    globalRules.add(it to Category.Airtime.id)
                }
                
                // Groceries
                listOf("supermarket", "naivas", "carrefour", "quickmart", "butchery", "quick mart", "friendly 5", "slice city", "memento butchery", "Chandarana").forEach {
                    globalRules.add(it to Category.Groceries.id)
                }
                
                // Dining Out
                listOf("restaurant", "cafe", "kfc", "java", "lounge", "chicken inn", "pizza inn", "creamy inn", "choma place", "nas n001", "caterers", "dishes", "glovo", "uber eats", "bolt food", "jumia food", "eat", "delivery").forEach {
                    globalRules.add(it to Category.DiningOut.id)
                }
                
                // Bank
                listOf("equity", "co-operative", "bank", "i&m", "ncba", "boa", "family bank", "stanbic", "loop", "sidian").forEach {
                    globalRules.add(it to Category.Bank.id)
                }
                
                // Loans
                listOf("loan repayment", "loan", "fuliza", "tala", "branch").forEach {
                    globalRules.add(it to Category.Loans.id)
                }
                
                // Savings
                listOf("m-shwari saving", "mshwari saving", "m-shwari", "mshwari", "kcb", "sacco", "chama", "orokise", "zimele", "etica", "gulfcap", "cytonn", "arvocap", "lofty", "kuza", "mali", "ziidi", "kasha", "genghis", "hela imara", "nabo capital", "stima sacco", "police sacco", "unaitas", "mwalimu", "harambee", "kimisitu", "hazina sacco", "imarisha", "tower sacco", "waumini", "dry associates", "m-pesa saving", "money market", "fund", "asset", "mmf").forEach {
                    globalRules.add(it to Category.Savings.id)
                }
                
                // Charity
                listOf("tithe", "offering", "citam", "church", "charity", "mosque", "prayer mountain").forEach {
                    globalRules.add(it to Category.Charity.id)
                }
                
                // Transport
                listOf("parking", "kaps", "bolt", "uber", "taxi", "rubis", "totalenergies", "shell", "little", "wasili", "indriver").forEach {
                    globalRules.add(it to Category.Transport.id)
                }
                
                // Health
                listOf("chemist", "pharmacy", "hospital", "clinic", "health", "meds", "dental", "hopemed", "medical").forEach {
                    globalRules.add(it to Category.Health.id)
                }
                
                // Subscriptions
                listOf("netflix", "spotify", "showmax", "youtube", "Prime", "OpenAI", "ChatGPT").forEach {
                    globalRules.add(it to Category.Subscriptions.id)
                }
                
                // Shopping
                listOf("jumia", "leather", "watches", "perfume", "clothes", "fashion", "mrp", "miniso", "woolworths", "tushop", "m-pesa card", "canva", "pdfaid").forEach {
                    globalRules.add(it to Category.Shopping.id)
                }
                
                // Personal Care
                listOf("salon", "barber", "beauty", "nail bar").forEach {
                    globalRules.add(it to Category.PersonalCare.id)
                }
                
                // Maintenance
                listOf("hardware", "timber", "maintenance", "repair").forEach {
                    globalRules.add(it to Category.Maintenance.id)
                }
                
                // Government
                listOf("e-citizen", "kra", "county").forEach {
                    globalRules.add(it to Category.Government.id)
                }
                
                // Insurance
                listOf("britam", "nhif", "shif", "insurance", "apa", "jubilee", "sanlam", "cic", "old mutual", "icea lion", "madison", "apollo", "ga insurance", "heritage", "geminia", "pioneer", "kenindia", "uap").forEach {
                    globalRules.add(it to Category.Insurance.id)
                }

                // Income
                globalRules.add("salary" to Category.Salary.id)
                globalRules.add("bonus" to Category.Bonus.id)
                globalRules.add("interest" to Category.Interest.id)
                globalRules.add("commission" to Category.OtherIncome.id)
                globalRules.add("income" to Category.OtherIncome.id)

                globalRules.forEach { (keyword, categoryId) ->
                    val isExpense = categoryId.startsWith("00000000")
                    queries.insertCategoryRule(
                        id = randomUUID(),
                        userId = null, // Global rule
                        keyword = keyword,
                        categoryId = categoryId,
                        isExpense = if (isExpense) 1L else 0L
                    )
                }
            }
        }
    }

    private data class AccountData(
        val name: String,
        val type: String,
        val createdAt: String,
        val linkedSources: List<String> = emptyList()
    )
}
