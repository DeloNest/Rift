<div dir="rtl">

# plan.md — مشروع Launcher ذكي ومحسّن لأندرويد (الاسم المؤقت: Orbit)

> **آخر تحديث:** 16 نوفمبر 2026 · **الجهاز المرجعي:** Samsung Galaxy S22 Ultra (Android 16 / One UI 8) · **الهوية البصرية:** Material 3 Expressive
>
> **مفتاح الرموز:** ✅ تم التحقق منه من مصدر رسمي/موثوق (نوفمبر 2026) · ⚠️ يحتاج تحقق عملي قبل الالتزام به · 🎯 هدف قياسي يُقاس ولا يُفترض

---

## 0. الملخص التنفيذي

**Orbit** هو Launcher (شاشة رئيسية بديلة) مبني بالكامل بـ **Kotlin + Jetpack Compose** وبهوية **Material 3 Expressive**، هدفه الأول: **أخف وأسرع launcher ممكن** على الأجهزة الحقيقية، مع **دعم كامل للودجت (Widgets)** و**ذكاء يعمل محليًا على الجهاز** بدون إنترنت وبدون إرسال أي بيانات.

### القرارات الكبرى (ملخص ADR)

| # | القرار | البديل المرفوض | السبب |
|---|--------|----------------|-------|
| 1 | بناء من الصفر بـ Compose، مع **الاستفادة من أفكار** AOSP Launcher3 | Fork من Lawnchair/Launcher3 | تحكم كامل بالأداء والذاكرة؛ تراخيص Lawnchair/Kvaesitso هي GPL فلا يجوز نسخ كودها لمشروع مغلق |
| 2 | **وحدة Activity واحدة + عملية (process) واحدة** | عدة processes / Services دائمة | كل process إضافي = ذاكرة + طاقة |
| 3 | الودجت تُستضاف عبر `AppWidgetHost` داخل `AndroidView` | إعادة رسم الودجت يدويًا | هذه هي الطريقة الوحيدة المدعومة من النظام |
| 4 | **بدون صلاحية INTERNET** في الإصدار 1.0 | تحليلات/إعلانات/Feed | خصوصية + بطارية + ثقة المستخدم |
| 5 | **minSdk 26** (Android 8.0) | minSdk 21/23 | يغطي الغالبية الساحقة، ويوفر Adaptive Icons و Pinned Shortcuts دون كود legacy |
| 6 | الذكاء = **Heuristics + نماذج صغيرة جدًا** أولًا | الاعتماد على Gemini Nano / LLM | Gemini Nano غير متاح على كل الأجهزة (انظر 7.6) |
| 7 | Room + Proto DataStore + Hilt | Realm / Koin / SharedPreferences | Hilt حل compile-time بلا reflection عند الإقلاع |
| 8 | لا SDKs خارجية (Firebase/Analytics) | Firebase Crashlytics | حجم + خصوصية؛ نكتفي بـ Android vitals و `ApplicationExitInfo` |

### المعالم الزمنية (تقدير لمطورَين)

`المرحلة 0 (أسبوعان)` Spikes → `المرحلة 1 (6 أسابيع)` MVP → `المرحلة 2 (5 أسابيع)` الودجت الكاملة → `المرحلة 3 (6 أسابيع)` الذكاء والتخصيص → `المرحلة 4 (4 أسابيع)` الصقل والتوافق → `المرحلة 5 (3 أسابيع)` Beta والإطلاق ≈ **26 أسبوعًا** للإصدار 1.0.

---

## 1. الرؤية والأهداف

### 1.1 الرؤية
> شاشة رئيسية تشعر أنها "جزء من النظام": فورية، هادئة، قابلة للتخصيص بعمق، وتتعلم عادات المستخدم **محليًا** دون أن تستهلك بطاريته أو ذاكرته.

### 1.2 الأهداف القابلة للقياس
1. **سرعة:** عودة للشاشة الرئيسية خلال ≤ 120ms (P95) والتمرير بدون jank على 120Hz. 🎯
2. **خفة:** استهلاك ذاكرة في الوضع الخامل أقل من أغلب launchers المنافسة، وبطارية "صفرية" في الخلفية. 🎯
3. **ودجت كاملة:** استضافة أي ودجت من أي تطبيق (بما فيها ودجت Samsung/Google) + ودجت خاصة بنا بـ Glance.
4. **ذكاء خصوصي:** اقتراحات تطبيقات وبحث موحّد يعملان 100% على الجهاز.
5. **هوية Material 3 Expressive:** ألوان ديناميكية، حركة Spring، أشكال متحولة (Shape morphing).
6. **توافق واسع:** من Android 8.0 إلى Android 17، هواتف/طيّات/تابلت، ومع كل الـ OEMs الكبرى.

### 1.3 خارج النطاق (Non-goals) في 1.0
- Google Discover Feed / Samsung Free (يتطلبان تكاملًا حصريًا مع النظام).
- استبدال شاشة Recents أو أنيميشن الانتقال المتزامن مع إيماءات النظام (هذه ملك الـ launcher الافتراضي للنظام) ⚠️ تُؤكَّد في Spike رقم 4.
- DeX كتجربة مخصصة، Wear OS، Android TV، Android Auto.
- حساب مستخدم/سحابة/مزامنة (الإصدار 1.0 محلي بالكامل؛ النسخ الاحتياطي عبر ملف + Auto Backup).

---

## 2. المستخدمون والمعايير

| Persona | الحاجة | ما يهمه |
|---------|--------|---------|
| **المستخدم اليومي (S22 Ultra)** | شاشة كبيرة 6.8"، وصول سريع بيد واحدة | بحث في الأسفل، إيماءات، ودجت كبيرة، دعم S Pen |
| **المخصِّص (Power user)** | تحكم بالشبكة والأيقونات والإيماءات | Icon packs، Grid مرن، نسخ احتياطي |
| **جهاز متوسط/ضعيف** | سلاسة على 3–4GB RAM | "Lite mode" تلقائي، بلا blur ثقيل |
| **الخصوصية أولًا** | لا تتبع | لا إنترنت، صلاحيات اختيارية فقط |

**معيار النجاح (Definition of Success):** تقييم ≥ 4.5 على Play، Crash-free users ≥ 99.7% 🎯، ANR أقل بكثير من حدود Android vitals، ونجاح جميع بوابات الأداء في القسم 8.

---

## 3. حقائق المنصة (الوضع في نوفمبر 2026)

| البند | الحالة | ملاحظة |
|-------|--------|--------|
| Android 17 (API 37) | ✅ مستقر منذ يونيو 2026 (Pixel أولًا) | QPR1 وصل Platform Stability عند Beta 6، وآخر Beta 9 بتاريخ 17 أغسطس 2026 |
| **حدود ذاكرة لكل تطبيق** | ✅ جديدة في Android 17 | مبنية على إجمالي RAM، وتنطبق على كل التطبيقات مهما كان targetSdk؛ تقتل التسريبات الشديدة. **أثرها علينا كبير لأن الـ launcher يعيش دائمًا** |
| Google Play target API | ✅ التطبيقات الجديدة والتحديثات يجب أن تستهدف **API 36 أو أعلى** منذ 31 أغسطس 2026 | التطبيقات القائمة يجب ≥ 35 لتظل متاحة للمستخدمين الجدد على أجهزة أحدث |
| Play Billing Library | ✅ 8.0.0+ مطلوبة منذ 31 أغسطس 2026 | فقط إذا اعتمدنا نموذج Premium |
| Compose 1.12 (BOM `2026.08.00`) | ✅ يتطلب `compileSdk 37` و **AGP ≥ 9.1.1** | ابدأ بالتوافق من اليوم الأول |
| Material3 | ✅ `1.4.0` مستقر، و`1.5.0-beta01` بتاريخ 7 أكتوبر 2026 | ⚠️ مصدر ثالث يذكر أن معظم واجهات Expressive أصبحت مستقرة من `1.5.0-alpha23`؛ تحقق من آخر إصدار قبل التثبيت |
| Jetpack Glance | ✅ `1.2.0` مستقر (26 أغسطس 2026) و`1.3.0-alpha02` | لبناء **ودجت التطبيق نفسه** (وليس لاستضافة ودجت الآخرين) |
| Gemini Nano | ✅ عبر ML Kit GenAI + AICore | ⚠️ يُسمح بالاستدعاء من التطبيق الأمامي فقط حسب التوثيق (أغسطس 2026)؛ **دعم S22 Ultra غير مؤكد** → قرار التصميم: لا نعتمد عليه |
| S22 Ultra | ✅ One UI 8 (Android 16) هو آخر تحديث رئيسي | ⚠️ وصول One UI 8.5 غير مؤكد في المصادر (متضارب) |

### 3.1 بروفايل الجهاز المرجعي: Galaxy S22 Ultra
- شاشة **6.8" QHD+ (3088×1440)** LTPO **1–120Hz**، حواف منحنية (تحتاج **هوامش إيماءات** لتجنب اللمس العرضي)، ثقب كاميرا وسطي (Display cutout).
- **Exynos 2200 أو Snapdragon 8 Gen 1** حسب المنطقة، 8/12GB RAM.
- **S Pen** (أحداث Hover و`TOOL_TYPE_STYLUS`).
- Android 16 → **اختبار الـ target الأساسي هو API 36**.
- الدقة الافتراضية غالبًا FHD+ (يغيّرها المستخدم إلى QHD+)، لذلك **كل الأحجام بالـ dp وكاش الأيقونات يُحسب بحسب density الفعلي**.

### 3.2 ما لا يستطيع أي launcher من طرف ثالث فعله ⚠️
- شاشة Recents وإيماءات التنقل تبقى للنظام (على Samsung: One UI Home يملك Recents).
- لا Google Feed ولا Samsung Free.
- لا قراءة Layout الخاص بـ One UI Home (محمي بصلاحية signature) → الاستيراد عبر ملف Orbit فقط.
- لا فتح لوحة الإشعارات بإيماءة سحب لأسفل **بدون حل بديل** (انظر 7.7).

---

## 4. المتطلبات

### 4.1 الوظيفية (FR)

**الشاشة الرئيسية والشبكة**
- FR-1: صفحات متعددة (`HorizontalPager`)، شبكة قابلة للضبط من 4×5 حتى 8×9، **Dock** من 4–6 عناصر.
- FR-2: عناصر: تطبيق، اختصار (Shortcut)، مجلد، ودجت. سحب وإفلات مع Spring physics.
- FR-3: قفل التخطيط، إخفاء تطبيقات، إعادة ترتيب بالإيماءات، **RTL كامل** (اتجاه الصفحات والمؤشرات).
- FR-4: Pull-down/Up configurable، مؤشر صفحات M3E.

**درج التطبيقات (App Drawer)**
- FR-5: قائمة أبجدية عربية/لاتينية + scroller سريع، تصنيفات تلقائية، صف "مقترحات" وصف "الأخيرة".
- FR-6: تطبيقات مخفية/مقفلة ببصمة (`BiometricPrompt`).
- FR-7: دعم **Work profile / Secure Folder / Private Space** مع شارة (badge) وتبويب.

**الودجت** (تفصيل في 7.4)
- FR-8: استضافة كاملة لأي ودجت + Picker احترافي + تغيير الحجم + إعادة الإعداد + استعادة.
- FR-9: ودجت خاصة بنا بـ Glance: ساعة + تاريخ هجري، مقترحات، بحث، حالة الجهاز.

**البحث والذكاء**
- FR-10: بحث موحّد (تطبيقات، اختصارات، جهات اتصال*، إعدادات النظام، حاسبة/تحويل وحدات) بـ fuzzy matching يفهم العربية.
- FR-11: اقتراحات تطبيقات بحسب السياق (وقت/يوم/شحن/سماعات/آخر تطبيق).
- FR-12: تصنيف تلقائي للتطبيقات، ووضع تركيز (Focus mode)*.

**المظهر**
- FR-13: Dynamic Color، **Themed icons** (monochrome Android 13+)، Icon packs، أشكال أيقونات من `MaterialShapes`، Dark/Light/Auto.
- FR-14: Blur للخلفيات عند التوفر، ظلال/حركة Expressive، تأثير parallax للخلفية.

**النظام**
- FR-15: طلب أن يصبح Home الافتراضي (`RoleManager.ROLE_HOME`)، Onboarding قصير.
- FR-16: نسخ احتياطي/استعادة (Auto Backup + تصدير/استيراد JSON)، تحديث آمن عند تثبيت/حذف/أرشفة التطبيقات (Archived apps في Android 15+).
- FR-17: نقاط الإشعارات (Notification dots)* اختيارية.

(* = تحتاج صلاحية اختيارية يمنحها المستخدم لاحقًا)

### 4.2 غير الوظيفية (NFR)
- NFR-1 أداء: القسم 8. · NFR-2 خصوصية: لا شبكة، لا تتبع. · NFR-3 توافق: القسم 9.
- NFR-4 وصولية: TalkBack كامل، خط 200%، Reduce motion. · NFR-5 حجم: AAB ≤ 8MB 🎯.
- NFR-6 قابلية الصيانة: وحدات واضحة، تغطية اختبارات للمنطق ≥ 80%، لا `@Suppress` بلا سبب.

---

## 5. الهوية البصرية — Material 3 Expressive

### 5.1 المبادئ
1. **اللون:** `dynamicColorScheme` من خلفية المستخدم (على Samsung تُغذّيه "لوحة الألوان" One UI). Fallback ثابت لما دون Android 12.
2. **الحركة:** `MotionScheme.expressive()` — نوابض (Springs) بدل منحنيات زمنية؛ **تُستخدم في فتح الدرج، السحب، المجلدات، الودجت**.
3. **الأشكال:** Shape morphing بين الأيقونة والمجلد وبين الزر الدائري والكبسولة بـ `MaterialShapes` / `Morph`.
4. **الخط:** `FontFamily.Default` (لا نحزم خطوطًا)؛ يوفر حجم ومطابقة لخط النظام العربي (SamsungOne/Roboto).
5. **الكثافة:** أهداف لمس ≥ 48dp، تباعد 8dp grid.

### 5.2 أبرز المكونات
| الاستخدام | المكوّن |
|-----------|---------|
| شريط البحث | `SearchBar` / `AppBarWithSearch` (توسّع Full-screen) |
| مقترحات | `HorizontalUncontainedCarousel` أو Row بسيط |
| إعدادات الأزرار | `ButtonGroup`, `ToggleButton`, `SplitButton` |
| التحميل/الفهرسة | `LoadingIndicator` (Expressive) |
| قوائم الإعدادات | `ListItem` مجمّعة (segmented) + `Switch` |
| Picker الودجت/الأيقونات | `ModalBottomSheet` + `FloatingToolbar` للتحكم أثناء التحرير |
| التنقل في الإعدادات | Navigation 3 (⚠️ تحقق من الإصدار) أو state بسيط |

### 5.3 مثال الثيم
```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OrbitTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val dark = isSystemInDarkTheme()
    val scheme = when {
        Build.VERSION.SDK_INT >= 31 && dark -> dynamicDarkColorScheme(ctx)
        Build.VERSION.SDK_INT >= 31 -> dynamicLightColorScheme(ctx)
        dark -> OrbitFallbackDark
        else -> OrbitFallbackLight
    }
    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        shapes = OrbitShapes,
        typography = OrbitTypography,
        content = content,
    )
}
```
> ملاحظة: على أجهزة الذاكرة المنخفضة نستخدم `MotionScheme.standard()` لتقليل تكلفة الحركة.

### 5.4 Edge-to-edge والـ Predictive Back
- `enableEdgeToEdge()` إلزامي لما بعد API 35؛ ندير `WindowInsets` يدويًا (ثقب الكاميرا + شريط الإيماءات + **منطقة حواف S22 Ultra المنحنية**).
- Predictive Back: `BackHandler`/`PredictiveBackHandler` لإغلاق الدرج/البحث/المجلد/وضع التحرير. ⚠️ تأكد من سلوك target 36 الافتراضي في Spike.

---

## 6. المعمارية

### 6.1 الطبقات
```
┌──────────────────────────── :app ─────────────────────────────┐
│  HomeActivity (single)  ·  Hilt graph  ·  Nav / Role request  │
├───────────────────────────────────────────────────────────────┤
│  :feature:home  :feature:drawer  :feature:search  :feature:widgets │
│  :feature:folders  :feature:settings  :feature:onboarding         │
│  :feature:smart (engine + suggestions)  :feature:gestures         │
├───────────────────────────────────────────────────────────────┤
│  :core:designsystem (Theme, M3E, Shapes)  :core:ui (Modifiers,    │
│  DnD, Grid layout)  :core:icons (pipeline+cache)  :core:perf     │
├───────────────────────────────────────────────────────────────┤
│  :data:apps (LauncherApps)  :data:workspace (Room)               │
│  :data:widgets (AppWidgetHost)  :data:settings (Proto DataStore) │
│  :data:usage (launch events, UsageStats optional)                │
└───────────────────────────────────────────────────────────────┘
  :benchmark (Macrobenchmark)   :baselineprofile   build-logic/ (convention plugins)
```
- **MVI/UDF:** كل شاشة = `State` غير قابلة للتغيير + `Intent/Event` + `ViewModel` يستخدم `StateFlow`. التحويلات الثقيلة خارج الـ Main.
- **لا over-modularization:** ندمج الميزات الصغيرة في وحدة واحدة حتى تتأكد الحاجة (كل وحدة تزيد زمن البناء).
- **Convention plugins** في `build-logic/` + **Version Catalog** (`libs.versions.toml`).

### 6.2 تدفق البيانات (الإقلاع السريع)
1. `onCreate`: يرسم **Snapshot مخزّنًا** (Layout + أسماء + أيقونات الصفحة الأولى) من Room/Proto → أول إطار.
2. في الخلفية: مطابقة (`reconcile`) مع `LauncherApps.getActivityList()` → إصدار الفروقات فقط (Diff) إلى `StateFlow`.
3. `LauncherApps.Callback` يراقب التثبيت/الحذف/التحديث **ما دامت العملية حية**؛ عند إعادة إنشاء العملية نقارن بالـ snapshot (hash/`lastUpdateTime`).
4. كل الـ collectors بـ `repeatOnLifecycle(STARTED)` → **لا عمل والشاشة مطفأة**.

### 6.3 قواعد حاكمة
- لا `Service` دائمة، لا `AlarmManager` دوري، لا `WorkManager` دوري. (WorkManager فقط لمهام نادرة: إعادة بناء كاش الأيقونات، تصدير نسخة).
- لا reflection في المسار الحرج؛ `kotlinx.serialization` بدل Gson.
- لا مكتبات native (يريح متطلب 16KB page size ويقلل المخاطر) ⚠️ إن أُضيفت فيجب أن تكون محاذاة 16KB.
- لا تهيئة في `Application.onCreate` عدا الضروري (Hilt component، StrictMode في debug). كل شيء آخر **Lazy**.
- عطّل مُهيئات `androidx.startup` غير اللازمة عبر `tools:node="remove"`.

---

## 7. تصميم الأنظمة الفرعية

### 7.1 الشاشة الرئيسية (Workspace)

**نموذج البيانات (Room)**
```kotlin
@Entity(tableName = "workspace_item", indices = [Index("container", "page")])
data class WorkspaceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: Int,            // APP, SHORTCUT, FOLDER, WIDGET
    val container: Long,      // DESKTOP / DOCK / folderId
    val page: Int,
    val cellX: Int, val cellY: Int, val spanX: Int, val spanY: Int,
    val componentName: String?,   // "pkg/cls"
    val userSerial: Long,         // work profile / secure folder
    val shortcutId: String?,
    val appWidgetId: Int?,        // لا يُنقل بين الأجهزة (انظر 7.4)
    val widgetProvider: String?,  // للاستعادة
    val title: String?,
    val rank: Int,
)
```
- التخطيط خوارزمية **Grid Occupancy** (مصفوفة bits) للتحقق من الاصطدام وإيجاد أقرب خلية فارغة.
- الحفظ **debounced** (≈300ms) وعلى `Dispatchers.IO`، مع معاملات (transactions).
- Layout مخصص `Layout {}` بدل `LazyLayout` للصفحة الرئيسية (عناصر قليلة وموضعها مطلق)؛ `HorizontalPager` مع `beyondViewportPageCount = 1`.
- **التحريك:** قراءة الإزاحة داخل `graphicsLayer {}` / `offset {}` (lambda) لتجنب إعادة التركيب (recomposition) أثناء السحب.

### 7.2 درج التطبيقات
- `LazyVerticalGrid` مع `key = componentKey` و`contentType`.
- الأيقونة تُرسم عبر `drawBehind`/`Canvas` من `ImageBitmap` جاهز في الكاش (بدون تخصيص كائنات أثناء التمرير).
- **Fast scroller أبجدي** يدعم العربية (أ ب ت …) واللاتينية و`#`.
- فتح الدرج بـ `AnchoredDraggable` + Spring Expressive؛ خلفية Blur فقط أثناء الفتح الكامل ويُتخلى عنها عند ضعف الجهاز.
- **التصنيف:** `ApplicationInfo.category` (CATEGORY_GAME/SOCIAL/…) مجانيًا، + قواعد اسم الحزمة، + (اختياري لاحقًا) نموذج صغير.
- مساحات: **Private Space** و**Work/Secure Folder** عبر `LauncherApps.getProfiles()` ⚠️ (صلاحيات الـ Private Space وشروطها تُتحقق في Spike رقم 5؛ مشروع Kvaesitso يدعم خيار إخفاء Private Space كمرجع عملي).

### 7.3 خط أنابيب الأيقونات (أهم نقطة أداء)
```
LauncherActivityInfo ──► getIcon(density) ──► Adaptive/Themed/IconPack mapper
        │                                         │
        ▼                                         ▼
   رندرة بحجم الهدف  ──►  LRU ذاكرة (Hardware Bitmap)  ◄──►  كاش قرص WebP (lossless)
```
- الحجم = `iconSizeDp × density` ويُخزَّن **مرة واحدة** لكل (حزمة، مستخدم، شكل، ثيم، حزمة أيقونات).
- ذاكرة: LRU **بالبايت** (≈ 6–10MB 🎯) لأيقونات المرئي والقريب فقط؛ الباقي من القرص عند الحاجة؛ `onTrimMemory` يقلّصه.
- مثال تقديري: 300 أيقونة بحجم ≈160px تساوي ≈ 30MB لو أُبقيت كلها في الذاكرة → **لذلك لا نفعل**.
- Themed icons (Android 13+): `AdaptiveIconDrawable.getMonochrome()` + tint من ColorScheme.
- أيقونات Work profile: `LauncherActivityInfo.getBadgedIcon(density)`.
- إبطال الكاش عند: تحديث الحزمة، تغيير الشكل/الثيم/الحجم، تغيير Icon pack.

### 7.4 الودجت — دعم كامل

#### أ) الاستضافة (Host) — **جوهر "دعم الودجت" في الـ Launcher**
> Glance **لا** يستضيف ودجت الآخرين؛ هو فقط لبناء ودجتنا. الاستضافة = `AppWidgetHost` + `AppWidgetHostView`.

**التدفق حسب التوثيق الرسمي ✅:**
1. تعريف `BIND_APPWIDGET` في الـ Manifest (خطوة أولى فقط).
2. `HOST_ID` ثابت ومُخزّن (hardcoded) داخل الحزمة.
3. `allocateAppWidgetId()` لكل نسخة ودجت، وتُخزَّن حالتها (الحجم/الموقع) مفتاحها هذا الـ ID.
4. `bindAppWidgetIdIfAllowed()` → إن رجع `false` نعرض حوار النظام `ACTION_APPWIDGET_BIND` (مرة/دائمًا).
5. إن وُجد Configure Activity وغير اختياري (ليس `configuration_optional` + `reconfigurable`) نشغّله ونعالج الإلغاء/الفشل (نحذف الـ ID).
6. نعرض الودجت بحجم لا يقل عن الافتراضي: بالخلايا إن وُجد `targetCellWidth/Height` (Android 12+) وإلا بالـ dp من `minWidth/minHeight`.
7. عند التغيير: `updateAppWidgetOptions()`/`updateAppWidgetSize()` → ينتج `onAppWidgetOptionsChanged()` عند المزوّد؛ نمرّر قائمة `SizeF` (حجمان على الهاتف، أربعة على الطيّات)، وبحد أقصى 16.
8. نحترم `maxResizeWidth/Height` و`resizeMode`، وخيار الـ category (Home screen).
9. `deleteAppWidgetId()` عند الإزالة.
10. Android 12+: `AppWidgetHostView.setColorResources()` لتمرير ألوان ثيمنا للودجت التي تدعمها.

**قواعد دورة الحياة والأداء**
- `startListening()` في `onStart` و`stopListening()` في `onStop` ⚠️ (التوثيق الرسمي لا يغطيها؛ تؤكَّد من مرجع `AppWidgetHost`).
- نُنشئ `AppWidgetHostView` فقط للصفحة الحالية والمجاورة، ونحتفظ بها في `WidgetViewCache` مفتاحها `appWidgetId` (لا نعيد إنشاءها عند إعادة التركيب).
- إخفاء الودجت عن الشجرة (detach) حين تخرج من الشاشة بدل إبقائها نشطة.
- تقليم الزوايا: نفرض Clip بزاوية موحّدة (على غرار `RoundedCornerEnforcement` في Launcher3) لأن بعض الودجت تتجاهل `system_app_widget_background_radius`.
- `InteractionHandler` مخصص لتشغيل `PendingIntent` مع `ActivityOptions` (أنيميشن فتح التطبيق من موضع الودجت).
- حالات الخطأ: المزوّد محذوف/معطل → عرض بديل "الودجت غير متاحة" + زر إزالة/إعادة تثبيت.
- `onProvidersChanged()` و`onProviderChanged()` → تحديث ذكي لا يعيد بناء الصفحة كلها.

**دمج Compose:**
```kotlin
@Composable
fun WidgetView(item: WidgetItem, mgr: WidgetHostManager, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx -> mgr.obtainView(ctx, item) },          // من الكاش
        update = { v -> mgr.dispatchSize(item, v) },              // options + SizeF
        onRelease = { v -> mgr.detach(v) },                       // detach وليس destroy
    )
}
```

**هيكل المدير:**
```kotlin
class WidgetHostManager @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val awm = AppWidgetManager.getInstance(ctx)
    private val host = object : AppWidgetHost(ctx, HOST_ID) {
        override fun onCreateView(c: Context, id: Int, info: AppWidgetProviderInfo?) =
            OrbitWidgetHostView(c).also { it.setAppWidget(id, info) }
        override fun onProvidersChanged() { /* refresh picker + placed widgets */ }
    }
    fun onStart() = host.startListening()
    fun onStop()  = runCatching { host.stopListening() }
    fun allocateId(): Int = host.allocateAppWidgetId()
    fun tryBind(id: Int, provider: ComponentName, user: UserHandle, opts: Bundle) =
        awm.bindAppWidgetIdIfAllowed(id, user, provider, opts)
    fun release(id: Int) = host.deleteAppWidgetId(id)
}
```

#### ب) Widget Picker احترافي
- قائمة مجمّعة حسب التطبيق (`getInstalledProvidersForProfile`)، بحث، معاينة حية.
- المعاينة: `previewLayout` (Android 12+) أو `generatedPreviews` (Android 15+) أو صورة `previewImage`.
- عرض الحجم بالخلايا، والوصف (`description`)، وفئة الودجت.
- السحب من الـ Picker إلى الشبكة مباشرة، مع إظهار خلايا الإفلات الصالحة.

#### ج) التخطيط وتغيير الحجم
- Handles بالتقاط على الخلايا (snap) ضمن حدود `minResize*`/`maxResize*`.
- **Widget Stacks** (مرحلة 3): `VerticalPager` يحمل عدة ودجت ولا يُرفق إلا المرئي منها.
- إعادة الإعداد (Reconfigure) للودجت التي تدعمها (Android 12+).

#### د) النسخ الاحتياطي والاستعادة
- معرّفات الودجت **لا تنتقل** بين الأجهزة → نخزن `widgetProvider` ونعلّم العنصر "بانتظار الاستعادة"، ثم نعيد bind (+ configure عند الحاجة) عند أول فتح، ونستمع لـ `ACTION_APPWIDGET_HOST_RESTORED`.
- Auto Backup يشمل Room وProto لكن بدون `appWidgetId`.

#### هـ) ودجتنا (Glance 1.2)
| الودجت | ملاحظات الأداء |
|--------|----------------|
| **ساعة + تاريخ ميلادي/هجري** | نغلّف `TextClock` عبر `AndroidRemoteViews` لتحديث النظام لها **بلا wakeups منا**؛ تقويم Umm al-Qura عبر `HijrahChronology` |
| **مقترحات تطبيقات** | تحديث عند تغيّر السياق أو عند فتح الشاشة (لا polling) |
| **شريط البحث** | `actionStartActivity` يفتح بحث Orbit مباشرة |
| **حالة الجهاز** (بطارية/تخزين) | تحديث عبر broadcast النظام فقط |
- `GlanceTheme` + `ColorProviders` للألوان الديناميكية، `SizeMode.Responsive`.
- ⚠️ ودجت Glance تُرسم عبر RemoteViews وليست قابلة للتبديل مع عناصر Compose العادية.
- `updateAll()` عند حدث حقيقي فقط.

### 7.5 البحث الموحّد
**المصادر:** تطبيقات، اختصارات (`LauncherApps.ShortcutQuery`)، إعدادات النظام (قائمة Intents ثابتة)، جهات اتصال (اختياري `READ_CONTACTS`)، حاسبة/وحدات/عملات ثابتة، سجل البحث، وبحث ويب عبر Intent للمتصفح (لا شبكة منا).

**الفهرسة:** فهرس في الذاكرة (مئات العناصر لا تحتاج FTS)، يُبنى على thread واحد (`limitedParallelism(1)`) ويُحدَّث تدريجيًا.

**التطبيع للعربية (Normalization):**
- إزالة التشكيل `U+064B–U+065F` و`U+0670` والتطويل `U+0640`.
- توحيد: أ إ آ ٱ → ا · ى → ي · ة → ه · الأرقام الهندية → غربية.
- Latin: lowercase + إزالة العلامات.
- **مفاتيح لكل تطبيق:** الاسم المطبَّع، النسخة المنقحرة (Transliteration عربي↔لاتيني)، الأحرف الأولى (Initials)، آخر مقطع من اسم الحزمة، وأسماء مستعارة يضيفها المستخدم (مثل "واتساب" ↔ "whatsapp").

**الترتيب:** تطابق تام > بداية الاسم > بداية كلمة > Initials > احتواء > Fuzzy (Damerau-Levenshtein ≤ 1–2 للكلمات ≥ 4 أحرف) ثم **ضرب النتيجة بـ frecency** (تكرار + حداثة).
🎯 الهدف: ≤ 16ms لكل ضغطة مفتاح على 500+ عنصر.

### 7.6 المحرك الذكي (On-device Intelligence)

**الفلسفة:** البسيط أولًا، يقاس عائده، ثم يُعقَّد. **لا شيء يغادر الجهاز.**

**1) اقتراحات التطبيقات (Predicted Apps)**
- **أحداث الإطلاق:** جدول `launch_event(pkg, ts, hourBucket, dow, charging, headset, prevApp)` محدود بـ ~5,000 صف مع ضغط.
- **التسجيل:** من داخل Orbit مباشرة (بدون صلاحيات). `UsageStatsManager` **اختياري** لاحقًا لاستيراد التاريخ.
- **النموذج v1:**
  `score(app|ctx) = w1·decayedFreq(app) + w2·P(app|hour,dow) + w3·P(app|prevApp) + w4·P(app|charging/headset)`
  مع Laplace smoothing وتناقص أسي (نصف عمر ≈ 14 يومًا)، والأوزان من انحدار لوجستي صغير يُحدَّث محليًا.
- **المخرج:** أعلى 4–8 تطبيقات في صف Dock الذكي والودجت.
- **تقييم:** Hit-rate@k محلي (هل النقرة التالية ضمن المقترحات؟) ويظهر للمطور في وضع debug فقط.

**2) التصنيف التلقائي:** قواعد (`ApplicationInfo.category` + أنماط الحزم) ← نموذج LiteRT صغير جدًا (< 1MB) كمرحلة اختيارية.

**3) الذكاء التوليدي (اختياري ومعلّق على قدرة الجهاز):**
- ML Kit GenAI/Gemini Nano يُستخدم **فقط** إذا أرجع `checkStatus()` أنه متاح؛ **وإلا تُخفى الميزة بصمت**.
- ⚠️ **افتراض:** S22 Ultra لا يدعمه (لم يُتحقق منه) → لا ميزة أساسية تعتمد عليه.
- Hybrid/cloud (Firebase AI Logic) **خارج 1.0** لأنه يكسر مبدأ "بدون إنترنت".
- أوامر طبيعية مثل "افتح كاميرا ثم أرسلها لـ…" = مرحلة R&D بعد 1.0.

**4) وضع التركيز (Focus):** مجموعات تطبيقات تُخفى/تُرمّد حسب جدول أو يدويًا، بدون خدمات خلفية (يُقيَّم عند فتح الشاشة).

### 7.7 الإيماءات (Gestures)
| الإيماءة | الافتراضي | ملاحظة |
|----------|-----------|--------|
| سحب لأعلى على الشاشة | فتح الدرج | `AnchoredDraggable` |
| سحب لأسفل | البحث | بحث أسفل الشاشة أقرب للإبهام على 6.8" |
| ضغط مطوّل على فراغ | وضع التحرير | |
| نقر مزدوج | قفل الشاشة (اختياري) | **تحذير سياسات:** يتطلب AccessibilityService أو DeviceAdmin؛ نجعله **اختياريًا ومعطلًا افتراضيًا** مع إفصاح واضح وتبرير في Play Console |
| Pinch | وضع نظرة عامة للصفحات | |
- فتح لوحة الإشعارات بالسحب لأسفل ⚠️ غير متاح عبر API عام مستقر؛ البدائل: (أ) ترك إيماءة النظام من حافة الشاشة العلوية، (ب) خدمة Accessibility اختيارية. القرار في Spike 6.
- المعالجة بـ `awaitEachGesture` وVelocityTracker؛ **لا كتابة State لكل حركة** (نستخدم `Animatable`/`graphicsLayer`).
- **حواف S22 Ultra:** نترك هامش ≥ 16–24dp ونستخدم `WindowInsets.systemGestures` حتى لا تتعارض سحبة الصفحات مع إيماءة الرجوع.

### 7.8 المجلدات والسحب والإفلات
- المجلد = Morph من شبكة أيقونات إلى Dialog (Expressive).
- السحب الداخلي بـ `detectDragGesturesAfterLongPress`؛ الإفلات بين تطبيقات النظام بـ `dragAndDropSource/Target` (للـ shortcuts القادمة من خارج التطبيق).
- بدائل وصولية للسحب: إجراءات مخصصة (Custom Actions) "انقل إلى صفحة…".
- **S Pen:** Hover يعرض معاينة المجلد/اسم التطبيق (`ACTION_HOVER_ENTER`، `TOOL_TYPE_STYLUS`) — ميزة صغيرة تميّزنا على Samsung.

### 7.9 نقاط الإشعارات (Dots) — اختياري
- `NotificationListenerService` (صلاحية خاصة يمنحها المستخدم) تُعلِم الأيقونات بوجود إشعارات.
- ⚠️ **التطبيقات المثبتة من خارج المتاجر (sideload/F-Droid)** قد تحتاج "السماح بالإعدادات المقيدة" يدويًا (Restricted settings منذ Android 13)؛ نشرحها في الـ Onboarding.
- لا نخزن نص الإشعارات إطلاقًا؛ نحتفظ بالعدّاد/المعرّف فقط.

### 7.10 الخلفية والثيمات
- `WallpaperManager` للألوان (يغذي Dynamic Color من النظام أصلًا)، و`setWallpaperOffsets` للـ parallax عند التنقل بين الصفحات.
- Blur: `Modifier.blur`/`RenderEffect` للعناصر، و`Window.setBackgroundBlurRadius` (Android 12+) للدرج؛ نفحص `isCrossWindowBlurEnabled` ونرجع إلى Scrim عند عدم التوفر أو وضع توفير الطاقة.
- طلب معدل تحديث أقل للمحتوى الثابت (`View.setRequestedFrameRate` Android 15+) ⚠️ تأكد من مكافئه في Compose 1.12.

### 7.11 الإعدادات والنسخ الاحتياطي
- Proto DataStore (قابل للتطور schema) لكل الإعدادات، وواجهة M3E بتجميع منطقي: المظهر · الشبكة · الإيماءات · الذكاء · الخصوصية · النسخ الاحتياطي.
- ملف تصدير `.orbit` = JSON مضغوط (layout + settings + aliases) بإصدار schema ومهاجرات (migrations).

### 7.12 الإعداد الأول (Onboarding) وتعيين Home
1. شاشة ترحيب مختصرة (3 خطوات كحد أقصى).
2. `RoleManager.createRequestRoleIntent(ROLE_HOME)` ← تعيين الـ Launcher الافتراضي.
3. استيراد اختياري، اختيار الشبكة والأيقونات والثيم.
4. الصلاحيات الاختيارية **لاحقًا وفي سياقها** (إشعارات، جهات اتصال، Usage) مع شرح الفائدة.

---

## 8. الأداء والتحسين (القلب الاستراتيجي للمشروع)

### 8.1 ميزانيات الأداء 🎯 (تُقاس على S22 Ultra + جهاز ضعيف مرجعي)
| المقياس | الهدف | الأداة |
|---------|-------|--------|
| Cold start → أول إطار | ≤ 600ms (P50) / ≤ 900ms (P95) | Macrobenchmark `StartupTimingMetric` |
| العودة للـ Home (process حي) | ≤ 120ms (P95) | Macrobenchmark + `reportFullyDrawn` |
| الإطارات | P90 ≤ 8.3ms · P99 ≤ 16.6ms | `FrameTimingMetric` + JankStats |
| ذاكرة خاملة (PSS) | ≤ ~110MB بلا احتساب الودجت | `MemoryUsageMetric` / `dumpsys meminfo` |
| زمن بحث لكل ضغطة | ≤ 16ms (P95) | `TraceSectionMetric` |
| الحفظ على الـ Main | ≤ 5ms | Perfetto |
| بطارية في الخلفية | **0 wakeups** منا (شاشة مطفأة) | Perfetto + `dumpsys batterystats` |
| حجم AAB / مثبّت | ≤ 8MB / ≤ 20MB | CI size budget |
| Crash-free / ANR | ≥ 99.7% / أقل بكثير من حدود vitals | Play Console |

### 8.2 تقنيات البناء والتشغيل
- **Baseline Profiles + Startup Profiles + Dex Layout Optimization** (R8) + `ProfileInstaller`. نولّدها في CI لكل إصدار.
- **R8 full mode** + shrink resources + `android:extractNativeLibs="false"` + لا مكتبات native.
- Kotlin K2 + **Strong Skipping Mode**؛ نماذج UI بـ `@Immutable`/`@Stable` و`kotlinx.collections.immutable`.
- `derivedStateOf`، قراءات متأخرة (lambda modifiers)، `key` + `contentType` في القوائم.
- لا `Gson`/reflection، لا صور PNG كبيرة (Vector/WebP).
- `androidx.tracing` Trace sections لكل مسار حرج (Bind، Reconcile، Search، Icon load).
- **ADPF** (`PerformanceHintManager` API 31+): جلسة Hint أثناء الإيماءات الثقيلة (فتح الدرج) — اختيارية وتُقاس فائدتها.
- **Lite Mode تلقائي:** `ActivityManager.isLowRamDevice`، memoryClass، `Build.VERSION` → إيقاف Blur والحركة الثقيلة وتقليل الكاش.

### 8.3 الذاكرة (وصلة مع Android 17)
- نظام حدود الذاكرة الجديد يقتل التسريبات الشديدة؛ **LeakCanary في debug** + اختبار تسريب لكل Activity/Composable رئيسي.
- `onTrimMemory`: تقليص LRU الأيقونات وWidgetViewCache.
- نراقب `ApplicationExitInfo` (API 30+) لاكتشاف أي قتل بسبب الذاكرة ونسجّله محليًا (Debug Export).
- أمر الاختبار: `adb shell am memory-limiter status` (للأجهزة التي تفرض الحدود) ✅.

### 8.4 الطاقة
- لا Foreground services، لا polling، لا broadcast receivers مسجّلة بشكل دائم (نسجّل فقط حين Visible).
- ودجتنا تعتمد على تحديثات النظام (TextClock…) بدل المؤقتات.
- نختبر Doze/App Standby وسلوك "Battery optimization" وSamsung "Sleeping apps".

### 8.5 قياس مستمر
- مجموعة Macrobenchmark: startup (COLD/WARM/HOT)، تمرير الدرج، تمرير الصفحات، السحب والإفلات، فتح Picker الودجت، كتابة بحث.
- تشغل على جهاز فعلي (S22 Ultra + جهاز متوسط + جهاز ضعيف) قبل كل إصدار، و**تفشل الـ CI** عند تراجع > 10% عن baseline.

---

## 9. التوافق: S22 Ultra وكل الأجهزة

### 9.1 استراتيجية الإصدارات
| الإصدار | سلوك |
|---------|------|
| Android 8–10 (API 26–29) | وظائف أساسية، Fallback للألوان، بلا Themed icons/Blur |
| Android 11–12 (30–32) | Dynamic Color، Blur، حجوم الودجت بالخلايا (31+) |
| Android 13–14 (33–34) | Themed icons، لغة لكل تطبيق، Predictive back |
| Android 15–16 (35–36) | Private Space، Archived apps، Generated widget previews، Edge-to-edge إلزامي |
| Android 17 (37) | اختبار حدود الذاكرة والـ adaptive apps |
- `compileSdk 37`، `targetSdk 36` عند الإطلاق (متطلب Play + نفس نظام S22 Ultra)، ثم **targetSdk 37** بعد اجتياز اختبارات الطيّات/التابلت.
- نتوقع طلب API 37 في دورة Play القادمة (≈ أغسطس 2027) ⚠️ تأكد من الإعلان الرسمي.

### 9.2 الأحجام والأشكال
- `WindowSizeClass` + `material3-adaptive`: هاتف / طيّ (Z Fold) / تابلت.
- Android 16 يتجاهل قيود الاتجاه وعدم قابلية تغيير الحجم على الشاشات الكبيرة (sw ≥ 600dp)، وAndroid 17 يزيل خيار الاستثناء لمن يستهدف 37 ✅ → **التخطيط يجب أن يكون متكيفًا حقًا**.
- أحجام شبكة مختلفة تلقائيًا بحسب مساحة الشاشة، وإعادة تدفق (reflow) عند الطي/الفتح دون فقدان ترتيب.
- نعلن `configChanges` في الـ Manifest ونتعامل مع تغيّر التكوين (دوران/طيّ/ثيم/خط) داخل Compose، حتى لا تُعاد إنشاء الـ Activity (إعادة الإنشاء مكلفة جدًا لـ launcher).

### 9.3 مصفوفة توافق الشركات (OEM)
| OEM / الواجهة | مخاطر معروفة | تخفيف |
|----------------|--------------|-------|
| **Samsung One UI** | Sleeping/Deep sleeping apps، Edge panel، حواف منحنية، Secure Folder، Good Lock/Home Up | لا اعتماد على عمل خلفي، هوامش حواف، دعم `userSerial`، اختبار مع Good Lock |
| **Xiaomi HyperOS/MIUI** | قتل خلفية عنيف، إذن اختصارات الشاشة الرئيسية، Autostart | Onboarding ذكي يكتشف الشركة ويرشد للإعدادات (مرجع: dontkillmyapp.com) |
| **Oppo/OnePlus/Realme (ColorOS/OxygenOS)** | تحسين بطارية مشدد | مثل أعلاه |
| **Huawei (HMS بلا GMS)** | غياب Google Play Services | **لا اعتماد على GMS إطلاقًا** (ML Kit GenAI اختياري فقط) |
| **Pixel / AOSP** | المرجع | إجراء اختبارات الإيماءات والـ Role |
| **Android Go / RAM ≤ 3GB** | ذاكرة | Lite Mode، تقليل الشبكة والكاش |

### 9.4 مختبر الأجهزة
- أجهزة فعلية: S22 Ultra (أساسي)، جهاز Samsung أحدث (One UI 8.5)، Pixel، Xiaomi، جهاز ضعيف، طيّ/تابلت.
- مزارع أجهزة: Samsung Remote Test Lab / Firebase Test Lab (للتغطية فقط؛ الأداء يُقاس على أجهزة فعلية).

---

## 10. الخصوصية والأمان وسياسات Google Play

### 10.1 الصلاحيات
| الصلاحية | الحالة | السبب |
|----------|--------|-------|
| `BIND_APPWIDGET` | مطلوبة | استضافة الودجت (تُمنح عبر حوار النظام) |
| `INTERNET` | ❌ غير موجودة في 1.0 | خصوصية |
| `QUERY_ALL_PACKAGES` | ❌ **لا نطلبها** | سياسة Play تقيّدها بحالات استخدام ضيقة؛ `LauncherApps` يكفينا لسرد التطبيقات القابلة للتشغيل ✅ |
| `READ_CONTACTS` | اختيارية | بحث جهات الاتصال |
| `PACKAGE_USAGE_STATS` | اختيارية | استيراد تاريخ الاستخدام لتحسين الاقتراحات |
| Notification Listener | اختيارية | نقاط الإشعارات |
| Accessibility | اختيارية جدًا ومعطلة | قفل الشاشة بالنقر المزدوج فقط |
| `USE_BIOMETRIC` | اختيارية | قفل التطبيقات المخفية |

### 10.2 الأمان
- بيانات محلية فقط؛ تخزين الـ Layout والإعدادات داخل مساحة التطبيق، والتشفير للمقفل منها عبر Keystore (ضمن حدود Android 17 لعدد المفاتيح ✅ — نستخدم مفتاحًا واحدًا).
- `exported=false` لكل المكونات عدا HomeActivity وما يلزم للـ Pin shortcuts/الـ Widget configure.
- Intents صريحة، وفحص مصدر `PinItemRequest`.
- تحديث الاعتماديات آليًا (Renovate/Dependabot) + `dependency-guard`.

### 10.3 Play Console
- **Data Safety:** "لا بيانات تُجمع / لا بيانات تُشارك" (يجب أن يطابق الواقع).
- إقرار الصلاحيات الحساسة المستخدمة فقط (Accessibility إن وُجد).
- سياسة خصوصية مختصرة، وعدم وجود سلوك مضلل (لا إعلانات خفية، لا استبدال لإعدادات المستخدم).
- Target API ≥ 36 ✅، Billing 8+ إن وُجد Premium ✅، 16KB page size لأي native.

### 10.4 التوزيع
- **Play Store** (الأساسي) + اختياري **GitHub/F-Droid** (يتطلب بناءً قابلًا لإعادة الإنتاج، وبلا اعتماد على Play Services).
- قرار الترخيص (مفتوح المصدر أم لا) في القسم 16.

---

## 11. الوصولية والتعريب

- **RTL أولًا:** `supportsRtl`، `LayoutDirection`، أيقونات تتعكس عند الحاجة، اتجاه الـ pager والـ scroller.
- لغات: العربية والإنجليزية في 1.0، `localeConfig` للغة لكل تطبيق (Android 13+).
- أرقام: خيار أرقام عربية-هندية/غربية في الساعة والتاريخ، وتقويم هجري Umm al-Qura.
- **TalkBack:** `semantics` لكل عنصر (اسم التطبيق، الحالة: مجلد/ودجت)، Custom Actions بدل السحب.
- أحجام خط حتى 200% دون قص، تباين ≥ WCAG AA، Reduce motion (يقرأ `ANIMATOR_DURATION_SCALE`)، اهتزاز لمسي (Haptics) مدروس.

---

## 12. الجودة والاختبار

| الطبقة | الأدوات | يغطي |
|--------|---------|------|
| Unit | JUnit + MockK + Turbine + Kotest assertions | محرك البحث، التطبيع العربي، Grid occupancy، نموذج الاقتراح، المهاجرات |
| Compose UI | Compose test (واجهات v2 هي الافتراضية منذ Compose 1.11 ✅ ← تعتمد `StandardTestDispatcher`) | الدرج، المجلد، وضع التحرير |
| Screenshot | Roborazzi أو Compose Preview Screenshot Testing | RTL/LTR، Light/Dark، خط 200%، أحجام شاشات |
| Instrumented | AndroidX Test | Role، Widget bind/configure، Pin shortcuts |
| Performance | Macrobenchmark + JankStats + Perfetto | القسم 8 |
| Memory | LeakCanary (debug) + اختبارات Heap | التسريب |
| Robustness | StrictMode + Monkey + اختبارات تحمل | تثبيت/حذف مئات التطبيقات، تدوير، طيّ |
| Migration | Room auto-migration tests | أي تغيير schema |

**اختبارات الودجت (مصفوفة):** ودجت Samsung (طقس/تقويم/ساعة)، Google (بحث/طقس)، ودجت قوائم (Collection/ListView)، ودجت تحتاج Configure، ودجت قابلة لإعادة الإعداد، ودجت تتجاهل الزوايا، ودجت ثقيلة؛ وحالات: حذف المزوّد، تحديث المزوّد، الاستعادة على جهاز جديد، إلغاء Configure.

**Definition of Done لأي ميزة:** كود + اختبارات + لقطات RTL/Dark + نتائج Macrobenchmark لا تتراجع + لا تحذيرات Lint/Detekt جديدة + وصولية مفحوصة.

---

## 13. CI/CD والإصدار

- **CI (GitHub Actions أو مماثل):** Spotless/ktlint + Detekt + Lint → Unit → Screenshot → Build AAB → فحص الحجم → (ليلًا) Macrobenchmark على أجهزة فعلية → رفع mapping.txt.
- **التفرع:** trunk-based + feature flags (محلية، ثابتة التركيب).
- **الإصدار:** Internal → Closed (≈ 50–100 مختبر على أجهزة متنوعة) → Open → Production بنشر تدريجي (1% → 5% → 20% → 100%) مع مراقبة Android vitals.
- **الإصدارات:** SemVer + `versionCode` من CI.
- **الاستجابة للحوادث:** Kill-switch عبر نشر مستعجل؛ لا خدمات بعيدة.
- **Crash reporting بدون SDK:** `ApplicationExitInfo` + سجل محلي يُصدَّر برغبة المستخدم + تقارير Play vitals.

---

## 14. خارطة الطريق التفصيلية

### المرحلة 0 — Spikes وإزالة المخاطر (أسبوعان)
- [ ] Spike 1: Home Activity + `ROLE_HOME` + الإقلاع على S22 Ultra وقياس الـ baseline.
- [ ] Spike 2: `AppWidgetHost` داخل Compose (`AndroidView`) مع 5 ودجت حقيقية على S22 Ultra (Samsung + Google) وقياس الذاكرة/jank.
- [ ] Spike 3: خط أنابيب الأيقونات (رندرة + LRU + WebP) على 300 تطبيق وقياس الذاكرة.
- [ ] Spike 4: سلوك الإيماءات/Recents مع Samsung (3 أزرار مقابل إيماءات) والتأكد من حدود 3.2.
- [ ] Spike 5: Private Space / Work profile / Secure Folder (الصلاحيات والـ APIs).
- [ ] Spike 6: قفل الشاشة/فتح لوحة الإشعارات بدائل (Accessibility مقابل لا شيء).
- [ ] Spike 7: Material3 Expressive: أي واجهات مستقرة فعلًا في الإصدار الحالي، وتحديد الإصدار النهائي.
- [ ] Spike 8: توافق `GenerativeModel.checkStatus()` على S22 Ultra (نعرف نهائيًا إن كان Nano متاحًا).
- **مخرجات:** تقرير مخاطر + قرارات مثبتة + هيكل المشروع و CI جاهز.
- **معيار الخروج:** ودجت تعمل، عودة للـ Home ≤ 150ms، لا مفاجآت حاجبة.

### المرحلة 1 — MVP (6 أسابيع)
- [ ] هيكل الوحدات، Hilt، convention plugins، version catalog، Baseline Profile أولي.
- [ ] Workspace + Dock + الصفحات + حفظ/استعادة + Edge-to-edge + Predictive Back.
- [ ] الدرج (أبجدي + scroller) + خط أنابيب الأيقونات + Themed icons.
- [ ] السحب والإفلات والمجلدات الأساسية.
- [ ] بحث تطبيقات مع تطبيع عربي.
- [ ] الثيم M3 Expressive + Dynamic Color + Dark.
- [ ] Onboarding + تعيين Home + الإعدادات الأساسية.
- **معيار الخروج:** يعمل كـ daily driver على S22 Ultra لمدة أسبوع بلا مشاكل حرجة، وتحقيق ميزانيات الإقلاع والإطارات.

### المرحلة 2 — الودجت الكاملة (5 أسابيع)
- [ ] Host Manager كاملة (bind/configure/reconfigure/delete/restore).
- [ ] Widget Picker + معاينات + السحب المباشر + تغيير الحجم بالخلايا.
- [ ] الألوان الديناميكية للودجت + تقليم الزوايا + InteractionHandler.
- [ ] ودجتنا بـ Glance: ساعة/هجري، بحث، مقترحات (ثابتة مؤقتًا)، حالة الجهاز.
- [ ] حالات الخطأ والاستعادة، وWidget Stacks.
- **معيار الخروج:** اجتياز مصفوفة اختبار الودجت (القسم 12) ولا تراجع في الأداء.

### المرحلة 3 — الذكاء والتخصيص (6 أسابيع)
- [ ] تسجيل الأحداث + نموذج الاقتراح v1 + صف المقترحات + قياس Hit-rate.
- [ ] بحث موحّد كامل (اختصارات، إعدادات، جهات، حاسبة) + fuzzy + مفاتيح الأسماء المستعارة.
- [ ] تصنيف تلقائي للدرج، وضع التركيز.
- [ ] Icon packs + أشكال `MaterialShapes` + إيماءات قابلة للضبط.
- [ ] Work profile / Secure Folder / Private Space + إخفاء/قفل التطبيقات.
- [ ] نقاط الإشعارات (اختياري) + النسخ الاحتياطي/الاستيراد.
- **معيار الخروج:** Hit-rate@4 ≥ 60% 🎯 على بيانات المختبرين، وزمن البحث ≤ 16ms.

### المرحلة 4 — الصقل والتوافق (4 أسابيع)
- [ ] طيّات/تابلت (adaptive)، Lite Mode، تحسينات Samsung (S Pen hover، حواف).
- [ ] وصولية كاملة (TalkBack، خط 200%)، RTL شامل، لغات إضافية.
- [ ] OEM matrix: Xiaomi/Oppo/Huawei، إرشادات البطارية في Onboarding.
- [ ] تحسين الأداء النهائي (Profiles، ADPF) وتقليل الحجم.
- **معيار الخروج:** كل بوابات القسم 8 خضراء على الأجهزة الثلاثة.

### المرحلة 5 — Beta والإطلاق (3 أسابيع)
- [ ] Closed testing + إصلاحات + ملف Play (لقطات، وصف، Data Safety، الخصوصية).
- [ ] نشر تدريجي ومراقبة vitals.

### ما بعد 1.0 (Backlog مرتب)
1. أوامر طبيعية/تلخيص بالـ LLM محلي حيث متاح (R&D).
2. مزامنة تخطيط عبر ملف/سحابة اختيارية مشفرة.
3. صفحة "اليوم" (Today) بودجت وبطاقات بدل Feed.
4. ودجت إضافية (مواقيت الصلاة بحساب محلي، ملاحظات سريعة).
5. وضع الشاشة الخارجية/DeX أساسي.
6. نموذج Premium (Icon packs متقدمة، Stacks، إيماءات إضافية) عبر Billing 8+.

---

## 15. سجل المخاطر

| الخطر | الاحتمال | الأثر | التخفيف |
|-------|----------|-------|---------|
| تعقيد DnD والودجت في Compose | عالٍ | عالٍ | Spike مبكر + عدم الاعتماد على LazyLayout للـ Workspace + حفظ آمن |
| حدود الذاكرة الجديدة (Android 17) تقتل الـ launcher | متوسط | عالٍ | ميزانيات ذاكرة + LRU + LeakCanary + مراقبة `ApplicationExitInfo` |
| تغيّر واجهات Material3 Expressive قبل الاستقرار | متوسط | متوسط | عزل عبر `core:designsystem`، تثبيت الإصدار بعد Spike 7 |
| سلوك Samsung (Sleeping apps/الإيماءات/Recents) | متوسط | عالٍ | اختبار مبكر + Onboarding إرشادي |
| سياسات Play (Accessibility/QUERY_ALL_PACKAGES/Data safety) | متوسط | عالٍ | أقل صلاحيات + ميزات اختيارية + مراجعة سياسات قبل كل إصدار |
| ودجت ثالثة سيئة السلوك (ثقيلة/تتعطل) | عالٍ | متوسط | عزل الأخطاء، عرض بديل، عدم إنشاء Views خارج الشاشة |
| تعارض متطلب Compose 1.12 (AGP 9.1.1 + compileSdk 37) مع أدوات الفريق | منخفض | متوسط | ترقية الأدوات من اليوم الأول |
| غياب Gemini Nano على أجهزة كثيرة | عالٍ | منخفض | الذكاء الأساسي لا يعتمد عليه |
| إجهاد المطورين من تعدد OEM | متوسط | متوسط | مصفوفة اختبار آلية وأجهزة فعلية |

---

## 16. قرارات مفتوحة (تحتاج قرارك)

1. **الترخيص:** مغلق المصدر (Play فقط)، أم مفتوح (GPL/Apache) ويوزَّع أيضًا على F-Droid؟ (يؤثر على إمكانية استعارة كود من مشاريع GPL: **لا يجوز لمشروع مغلق**).
2. **الاسم والـ package:** `Orbit` اسم مؤقت؛ `com.example.orbit` للتجربة.
3. **نموذج العمل:** مجاني بالكامل أم Freemium (Premium)؟
4. **حجم الفريق/الميزانية:** التقديرات تفترض مطورَين؛ هل هذا مناسب؟
5. **اللغات عند الإطلاق:** العربية + الإنجليزية فقط؟
6. **الاعتماد على Spike 8:** هل نجعل Gemini Nano ميزة "اختيارية بقدرة الجهاز" أم نتركها بعد 1.0؟

---

## 17. الملاحق

### أ) Version Catalog (مقترح — تحقق من آخر إصدار عند البدء)
```toml
[versions]
agp = "9.1.1"            # الحد الأدنى المطلوب لـ Compose 1.12 ✅
kotlin = "2.3.+"         # ⚠️ ثبّت آخر مستقر
composeBom = "2026.08.00" # أو أحدث ✅
material3 = "1.5.0-beta01" # ⚠️ أو أحدث/المستقر
glance = "1.2.0"         # ✅
compileSdk = "37"
targetSdk = "36"
minSdk = "26"

[libraries]
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-material3 = { group = "androidx.compose.material3", name = "material3", version.ref = "material3" }
androidx-glance-appwidget = { group = "androidx.glance", name = "glance-appwidget", version.ref = "glance" }
androidx-glance-material3 = { group = "androidx.glance", name = "glance-material3", version.ref = "glance" }
# + room, datastore-proto, hilt, coroutines, serialization, window, material3-adaptive,
#   profileinstaller, benchmark-macro, tracing, leakcanary (debug)
```

### ب) Manifest (الهيكل الأساسي)
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
          xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.BIND_APPWIDGET"
        tools:ignore="ProtectedPermissions" />
    <!-- لا INTERNET، لا QUERY_ALL_PACKAGES -->

    <application
        android:name=".OrbitApp"
        android:supportsRtl="true"
        android:enableOnBackInvokedCallback="true"
        android:extractNativeLibs="false"
        android:localeConfig="@xml/locales_config"
        android:theme="@style/Theme.Orbit.Splash">

        <activity
            android:name=".HomeActivity"
            android:exported="true"
            android:launchMode="singleTask"
            android:stateNotNeeded="true"
            android:resumeWhilePausing="true"
            android:taskAffinity=""
            android:windowSoftInputMode="adjustPan"
            android:configChanges="orientation|screenSize|smallestScreenSize|screenLayout|uiMode|density|fontScale|layoutDirection|locale">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.HOME" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </activity>

        <!-- Pin shortcuts/widgets: LauncherApps.PinItemRequest -->
        <!-- Notification listener و Accessibility: خدمات اختيارية، معطلة افتراضيًا -->
    </application>
</manifest>
```

### ج) تشغيل تطبيق مع أنيميشن
```kotlin
fun launchApp(la: LauncherApps, comp: ComponentName, user: UserHandle, bounds: Rect, view: View) {
    val opts = ActivityOptions.makeClipRevealAnimation(view, 0, 0, view.width, view.height).toBundle()
    la.startMainActivity(comp, user, bounds, opts)
}
```

### د) قائمة فحص قبل كل إصدار
- [ ] جميع بوابات القسم 8 خضراء (S22 Ultra + جهاز ضعيف).
- [ ] مصفوفة الودجت اجتازت.
- [ ] RTL/Dark/خط 200% مفحوصة.
- [ ] Manifest بلا صلاحيات زائدة؛ Data Safety يطابق الواقع.
- [ ] Baseline Profile مُحدّث ومرفق.
- [ ] mapping.txt مرفوع؛ الحجم ضمن الميزانية.
- [ ] اختبار الترقية من الإصدار السابق (Room/Proto migration).
- [ ] نشر تدريجي مع خطة تراجع.

### هـ) حزمة الاقتراحات — الأولويات إن ضاق الوقت
1. (يجب) Home + Drawer + Icons + Widgets Host + بحث تطبيقات.
2. (مهم) اقتراحات التطبيقات، Themed icons، النسخ الاحتياطي.
3. (يمكن تأجيله) Stacks، Focus، Icon packs المتقدمة، نقاط الإشعارات.

### و) المصادر (تم الاطلاع عليها في نوفمبر 2026)
- Android 17 release notes: https://developer.android.com/about/versions/17/release-notes
- Android 17 behavior changes (all apps) — حدود الذاكرة: https://developer.android.com/about/versions/17/behavior-changes-all
- Google Play target API requirements: https://support.google.com/googleplay/android-developer/answer/11926878
- سياسة QUERY_ALL_PACKAGES: https://support.google.com/googleplay/android-developer/answer/10158779
- Jetpack Compose August '26 release (Compose 1.12): https://android-developers.googleblog.com/2026/08/jetpack-compose-august-2026-release.html
- Compose Material 3 releases: https://developer.android.com/jetpack/androidx/releases/compose-material3
- Jetpack Glance releases: https://developer.android.com/jetpack/androidx/releases/glance
- Building an AppWidgetHost: https://developer.android.com/develop/ui/views/appwidgets/host
- Gemini Nano / AICore: https://developer.android.com/ai/aicore
- AI on Android at I/O 2026: https://android-developers.googleblog.com/2026/05/android-ai-intelligence-system.html
- Galaxy S22 One UI 8 (آخر تحديث رئيسي): https://www.sammobile.com/news/samsung-galaxy-s22-plus-ultra-one-ui-8-update/
- Kvaesitso (مرجع Launcher مفتوح بـ Compose): https://f-droid.org/packages/de.mm20.launcher2.release

</div>
