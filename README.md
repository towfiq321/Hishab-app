# Hishab (হিসাব) — Personal Finance Tracker

A complete, offline-first Android personal finance app built with Kotlin and Jetpack
Compose. Track daily income and expenses in Bangladeshi Taka (৳), see daily/weekly/monthly
reports, manage income sources and recurring bills, set category budgets, and get an
**expense-only** forecast for next month based on your own spending history.

---

## ⚠️ Important: this build environment could not compile the app

The environment this project was written in has **no Android SDK, no Gradle, no Kotlin
compiler, and no network access** — only `java`, `python3`, and `zip`. That means:

- Every file in this project was hand-written to compile against the pinned dependency
  versions below, and was checked line-by-line for consistent types and signatures
  across files (repositories, ViewModels, and screens were all cross-referenced against
  each other as they were written).
- **It was not run through `gradlew build`, and no APK was produced**, because those
  tools do not exist here. There is a real chance a small thing — a typo, an import, an
  API used slightly wrong — needs a one-line fix the first time you build it in Android
  Studio.
- What **was** independently verified: the forecasting and reporting math. The core
  algorithm in `ForecastEngine.compute()` and the demo-data generator in `Seeder.kt` were
  ported to Python and run against the same 4 months of generated demo data (see
  *Forecast validation* below). The numbers check out.

So: open this in Android Studio, let Gradle sync, and fix anything it flags. Given the
project's size (~50 Kotlin files) that is a realistic and normal step, not a sign
something is fundamentally wrong — treat the first sync/build the way you would for any
project you're integrating for the first time.

---

## Getting it running

1. **Unzip** the project and open the root folder (`Hishab/`) in **Android Studio**
   (Koala/2024.1 or newer recommended, for Kotlin 2.0 + Compose Compiler plugin support).
2. Let Gradle sync. It will download the Android Gradle Plugin, Kotlin, and all
   dependencies listed below — this needs network access.
3. If Android Studio's bundled Gradle/AGP doesn't match the pinned versions
   (`gradle/libs.versions.toml`), accept its offer to upgrade the Gradle wrapper, or edit
   the versions there to match your installed SDK/AGP. The project has **no wrapper jar**
   checked in (this environment can't fetch or generate one) — Android Studio will
   generate `gradle/wrapper/gradle-wrapper.jar` automatically on first sync, or run
   `gradle wrapper` yourself if you have a system Gradle install.
4. Run the `app` configuration on an emulator or device (**minSdk 26 / Android 8.0+**).
5. On first launch the app seeds its default categories and income sources, then loads
   about four months of realistic demo data automatically, so every screen — dashboard,
   reports, forecast, budgets — has something to show immediately. You can wipe this from
   **More → Your data → Clear all transactions**, or reload it with **Load sample data**.

### Building an APK from the command line (once you have a JDK 17 + Android SDK set up)

```bash
# Debug APK
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# Release APK (unsigned; add a signing config in app/build.gradle.kts to sign it)
./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release-unsigned.apk
```

If `./gradlew` isn't executable or the wrapper jar is missing, run `gradle wrapper`
first (with any Gradle 8.x install) to generate it, or just use Android Studio's *Build →
Build Bundle(s) / APK(s) → Build APK(s)* menu, which sidesteps the wrapper entirely.

---

## Pinned versions

| Component | Version |
|---|---|
| Android Gradle Plugin | 8.5.2 |
| Kotlin | 2.0.21 |
| KSP | 2.0.21-1.0.25 |
| Compose BOM | 2024.10.01 |
| Room | 2.6.1 |
| Navigation Compose | 2.8.4 |
| Lifecycle | 2.8.7 |
| WorkManager | 2.9.1 |
| DataStore | 1.1.1 |
| Gradle wrapper | 8.9 |
| compileSdk / targetSdk | 34 |
| minSdk | 26 |
| JVM target | 17 |

If your local Android Studio ships a newer AGP/Kotlin pairing, bumping these in
`gradle/libs.versions.toml` should be safe — nothing in this codebase relies on
version-specific behaviour beyond standard, stable Compose/Room APIs.

---

## Architecture

Plain **MVVM**, with a **hand-written dependency container** (`AppContainer.kt`) instead
of Hilt/Dagger — this was a deliberate choice to keep the build simple and avoid another
KSP processor in an environment where nothing could be test-compiled. Swapping in Hilt
later only means replacing `AppContainer` and the `rememberViewModel` helper in
`ui/Locals.kt`; nothing else references a DI framework directly.

```
data/
  local/          Room entities, DAOs, the database, and the demo-data seeder
  repository/      One repository per concern: transactions, categories/sources,
                    budgets, recurring rules, settings (DataStore)
  backup/          JSON export/import of the entire database
  export/          CSV (Excel-readable) and PDF report generation
domain/
  ReportEngine     Turns a list of transactions + a date range into totals, daily
                    points, and category/source/method breakdowns — daily, weekly and
                    monthly reports are all this same function over a different window
  ForecastEngine   The expense forecasting model (see below)
notification/      Notification channels + a single daily WorkManager worker that
                    covers daily reminders, bill reminders, budget alerts, and the
                    monthly report
ui/
  theme/           Material 3 colour schemes (light + dark) plus a MoneyColors
                    CompositionLocal for the income/expense/forecast palette
  components/      Shared building blocks: cards, charts (custom Canvas — no charting
                    library dependency), breakdown rows, pickers
  navigation/       Single-Activity NavHost; a custom bottom bar with a centred
                    gradient "Add" button that opens a bottom sheet
  dashboard/ transactions/ entry/ reports/ sources/ budget/ recurring/ categories/ more/
                    One package per screen: a ViewModel exposing a StateFlow<...State>,
                    and a Composable that collects it
```

Every screen follows the same shape: `XScreen()` calls `rememberViewModel { XViewModel(it) }`
to get an instance scoped to that screen (backed by `AppContainer`, no DI framework
needed), collects its `StateFlow` with `collectAsStateWithLifecycle()`, and renders.

### Design choices worth knowing about

- **No charting library.** `DonutChart`, `GroupedBarChart`, `TrendLineChart`, and
  `ForecastAccuracyChart` in `ui/components/Charts.kt` are all plain `Canvas` drawing —
  no extra dependency, full control over the look.
- **No image/icon-pack dependency.** Categories and income sources use emoji as their
  icon, stored as a string column. Simple, always renders, fully customisable by the
  user without shipping an icon set.
- **CSV instead of a real `.xlsx` writer.** `ExportManager.exportCsv()` writes a UTF-8
  CSV with a BOM, which Excel (and Google Sheets) opens directly and renders Bangla text
  and the ৳ sign correctly. Adding a real `.xlsx` library was possible but felt like
  unnecessary weight for what Excel already opens natively.
- **PDF via `android.graphics.pdf.PdfDocument`**, not a PDF library — one more
  dependency avoided, and it's the standard platform API for exactly this.
- **Deleting a category or income source never deletes money.** Transactions that used
  it are detached (they show as "Uncategorised" / with no source) rather than removed;
  their budgets and recurring rules are cleaned up. See `CatalogRepository`.
- **Recurring rules can auto-post or stay reminder-only** (`autoPost` flag). An
  auto-posting rule writes its expense transaction the moment it falls due (checked on
  every app launch in `RecurringRepository.postDue()`), so rent and bills show up in
  reports without manual entry. A reminder-only rule just notifies you.

---

## The forecast — and why there's no income forecast

`ForecastEngine.compute()` estimates **next month's expenses only**. There is
intentionally no income prediction anywhere in the app: income here is salary, project
payments, and one-off work, which is inherently irregular, and a wrong predicted income
number is a genuinely dangerous thing to plan a budget against. Expenses, by contrast,
tend to repeat. The Forecast tab in Reports says this directly to the user.

The model, in plain terms:

1. **Recurring bills are priced exactly.** For every auto-posting recurring rule, the
   engine counts how many times it actually falls inside the target month (a weekly rule
   in a 5-week month occurs 5 times, not 4.33) and adds that up per category. This is
   exact, not averaged.
2. **Everything else is a recency-weighted average of the last six months**, per
   category, using only *non-recurring* transactions (so a manually-entered bill isn't
   averaged in on top of its own recurring rule and double-counted). More recent months
   are weighted more heavily.
3. **A part-month is scaled up.** If the engine is forecasting from partway through the
   current month, that month's partial total is scaled up to a full-month equivalent
   (capped at 3×) before being folded into the average, so an early-month forecast isn't
   dragged down by "the month isn't over yet."
4. **The range comes from volatility.** The coefficient of variation across the recent
   monthly totals sets the low–high band, clamped to a sensible 8%–28% so the range is
   never unrealistically tight or absurdly wide.
5. **Confidence** is reported as Low/Medium/High based on how many months of history
   exist and how volatile spending has been.

The Forecast tab also shows a **backtest**: for each of the last several months, it
reconstructs what the model would have predicted using only data available *before* that
month started, and compares it to what actually happened. That's the "Forecast vs
actual" chart and table.

### Forecast validation

`ForecastEngine.compute()` is a pure function (no Android/Room types), so it was ported
to Python and run against the same demo-data generator used in `Seeder.kt` — 725
transactions across May–September 2026 — to sanity-check the math before it went in the
app:

- Forecast for the next month: **৳61,450**, range **৳56,500 – ৳66,500**, based on 5
  months of history, volatility 0.080 (i.e. fairly stable spending).
- Backtesting the model against three prior actual months gave errors of **−5.1%,
  +10.8%, +2.1%** — a mean absolute error of **6.0%**.
- Structural checks all passed: category forecast lines sum to the reported total, the
  low/high range always brackets the expected value, and the range stays within a
  sensible 0.75×–1.35× band of the point estimate.

This confirms the *algorithm* is sound. It does not substitute for compiling and running
the actual Kotlin — see the warning at the top of this file.

---

## Feature map (spec → code)

| Spec area | Where it lives |
|---|---|
| Daily expense & income tracking | `ui/entry/` (add/edit), `data/local/entity/TransactionEntity` |
| Daily / weekly / monthly reports | `domain/ReportEngine`, `ui/reports/ReportsScreen` (Weekly & Monthly tabs) |
| Expense forecasting (no income forecast) | `domain/ForecastEngine`, `ui/reports/ReportsScreen` (Forecast tab) |
| Income source management | `data/local/entity/IncomeSourceEntity`, `ui/sources/` |
| Budgets | `data/repository/BudgetRepository`, `ui/budget/BudgetScreen` |
| Recurring expenses | `data/local/entity/RecurringRuleEntity`, `data/repository/RecurringRepository`, `ui/recurring/RecurringScreen` |
| Search & filter | `TransactionRepository.TransactionFilter`, `ui/transactions/TransactionsScreen` |
| Backup (JSON) | `data/backup/BackupManager`, wired up in `ui/more/MoreScreen` |
| CSV / PDF export | `data/export/ExportManager`, wired up in `ui/more/MoreScreen` |
| Notifications (daily reminder, bill reminders, budget alerts, monthly report) | `notification/`, toggles in `ui/more/MoreScreen` |
| Light / dark mode | `ui/theme/Theme.kt`, mode picker in `ui/more/MoreScreen` |
| Categories (custom, emoji + colour) | `ui/categories/CategoriesScreen` |
| BDT currency formatting | `core/Money.kt` |
| Bottom navigation (Dashboard / Transactions / Add / Reports / More) | `ui/navigation/HishabRoot.kt` |

---

## Demo data

`Seeder.kt` generates roughly four months of realistic transactions (seeded random, so
it's the same every install) across all default categories and income sources, plus a
few recurring rules and two months of budgets, so a fresh install already has reports,
budgets, and a forecast to look at rather than empty screens. This can be cleared or
reloaded from **More → Your data**.
