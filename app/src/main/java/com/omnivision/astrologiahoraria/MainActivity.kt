package com.omnivision.astrologiahoraria

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.material3.DrawerValue.Closed
import androidx.compose.material3.DrawerValue.Open
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val PREFS = "ah_prefs"
private const val KEY_HISTORY = "history_json"

data class HistoryEntry(
    val question: String,
    val utcIso: String,
    val tzId: String,
    val lat: Double?,
    val lon: Double?,
    val verdict: String,
    val confidence: Int
)

class MainActivity : ComponentActivity() {

    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { /* handled in UI */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App(requestLocationPermission) }
    }
}

@Composable
private fun App(requestPermLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>) {
    val nav = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = Closed)
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    var darkMystic by remember { mutableStateOf(true) }
    var advancedMode by remember { mutableStateOf(false) }

    MysticTheme(darkMystic) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                DrawerContent(
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        nav.navigate(route) { launchSingleTop = true }
                    },
                    advancedMode = advancedMode,
                    onToggleAdvanced = { advancedMode = !advancedMode }
                )
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Astrologia Horária") },
                        navigationIcon = {
                            IconButton(onClick = {
                                scope.launch { drawerState.open() }
                            }) { Text("☰") }
                        }
                    )
                }
            ) { pad ->
                NavHost(
                    navController = nav,
                    startDestination = "splash",
                    modifier = Modifier.padding(pad)
                ) {
                    composable("splash") { SplashScreen { nav.navigate("home") { popUpTo("splash") { inclusive = true } } } }
                    composable("home") { HomeScreen(onStart = { nav.navigate("question") }) }
                    composable("question") {
                        QuestionScreen(
                            onOpenHistory = { nav.navigate("history") },
                            onSubmit = { q -> nav.navigate("capture?question=${encode(q)}") }
                        )
                    }
                    composable("capture?question={question}") { backStack ->
                        val q = decode(backStack.arguments?.getString("question") ?: "")
                        CaptureAndComputeScreen(
                            question = q,
                            requestPermLauncher = requestPermLauncher,
                            onDone = { resultId -> nav.navigate("result?id=$resultId") },
                            onBack = { nav.popBackStack() }
                        )
                    }
                    composable("result?id={id}") { backStack ->
                        val id = backStack.arguments?.getString("id") ?: ""
                        ResultScreen(
                            id = id,
                            advancedMode = advancedMode,
                            onNewQuestion = {
                                nav.navigate("question") {
                                    popUpTo("question") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    composable("history") { HistoryScreen(onOpen = { id -> nav.navigate("result?id=$id") }) }
                    composable("settings") {
                        SettingsScreen(
                            darkMystic = darkMystic,
                            onToggleTheme = { darkMystic = !darkMystic }
                        )
                    }
                    composable("what") { InfoScreen(title = "O que é Astrologia Horária", body = whatIsHoraryText()) }
                    composable("how") { InfoScreen(title = "Como usar o app", body = howToUseText()) }
                    composable("about") { InfoScreen(title = "Sobre o app", body = aboutText()) }
                    composable("privacy") { InfoScreen(title = "Política de Privacidade", body = privacyText()) }
                    composable("new") { nav.navigate("question") { launchSingleTop = true } }
                }
            }
        }
    }
}

@Composable
private fun DrawerContent(
    onNavigate: (String) -> Unit,
    advancedMode: Boolean,
    onToggleAdvanced: () -> Unit
) {
    ModalDrawerSheet {
        Spacer(Modifier.height(12.dp))
        DrawerItem("Histórico") { onNavigate("history") }
        DrawerItem("Nova Pergunta") { onNavigate("question") }
        DrawerItem("Configurações") { onNavigate("settings") }
        Divider(Modifier.padding(vertical = 8.dp))
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Modo Avançado", modifier = Modifier.weight(1f))
            Switch(checked = advancedMode, onCheckedChange = { onToggleAdvanced() })
        }
        Divider(Modifier.padding(vertical = 8.dp))
        DrawerItem("O que é Astrologia Horária") { onNavigate("what") }
        DrawerItem("Como usar o app") { onNavigate("how") }
        DrawerItem("Sobre o app") { onNavigate("about") }
        DrawerItem("Política de Privacidade") { onNavigate("privacy") }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable private fun DrawerItem(label: String, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}

@Composable
private fun SplashScreen(onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1400)
        onDone()
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ASTROLOGIA", style = MaterialTheme.typography.headlineLarge)
            Text("HORÁRIA", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text("Omni Vision", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun HomeScreen(onStart: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text("O momento contém a resposta.", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(18.dp))
            Button(onClick = onStart) { Text("Abrir o Oráculo") }
        }
    }
}

@Composable
private fun QuestionScreen(onOpenHistory: () -> Unit, onSubmit: (String) -> Unit) {
    var q by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Escreva sua pergunta com clareza.", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Box(Modifier.padding(12.dp)) {
                BasicTextField(
                    value = q,
                    onValueChange = { q = it },
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { if (q.trim().isNotEmpty()) onSubmit(q.trim()) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Fixar o Momento") }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth()) { Text("Ver Histórico") }
        Spacer(Modifier.height(18.dp))
        Text("Nota: interpretação simbólica. Não substitui aconselhamento médico, jurídico ou financeiro.",
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CaptureAndComputeScreen(
    question: String,
    requestPermLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    onDone: (String) -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    var status by remember { mutableStateOf("Registrando tempo e local...") }
    var busy by remember { mutableStateOf(true) }

    LaunchedEffect(question) {
        // Timestamp é capturado imediatamente e é imutável (regra B)
        val now = Date()
        val tz = TimeZone.getDefault().id
        val utcIso = isoUtc(now)

        val hasFine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            status = "Solicitando permissão de localização..."
            requestPermLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            delay(600)
        }

        status = "Obtendo localização..."
        val loc = getLocationBestEffort(ctx)
        status = "Calculando leitura..."
        delay(700)

        // Motor ainda stub no v0: gera veredito demonstrativo + confiança
        val verdict = "SIM COM CONDIÇÕES"
        val confidence = 68

        val entry = HistoryEntry(
            question = question,
            utcIso = utcIso,
            tzId = tz,
            lat = loc?.first,
            lon = loc?.second,
            verdict = verdict,
            confidence = confidence
        )
        val id = saveHistory(ctx, entry)
        busy = false
        onDone(id)
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
        Text(status, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack) { Text("Voltar") }
    }
}

@Composable
private fun ResultScreen(id: String, advancedMode: Boolean, onNewQuestion: () -> Unit) {
    val ctx = LocalContext.current
    val entry = remember(id) { loadHistoryById(ctx, id) }

    if (entry == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Resultado não encontrado.")
        }
        return
    }

    val localStr = if (entry.lat != null && entry.lon != null) {
        "Lat ${"%.5f".format(entry.lat)} | Lon ${"%.5f".format(entry.lon)}"
    } else "Localização indisponível"

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Veredito: ${entry.verdict}", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text("Confiança Estrutural: ${entry.confidence}%", style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.height(12.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Resposta", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Há indicação de mudança, porém a concretização depende de ação estratégica e consistência. " +
                            "O mapa sugere oportunidade real, com pontos de atrito que exigem método."
                )
                Spacer(Modifier.height(10.dp))
                Text("Timing: 4–8 semanas (estimativa)", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Gráfico Zodiacal", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("v0: gráfico será desenhado aqui (signos + casas + planetas + aspectos).")
            }
        }

        Spacer(Modifier.height(12.dp))
        if (advancedMode) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("Modo Avançado", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("UTC: ${entry.utcIso}")
                    Text("Timezone: ${entry.tzId}")
                    Text(localStr)
                    Text("Score Final (v0): +4")
                    Text("Classificação: YES_CONDITIONAL")
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(8.dp))
        Button(onClick = onNewQuestion, modifier = Modifier.fillMaxWidth()) {
            Text("Fazer Nova Pergunta")
        }
        Spacer(Modifier.height(10.dp))
        Text("Pergunta salva no histórico automaticamente.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun HistoryScreen(onOpen: (String) -> Unit) {
    val ctx = LocalContext.current
    val list = remember { loadHistory(ctx) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Histórico", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        if (list.isEmpty()) {
            Text("Nenhuma pergunta registrada ainda.")
        } else {
            list.forEach { (id, e) ->
                Card(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(e.question, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text("${e.verdict} • ${e.confidence}%")
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(onClick = { onOpen(id) }) { Text("Abrir") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(darkMystic: Boolean, onToggleTheme: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Configurações", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tema Místico", modifier = Modifier.weight(1f))
            Switch(checked = darkMystic, onCheckedChange = { onToggleTheme() })
        }
        Spacer(Modifier.height(12.dp))
        Text("Idioma e opções avançadas entrarão no próximo build.", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun InfoScreen(title: String, body: String) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Tema: v0 simples (místico escuro vs minimal claro) */
@Composable
private fun MysticTheme(mystic: Boolean, content: @Composable () -> Unit) {
    val scheme = if (mystic) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = scheme, content = content)
}

/** Utils */

private fun isoUtc(d: Date): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(d)
}

@SuppressLint("MissingPermission")
private suspend fun getLocationBestEffort(ctx: Context): Pair<Double, Double>? {
    return try {
        val client = LocationServices.getFusedLocationProviderClient(ctx)
        val task = client.lastLocation
        // espera “best effort”
        var waited = 0
        while (!task.isComplete && waited < 8) {
            delay(250)
            waited++
        }
        val loc = task.result
        if (loc != null) Pair(loc.latitude, loc.longitude) else null
    } catch (_: Throwable) {
        null
    }
}

private fun saveHistory(ctx: Context, entry: HistoryEntry): String {
    val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val arr = JSONArray(prefs.getString(KEY_HISTORY, "[]") ?: "[]")
    val id = System.currentTimeMillis().toString()

    val obj = JSONObject().apply {
        put("id", id)
        put("q", entry.question)
        put("utc", entry.utcIso)
        put("tz", entry.tzId)
        put("lat", entry.lat)
        put("lon", entry.lon)
        put("v", entry.verdict)
        put("c", entry.confidence)
    }
    // prepend
    val newArr = JSONArray().put(obj)
    for (i in 0 until arr.length()) newArr.put(arr.getJSONObject(i))
    prefs.edit().putString(KEY_HISTORY, newArr.toString()).apply()
    return id
}

private fun loadHistory(ctx: Context): List<Pair<String, HistoryEntry>> {
    val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val arr = JSONArray(prefs.getString(KEY_HISTORY, "[]") ?: "[]")
    val out = ArrayList<Pair<String, HistoryEntry>>()
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val id = o.getString("id")
        val e = HistoryEntry(
            question = o.getString("q"),
            utcIso = o.getString("utc"),
            tzId = o.getString("tz"),
            lat = if (o.isNull("lat")) null else o.getDouble("lat"),
            lon = if (o.isNull("lon")) null else o.getDouble("lon"),
            verdict = o.getString("v"),
            confidence = o.getInt("c")
        )
        out.add(id to e)
    }
    return out
}

private fun loadHistoryById(ctx: Context, id: String): HistoryEntry? {
    return loadHistory(ctx).firstOrNull { it.first == id }?.second
}

private fun encode(s: String) = java.net.URLEncoder.encode(s, "UTF-8")
private fun decode(s: String) = java.net.URLDecoder.decode(s, "UTF-8")

private fun whatIsHoraryText() =
    "Astrologia horária é um método tradicional em que o mapa do instante em que a pergunta é formulada " +
    "serve como matriz simbólica para leitura. O app registra tempo e local automaticamente e aplica regras " +
    "estruturadas para interpretar o momento."

private fun howToUseText() =
    "1) Escreva uma pergunta obje
