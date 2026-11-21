package ir.aminim.smartplant

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ir.aminim.smartplant.knowledge.PlantInfo
import ir.aminim.smartplant.ui.screens.*
import ir.aminim.smartplant.ui.theme.SmartPlantTheme
import ir.aminim.smartplant.ui.theme.ThemeState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()
    private var incoming by mutableStateOf<Incoming?>(null)

    /** عکس اشتراک‌گذاری‌شده از برنامه‌های دیگر یا باز شدن از اعلان */
    data class Incoming(val image: Uri? = null, val open: String? = null)

    fun applyTheme(mode: String) {
        vm.settings.themeMode = mode
        val sysDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        ThemeState.dark = when (mode) { "light" -> false; "system" -> sysDark; else -> true }
        val t = android.graphics.Color.TRANSPARENT
        val style = if (ThemeState.dark) SystemBarStyle.dark(t) else SystemBarStyle.light(t, t)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (vm.settings.themeMode == "system") applyTheme("system")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyTheme(vm.settings.themeMode)
        incoming = intent.incoming()
        // حالت نمایشی برای تصاویر فروشگاه: adb shell am start ... --es demo result
        val demo = intent.getStringExtra("demo")
        if (demo != null && BuildConfig.DEBUG) vm.seedDemo()
        setContent {
            SmartPlantTheme {
                AppRoot(vm, incoming, demo.takeIf { BuildConfig.DEBUG }, ::applyTheme) { incoming = null }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.incoming()?.let { incoming = it }
    }

    @Suppress("DEPRECATION")
    private fun Intent.streamUri(): Uri? =
        if (Build.VERSION.SDK_INT >= 33) getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java) else getParcelableExtra<Uri>(Intent.EXTRA_STREAM)

    private fun Intent.incoming(): Incoming? {
        if (action == Intent.ACTION_SEND && type?.startsWith("image/") == true) {
            return streamUri()?.let { Incoming(image = it) }
        }
        return getStringExtra("open")?.let { Incoming(open = it) }
    }
}

object Routes {
    const val SPLASH = "splash"; const val ONBOARD = "onboard"; const val HOME = "home"; const val CAPTURE = "capture"
    const val RESULT = "result"; const val SYMPTOMS = "symptoms"; const val CHAT = "chat"; const val HISTORY = "history"
    const val LIBRARY = "library"; const val PLANT = "plant"; const val PROFILE = "profile"; const val PROBLEMS = "problems"
    const val PROBLEM = "problem"; const val GUIDES = "guides"; const val GUIDE = "guide"; const val ADVISOR = "advisor"
    const val LIGHT = "light"; const val GARDEN = "garden"; const val GARDEN_EDIT = "garden_edit"; const val SETTINGS = "settings"
    const val ABOUT = "about"
}

@Composable
private fun AppRoot(vm: MainViewModel, incoming: MainActivity.Incoming?, demo: String?, onTheme: (String) -> Unit, onConsumed: () -> Unit) {
    val nav = rememberNavController()
    val drawer = rememberDrawerState(if (demo == "drawer") DrawerValue.Open else DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route

    LaunchedEffect(vm.toast) {
        vm.toast?.let { Toast.makeText(ctx, it, Toast.LENGTH_LONG).show(); vm.toast = null }
    }

    fun NavHostController.go(r: String) {
        scope.launch { drawer.close() }
        if (currentDestination?.route == r) return
        navigate(r) { launchSingleTop = true; if (r == Routes.HOME) popUpTo(Routes.HOME) { inclusive = true } }
    }

    fun openPlant(p: PlantInfo) { vm.plant = p; nav.navigate(Routes.PLANT) }
    fun openProblem(id: String) { vm.problemId = id; nav.navigate(Routes.PROBLEM) }
    fun newCapture() { vm.newCapture(); nav.go(Routes.CAPTURE) }

    fun consume(i: MainActivity.Incoming) {
        i.image?.let { vm.newCapture(); vm.addImage(it); nav.go(Routes.CAPTURE) }
        if (i.open == "garden") nav.go(Routes.GARDEN)
        onConsumed()
    }

    LaunchedEffect(incoming) {
        val i = incoming ?: return@LaunchedEffect
        if (nav.currentDestination?.route == Routes.SPLASH) return@LaunchedEffect
        consume(i)
    }

    ModalNavigationDrawer(
        drawerState = drawer,
        gesturesEnabled = route == Routes.HOME,
        drawerContent = { AppDrawer(vm, current = route, onNavigate = { nav.go(it) }, onTheme = onTheme, onDiagnose = { newCapture() }) },
    ) {
        NavHost(
            navController = nav,
            startDestination = if (demo != null) Routes.HOME else Routes.SPLASH,
            enterTransition = { slideInHorizontally(tween(360)) { -it / 4 } + fadeIn(tween(280)) },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(280)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(240)) },
        ) {
            composable(Routes.SPLASH, enterTransition = { fadeIn() }) {
                SplashScreen {
                    nav.navigate(if (vm.settings.onboarded) Routes.HOME else Routes.ONBOARD) { popUpTo(Routes.SPLASH) { inclusive = true } }
                    incoming?.let { consume(it) }
                }
            }
            composable(Routes.ONBOARD) { OnboardingScreen(vm) { nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARD) { inclusive = true } } } }
            composable(Routes.HOME) {
                var demoDone by rememberSaveable { mutableStateOf(false) }
                LaunchedEffect(demo) {
                    if (demoDone || demo == null) return@LaunchedEffect
                    demoDone = true
                    when (demo) {
                        "result" -> nav.navigate(Routes.RESULT)
                        "result_light" -> { onTheme("light"); nav.navigate(Routes.RESULT) }
                        "home_light" -> onTheme("light")
                        "capture" -> { vm.newCapture(); vm.diagnoses.firstOrNull()?.images?.let { vm.shots.addAll(it) }; vm.note = "از دو هفته پیش برگ‌های پایینی زرد شده و خاک همیشه خیس است"; vm.plantHint = "پتوس"; nav.navigate(Routes.CAPTURE) }
                        "symptoms" -> { vm.symptomSel.addAll(listOf("yellow_old", "soil_wet", "gnats")); nav.navigate(Routes.SYMPTOMS) }
                        "chat" -> { vm.startChat(null); vm.demoChat(); nav.navigate(Routes.CHAT) }
                        "library" -> nav.navigate(Routes.LIBRARY)
                        "plant" -> { ir.aminim.smartplant.knowledge.PlantDb.byId("monstera")?.let { openPlant(it) } }
                        "problem" -> openProblem("spider_mite")
                        "advisor" -> nav.navigate(Routes.ADVISOR)
                        "light" -> nav.navigate(Routes.LIGHT)
                        "garden" -> nav.navigate(Routes.GARDEN)
                        "garden_light" -> { onTheme("light"); nav.navigate(Routes.GARDEN) }
                        "plant_light" -> { onTheme("light"); ir.aminim.smartplant.knowledge.PlantDb.byId("monstera")?.let { openPlant(it) } }
                        "result_problems" -> nav.navigate(Routes.RESULT)
                        "guides" -> nav.navigate(Routes.GUIDES)
                        "history" -> nav.navigate(Routes.HISTORY)
                        "settings" -> nav.navigate(Routes.SETTINGS)
                        "about" -> nav.navigate(Routes.ABOUT)
                        "onboard" -> nav.navigate(Routes.ONBOARD)
                    }
                }
                HomeScreen(
                    vm,
                    onMenu = { scope.launch { drawer.open() } },
                    onTheme = onTheme,
                    onCapture = { nav.go(Routes.CAPTURE) },
                    onDescribe = { newCapture() },
                    onOpen = { vm.open(it); nav.go(Routes.RESULT) },
                    onNavigate = { r -> if (r == Routes.CHAT) vm.startChat(null); nav.go(r) },
                )
            }
            composable(Routes.CAPTURE) {
                CaptureScreen(vm, onBack = { nav.popBackStack() }, onAnalyze = { nav.navigate(Routes.RESULT) { popUpTo(Routes.HOME) } }, onSymptoms = { nav.navigate(Routes.SYMPTOMS) })
            }
            composable(Routes.RESULT) {
                ResultScreen(
                    vm, onBack = { nav.popBackStack() },
                    onChat = { vm.startChat(it); nav.navigate(Routes.CHAT) },
                    onGarden = { d -> vm.editing = vm.newGardenPlant(d = d); nav.navigate(Routes.GARDEN_EDIT) },
                    onPlant = { id -> ir.aminim.smartplant.knowledge.PlantDb.byId(id)?.let { openPlant(it) } },
                    onProblem = { openProblem(it) },
                    onSymptoms = { nav.navigate(Routes.SYMPTOMS) },
                )
            }
            composable(Routes.SYMPTOMS) { SymptomScreen(vm, onBack = { nav.popBackStack() }, onResult = { nav.navigate(Routes.RESULT) }) }
            composable(Routes.CHAT) { ChatScreen(vm, onBack = { nav.popBackStack() }) }
            composable(Routes.HISTORY) { HistoryScreen(vm, onBack = { nav.popBackStack() }, onOpen = { vm.open(it); nav.navigate(Routes.RESULT) }) }
            composable(Routes.LIBRARY) {
                LibraryScreen(vm, onBack = { nav.popBackStack() }, onPlant = { openPlant(it) }, onAiProfile = { vm.askProfile(it); nav.navigate(Routes.PROFILE) })
            }
            composable(Routes.PLANT) {
                PlantScreen(
                    vm, onBack = { nav.popBackStack() }, onProblem = { openProblem(it) },
                    onGarden = { p -> vm.editing = vm.newGardenPlant(p); nav.navigate(Routes.GARDEN_EDIT) },
                    onAsk = { q -> vm.startChat(null); vm.chatDraft = q; nav.navigate(Routes.CHAT) },
                )
            }
            composable(Routes.PROFILE) { AiProfileScreen(vm, onBack = { nav.popBackStack() }) }
            composable(Routes.PROBLEMS) { ProblemsScreen(onBack = { nav.popBackStack() }, onProblem = { openProblem(it) }) }
            composable(Routes.PROBLEM) { ProblemScreen(vm, onBack = { nav.popBackStack() }, onPlant = { openPlant(it) }) }
            composable(Routes.GUIDES) { GuidesScreen(onBack = { nav.popBackStack() }, onGuide = { vm.guideId = it; nav.navigate(Routes.GUIDE) }) }
            composable(Routes.GUIDE) { GuideScreen(vm, onBack = { nav.popBackStack() }) }
            composable(Routes.ADVISOR) { AdvisorScreen(vm, onBack = { nav.popBackStack() }, onPlant = { openPlant(it) }, onLight = { nav.navigate(Routes.LIGHT) }) }
            composable(Routes.LIGHT) {
                LightMeterScreen(vm, onBack = { nav.popBackStack() }, onPlant = { openPlant(it) }, onUse = { lv ->
                    vm.advisor = vm.advisor.copy(light = lv)
                    nav.navigate(Routes.ADVISOR) { popUpTo(Routes.ADVISOR) { inclusive = true } }
                })
            }
            composable(Routes.GARDEN) {
                GardenScreen(vm, onBack = { nav.popBackStack() }, onEdit = { vm.editing = it; nav.navigate(Routes.GARDEN_EDIT) }, onPlant = { openPlant(it) })
            }
            composable(Routes.GARDEN_EDIT) { GardenEditScreen(vm, onBack = { nav.popBackStack() }) }
            composable(Routes.SETTINGS) { SettingsScreen(vm, onBack = { nav.popBackStack() }, onTheme = onTheme, onAbout = { nav.navigate(Routes.ABOUT) }) }
            composable(Routes.ABOUT) { AboutScreen(onBack = { nav.popBackStack() }) }
        }
    }
}
