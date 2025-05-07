package com.example.hesaplayici

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.graphics.Canvas
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import java.text.DecimalFormat
import java.text.NumberFormat
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.tasks.Task
import com.example.hesaplayici.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: CalculatorViewModel by viewModels()
    private lateinit var textViewResult: TextView
    private lateinit var textViewHistory: TextView
    private var currentNumberFormat: String = ""

    private var currentExpression = StringBuilder()
    private var lastAnswer = 0.0
    private var memory = 0.0
    private var loadingCompleted = false // Yeni değişken
    private lateinit var appUpdateManager: AppUpdateManager
    private lateinit var updateActivityResultLauncher: ActivityResultLauncher<Intent>
    private val MY_REQUEST_CODE = 100
    private lateinit var installStateUpdatedListener: InstallStateUpdatedListener

    private var isUpdateInProgress = false

        override fun onCreate(savedInstanceState: Bundle?) {
            val splashScreen = installSplashScreen()
            var isLoading = true
            splashScreen.setKeepOnScreenCondition { isLoading }

            // Kullanıcı tercihine göre temayı ayarla
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            val darkMode = sharedPreferences.getBoolean("dark_mode", false) // Varsayılan olarak false (açık tema)

            if (darkMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            setContentView(R.layout.activity_splash)
            super.onCreate(savedInstanceState)
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            viewModel.currentNumberFormat.observe(this) { format ->
                currentNumberFormat = format
                if (currentExpression.isNotEmpty() && currentExpression.toString().toDoubleOrNull() != null){
                    updateResult()
                }
            }



            initializeViews(savedInstanceState)
            setupViewModelObservers()
            restoreInstanceState(savedInstanceState)
            configureActionBar()
            // In-app Update
            appUpdateManager = AppUpdateManagerFactory.create(this)
            updateActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode != Activity.RESULT_OK) {
                    Log.e("MY_APP", "Update flow failed! Result code: " + result.resultCode)
                    // If the update is cancelled or fails, you can request to start the update again.
                    checkUpdateAvailability()
                }
            }

            installStateUpdatedListener = InstallStateUpdatedListener { state ->
                if (state.installStatus() == InstallStatus.DOWNLOADED) {
                    popupSnackbarForCompleteUpdate()
                } else if (state.installStatus() == InstallStatus.INSTALLED) {
                    appUpdateManager.unregisterListener(installStateUpdatedListener)
                } else {
                    Log.d("MY_APP", "InstallStateUpdatedListener: state: ${state.installStatus()}")
                }
            }

            appUpdateManager.registerListener(installStateUpdatedListener)

            checkUpdateAvailability()


            // In-App update check.
            val appUpdateInfoTask: Task<AppUpdateInfo> = appUpdateManager.appUpdateInfo

            appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && appUpdateInfo.isUpdateTypeAllowed(
                        AppUpdateType.IMMEDIATE
                    )
                ) {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.IMMEDIATE,
                        this,
                        MY_REQUEST_CODE
                    )
                } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.IMMEDIATE,
                        this,
                        MY_REQUEST_CODE
                    )
                }
            }
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    delay(100)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Loading error", e)
                }

                CoroutineScope(Dispatchers.Main).launch {
                    loadingCompleted = true
                    isLoading = false
                }
            }

        }
    override fun onResume() {
        super.onResume()
        appUpdateManager
            .appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                // If the update is downloaded but not installed,
                // notify the user to complete the update.
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    popupSnackbarForCompleteUpdate()
                }

                //Check if Immediate update is in progress
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.IMMEDIATE,
                        this,
                        MY_REQUEST_CODE
                    )
                }
            }
    }
    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager.unregisterListener(installStateUpdatedListener)
    }


    private fun popupSnackbarForCompleteUpdate() {
        Snackbar.make(
            findViewById(R.id.main_layout),
            "An update has just been downloaded.",
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction("Restart") { appUpdateManager.completeUpdate()

            }
            setActionTextColor(resources.getColor(R.color.accent))
            show()
        }
    }


    private fun checkUpdateAvailability() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                Snackbar.make(
                    findViewById(R.id.main_layout),
                    "Yeni güncelleme mevcut",
                    Snackbar.LENGTH_INDEFINITE
                ).apply {
                    setAction("Yükle") {
                        isUpdateInProgress = true
                        if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                            appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                AppUpdateType.IMMEDIATE,
                                this@MainActivity,
                                MY_REQUEST_CODE
                            )
                        } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                            appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                AppUpdateType.FLEXIBLE,
                                this@MainActivity,
                                MY_REQUEST_CODE
                            )
                        }
                    }
                    setActionTextColor(resources.getColor(R.color.accent))
                    show()
                }
            }
        }.addOnFailureListener { e -> showToast("Hata: ${e.message}") }
    }

    private fun initializeViews(savedInstanceState: Bundle?) { // Parametre ekle
        textViewResult = binding.textViewResult
        textViewHistory = binding.textViewHistory

        if (savedInstanceState == null) { // Artık parametreden erişilebilir
            resetDisplayTexts()
        }
    }


    private fun resetDisplayTexts() {
        textViewResult.text = "Sonuç"
        textViewHistory.text = "Geçmiş"
    }

    private fun setupViewModelObservers() {
        viewModel.currentInput.observe(this) { input ->
            if (input.isNotEmpty()) textViewResult.text = formatNumber(input)
        }

        viewModel.history.observe(this) { history ->
            if (history.isNotEmpty()) textViewHistory.text = history
        }
    }

    private fun restoreInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            currentExpression = StringBuilder(it.getString("currentExpression", ""))
            lastAnswer = it.getDouble("lastAnswer", 0.0)
            memory = it.getDouble("memory", 0.0)
            textViewResult.text = it.getString("resultText", "Sonuç")
            textViewHistory.text = it.getString("historyText", "Geçmiş")
        }
    }

    private fun configureActionBar() {
        supportActionBar?.apply {
            title = "Hesaplayıcı"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)

            val logoDrawable: Drawable = resources.getDrawable(R.mipmap.ic_launcher_round, theme)
            val scaledBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && logoDrawable is AdaptiveIconDrawable) {
                val foreground = logoDrawable.foreground
                val bitmap = Bitmap.createBitmap(foreground.intrinsicWidth, foreground.intrinsicHeight, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                foreground.setBounds(0, 0, canvas.width, canvas.height)
                foreground.draw(canvas)
                Bitmap.createScaledBitmap(bitmap, dpToPx(48), dpToPx(48), true)
            } else {
                if (logoDrawable is BitmapDrawable) {
                    Bitmap.createScaledBitmap(logoDrawable.bitmap, dpToPx(48), dpToPx(48), true)
                } else {
                    val bitmap = Bitmap.createBitmap(logoDrawable.intrinsicWidth, logoDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    logoDrawable.setBounds(0, 0, canvas.width, canvas.height)
                    logoDrawable.draw(canvas)
                    Bitmap.createScaledBitmap(bitmap, dpToPx(48), dpToPx(48), true)
                }
            }
            setIcon(BitmapDrawable(resources, scaledBitmap))
            setDisplayUseLogoEnabled(true)
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        AlertDialog.Builder(this, R.style.MyAlertDialogStyle)
            .setTitle("Uygulamadan Çıkılsın mı?")
            .setMessage("Hesaplamalarınız kaydedilmeyecek. Emin misiniz?")
            .setIcon(R.drawable.ic_dialog_alert)
            .setPositiveButton("Evet, Çıkış Yap") { _, _ -> super.onBackPressed() }
            .setNegativeButton("Hayır, Devam Et", null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_help -> {
                showHelpDialog()
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showHelpDialog() {
        AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Light_Dialog_Alert)
            .setTitle("Yardım")
            .setMessage(R.string.help_message)
            .setPositiveButton("Anladım", null)
            .show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.apply {
            putString("currentExpression", currentExpression.toString())
            putDouble("lastAnswer", lastAnswer)
            putDouble("memory", memory)
            putString("resultText", textViewResult.text.toString())
            putString("historyText", textViewHistory.text.toString())
        }
    }



    fun onEqualsClick(view: View) {
        try {
            if (currentExpression.isEmpty()) {
                showToast("Hata: Boş ifade hesaplanamaz")
                return
            }

            val expression = currentExpression.toString() // Hesaplama ifadesini al
            var result = evaluateExpression(expression) // Hesaplamayı yap
            var resultText = formatNumber(result.toString())

           if (currentNumberFormat != "standard"){
               resultText=result.toString()
           }else {
                // Tam sayı kontrolü
                result.toString()
            }

            textViewHistory.text = "$expression =" // Geçmişi güncelle
            textViewResult.text = resultText // Sonucu göster

            lastAnswer = result // Son cevabı güncelle
            currentExpression.clear() // İfadeyi temizle
            currentExpression.append(resultText) // Sonucu ifadeye ekle

            // Hesaplama geçmişini kaydet
            val historyViewModel = ViewModelProvider(this).get(HistoryViewModel::class.java)
            historyViewModel.historyList.value?.add("$expression = $resultText")
            historyViewModel.historyList.value?.let { historyViewModel.saveHistory(it) }

        } catch (e: ArithmeticException) {
            showToast("Hata: Sıfıra bölme hatası")
        } catch (e: NumberFormatException) {
            showToast("Hata: Geçersiz sayı formatı")
        } catch (e: Exception) {
            showToast("Hata: ${e.message}")
        }
    }

    private fun evaluateExpression(expression: String): Double {
        Log.d("Expression", "Current Expression: $expression")

        if (expression.isEmpty()) {
            throw IllegalArgumentException("Geçersiz ifade: İfade boş olamaz")
        }

        return object {
            var pos = -1
            var ch = ' '

            fun nextChar() {
                do {
                    ch = if (++pos < expression.length) expression[pos] else '\u0000'
                } while (ch == ' ') // Boşlukları atla
            }


            fun eat(c: Char): Boolean {
                if (ch == c) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val result = parseExpression()
                if (pos < expression.length) throw RuntimeException("Geçersiz Giriş: Beklenmeyen karakter '$ch' konum $pos")
                return result
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    when {
                        eat('+') -> x += parseTerm()
                        eat('-') -> x -= parseTerm()
                        else -> return x
                    }
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    when {
                        eat('*') || eat('x') -> x *= parseFactor()
                        eat('/') -> x /= parseFactor()
                        else -> return x
                    }
                }
            }

            fun parseFactor(): Double {
                if (eat('+')) return parseFactor()
                if (eat('-')) return -parseFactor()

                var result: Double
                val startPos = pos

                // Fonksiyonları kontrol et (sin, cos, tan, log, ln)
                if (ch.isLetter()) {
                    // Fonksiyon adını oku (ör: "sin")
                    while (ch.isLetter()) nextChar()
                    val funcName = expression.substring(startPos, pos)

                    // Parantez içindeki değeri al
                    if (!eat('(')) throw RuntimeException("Fonksiyonlardan sonra '(' bekleniyor")
                    val arg = parseExpression()
                    if (!eat(')')) throw RuntimeException("Fonksiyonlardan sonra ')' bekleniyor")

                    // Fonksiyonu uygula
                    result = when (funcName) {
                        "sin" -> sin(Math.toRadians(arg)) // Derece modunda
                        "cos" -> cos(Math.toRadians(arg))
                        "tan" -> tan(Math.toRadians(arg))
                        "log" -> log10(arg)
                        "ln" -> ln(arg)
                        else -> throw RuntimeException("Tanımsız fonksiyon: $funcName")
                    }
                }
                // Karekök işlemi
                else if (ch == '√') {
                    nextChar()
                    result = sqrt(parseFactor())
                }
                // Parantez veya sayı
                else if (eat('(')) {
                    result = parseExpression()
                    if (!eat(')')) throw RuntimeException("Beklenen ')'")
                }
                else if (ch.isDigit() || ch == '.') {
                    while (ch.isDigit() || ch == '.') nextChar()
                    result = expression.substring(startPos, pos).toDouble()
                }
                else {
                    throw RuntimeException("Beklenmeyen karakter: '$ch' konum $pos")
                }

                // Üs ve faktöriyel işlemleri
                if (eat('^')) result = result.pow(parseFactor())
                if (eat('!')) {
                    if (result != result.toLong().toDouble()) throw RuntimeException("Faktöriyel sadece tam sayılar için")
                    result = factorial(result.toInt())
                }

                return result
            }

            fun factorial(n: Int): Double {
                if (n < 0) throw RuntimeException("Negatif sayıların faktöriyeli hesaplanamaz")
                var result = 1.0
                for (i in 1..n) {
                    result *= i
                }
                return result
            }
        }.parse()
    }

    fun onMemoryClick(view: View) {
        when (view.id) {
            R.id.button_mc -> { // Memory Clear (MC)
                memory = 0.0
                showToast("Hafıza temizlendi")
            }
            R.id.button_mr -> { // Memory Recall (MR)
                currentExpression.clear()
                currentExpression.append(memory.toString())
                updateResult()
            }
            R.id.button_m_plus -> { // Memory Plus (M+)
                try {
                    memory += evaluateExpression(currentExpression.toString())
                    showToast("Hafızaya eklendi: $memory")
                } catch (e: Exception) {
                    showToast("Hata: ${e.message}")
                }
            }
            R.id.button_m_minus -> { // Memory Minus (M-)
                try {
                    memory -= evaluateExpression(currentExpression.toString())
                    showToast("Hafızadan çıkarıldı: $memory")
                } catch (e: Exception) {
                    showToast("Hata: ${e.message}")
                }
            }
            R.id.button_ms -> { // Memory Store (MS)
                try {
                    memory = evaluateExpression(currentExpression.toString())
                    showToast("Hafızaya kaydedildi: $memory")
                } catch (e: Exception) {
                    showToast("Hata: ${e.message}")
                }
            }
        }
    }
    fun onPercentageClick(view: View) {
        try {
            // Mevcut ifadeyi değerlendir ve 100'e böl
            val result = evaluateExpression(currentExpression.toString()) / 100
            textViewResult.text = result.toString() // Sonucu ekranda göster
            currentExpression.clear()
            currentExpression.append(result) // Sonucu currentExpression'a kaydet
            showToast("Yüzde hesaplandı: $result")
        } catch (e: Exception) {
            showToast("Hata: ${e.message}")
        }
    }

    fun onDeleteClick(view: View) {
        if (currentExpression.isNotEmpty()) {
            currentExpression.deleteCharAt(currentExpression.length - 1) // Son karakteri sil
            updateResult() // Ekranı güncelle
        } else {
            showToast("Silinecek bir şey yok") // Boş ifade durumunda kullanıcıya bilgi ver
        }
    }

    fun onAnswerClick(view: View) {
        // "Ans" butonuna tıklandığında son cevabı ekrana yazdır
        currentExpression.clear()
        currentExpression.append(lastAnswer.toString())
        textViewResult.text = lastAnswer.toString()
        showToast("Son cevap kullanıldı: $lastAnswer")
    }

    fun onFunctionClick(view: View) {
        val button = view as Button
        val functionName = button.text.toString()

        currentExpression.append("$functionName(")
        updateResult()
    }

    fun onNumberClick(view: View) {
        val button = view as Button
        currentExpression.append(button.text)
        updateResult()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    fun onOperationClick(view: View) {
        val button = view as Button
        val operation = button.text.toString()

        if (currentExpression.isNotEmpty() && "+-*/^".contains(currentExpression.last())) {
            showToast("Arka arkaya işlem operatörleri kullanılamaz")
        } else {
            currentExpression.append(operation)
            updateResult()
        }
    }

    fun onClearClick(view: View) {
        currentExpression.clear()
        textViewResult.text = "Sonuç"
        textViewHistory.text = "Geçmiş"
        showToast("Temizlendi")
    }

    private fun updateResult() {
        textViewResult.text = formatNumber(currentExpression.toString())
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
fun MainActivity.formatNumber(numberString: String): String {
    return try {
        val number = numberString.toDouble()
        val format = when (currentNumberFormat) {
            "scientific" -> DecimalFormat("0.######E0")
            "engineering" -> DecimalFormat("0.###E0")
            else -> NumberFormat.getNumberInstance()
        }
        format.format(number)
    } catch (e: NumberFormatException) {
        numberString
    }
}





