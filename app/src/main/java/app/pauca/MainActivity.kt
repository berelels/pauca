package app.pauca

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import app.pauca.data.Constants
import app.pauca.data.Palette
import app.pauca.data.Prefs
import app.pauca.databinding.ActivityMainBinding
import app.pauca.focus.FocusManager
import app.pauca.helper.CrashGuard
import app.pauca.helper.Language
import app.pauca.helper.LiteImport
import app.pauca.helper.OlDialog
import app.pauca.helper.hasBeenHours
import app.pauca.helper.isDefaultLauncher
import app.pauca.helper.isEinkDisplay
import app.pauca.helper.isSystemAnimationsDisabled
import app.pauca.helper.isTablet
import app.pauca.helper.resetLauncherViaFakeActivity
import app.pauca.helper.showLauncherSelector
import app.pauca.helper.showMessageDialog
import app.pauca.helper.showToast
import app.pauca.ui.Wallpaper
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var navController: NavController
    private lateinit var viewModel: MainViewModel
    private lateinit var binding: ActivityMainBinding
    private var profileReceiver: BroadcastReceiver? = null
    private var launcherAppsCallback: LauncherApps.Callback? = null
    private var messageDialog: OlDialog? = null

    /**
     * Uma tela abriu outro app de propósito (ex.: o seletor de fotos) e espera a resposta:
     * sair de cena agora não é "ir para o início". Volta a valer no próximo onResume.
     */
    var keepCurrentScreen = false

    private var importedFromLite = false

    override fun attachBaseContext(context: Context) {
        // Antes de tudo: se as últimas aberturas fecharam sozinhas, os ajustes de texto voltam ao padrão
        CrashGuard.install(context)
        CrashGuard.onStart(context)
        // Antes de ler qualquer ajuste: na primeira abertura, o Pauca traz os do Lite
        importedFromLite = LiteImport.runOnce(context)
        // Só o que o app muda (escala da fonte e idioma): copiar a configuração inteira
        // prenderia o resto ao valor de agora
        val newConfig = Configuration()
        newConfig.fontScale = Prefs(context).textSizeScale
        val locale = Language.locale(context)
        Locale.setDefault(locale)
        newConfig.setLocale(locale)
        applyOverrideConfiguration(newConfig)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = Prefs(this)
        AppCompatDelegate.setDefaultNightMode(prefs.appTheme)
        super.onCreate(savedInstanceState)
        if (isEinkDisplay() || isSystemAnimationsDisabled()) theme.applyStyle(R.style.NoAnimationOverlay, true)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyBackground()

        navController = this.findNavController(R.id.nav_host_fragment)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Voltar nunca sai da tela inicial; nas outras, volta uma tela
                if (navController.currentDestination?.id != R.id.mainFragment)
                    navController.popBackStack()
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        if (importedFromLite) showToast(getString(R.string.lite_imported), Toast.LENGTH_LONG)
        CrashGuard.takeReset(this)?.let { CrashGuard.showResetNotice(this, it) }
        if (prefs.firstOpen) {
            prefs.firstOpen = false
            prefs.firstOpenTime = System.currentTimeMillis()
            viewModel.setDefaultClockApp()
            viewModel.resetLauncherLiveData.call()
        }

        initObservers(viewModel)
        viewModel.getAppList()
        registerShortcutCallback()
        setupOrientation()
        FocusManager.refreshLaunchable(this)

        window.addFlags(FLAG_LAYOUT_NO_LIMITS)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            profileReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    viewModel.isPrivateSpaceToggling = false
                    viewModel.getPrivateSpaceAppList()
                }
            }
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_PROFILE_AVAILABLE)
                addAction(Intent.ACTION_PROFILE_UNAVAILABLE)
            }
            registerReceiver(profileReceiver, filter)
        }
        handleIntent(intent)
    }

    /**
     * Pinta o fundo (cor, papel de parede do sistema ou imagem). [look] diferente do salvo
     * serve para a prévia do editor de papel de parede.
     */
    fun applyBackground(
        palette: Palette = prefs.palette,
        look: Wallpaper.Look = Wallpaper.Look.from(prefs),
    ) {
        // A janela fica sempre translúcida e mostrando o papel de parede (vem do tema);
        // quem cobre com a cor sólida é a raiz do layout. Ver Wallpaper.
        window.setBackgroundDrawable(ColorDrawable(0))
        Wallpaper.apply(window, binding.mainActivityLayout, binding.wallpaperImage, binding.wallpaperShade, palette, look)
    }

    override fun onStart() {
        super.onStart()
        restartLauncherIfStale()
    }

    override fun onResume() {
        super.onResume()
        keepCurrentScreen = false
        viewModel.isPrivateSpaceToggling = false
        viewModel.getAppList()
        // Durante o foco, confere se o ouvinte de notificações continua ligado
        if (prefs.focusActive) FocusManager.ensureListener(this)
        window.decorView.postDelayed(markStable, CrashGuard.STABLE_MS)
    }

    private val markStable = Runnable { CrashGuard.onStable(this) }

    override fun onPause() {
        window.decorView.removeCallbacks(markStable)
        super.onPause()
    }

    private fun registerShortcutCallback() {
        val launcherApps = getSystemService(LauncherApps::class.java)
        launcherAppsCallback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: android.os.UserHandle) = Unit
            override fun onPackageAdded(packageName: String, user: android.os.UserHandle) {
                FocusManager.refreshLaunchable(this@MainActivity)
            }

            override fun onPackageChanged(packageName: String, user: android.os.UserHandle) = Unit
            override fun onPackagesAvailable(
                packageNames: Array<out String>,
                user: android.os.UserHandle,
                replacing: Boolean,
            ) = Unit

            override fun onPackagesUnavailable(
                packageNames: Array<out String>,
                user: android.os.UserHandle,
                replacing: Boolean,
            ) = Unit

            override fun onShortcutsChanged(
                packageName: String,
                shortcuts: MutableList<ShortcutInfo>,
                user: android.os.UserHandle,
            ) {
                viewModel.getAppList()
            }
        }
        launcherApps.registerCallback(launcherAppsCallback!!)
    }

    override fun onStop() {
        backToHomeScreen()
        super.onStop()
    }

    override fun onUserLeaveHint() {
        backToHomeScreen()
        super.onUserLeaveHint()
    }

    override fun onNewIntent(intent: Intent) {
        backToHomeScreen()
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** Tocar na regra "Modo foco" nos ajustes do sistema abre os ajustes do foco aqui. */
    private fun handleIntent(intent: Intent?) {
        if (intent?.action != NotificationManager.ACTION_AUTOMATIC_ZEN_RULE) return
        try {
            navController.navigate(R.id.settingsPageFragment, bundleOf(Constants.Key.PAGE to Constants.Page.FOCUS))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initObservers(viewModel: MainViewModel) {
        viewModel.launcherResetFailed.observe(this) {
            openLauncherChooser(it)
        }
        viewModel.resetLauncherLiveData.observe(this) {
            if (isDefaultLauncher() || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                resetLauncherViaFakeActivity()
            else
                showLauncherSelector(Constants.REQUEST_CODE_LAUNCHER_SELECTOR)
        }
        viewModel.showDialog.observe(this) {
            when (it) {
                Constants.Dialog.HIDDEN ->
                    showMessage(R.string.hidden_apps, R.string.hidden_apps_message, R.string.okay) {}

                Constants.Dialog.KEYBOARD ->
                    showMessage(R.string.app_name, R.string.keyboard_message, R.string.okay) {}

                Constants.Dialog.DIGITAL_WELLBEING ->
                    showMessage(R.string.screen_time, R.string.app_usage_message, R.string.permission) {
                        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
            }
        }
    }

    private fun showMessage(title: Int, message: Int, action: Int, clickListener: () -> Unit) {
        messageDialog?.dismiss()
        messageDialog = showMessageDialog(title, message, action, clickListener)
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun setupOrientation() {
        if (isTablet(this) || Build.VERSION.SDK_INT == Build.VERSION_CODES.O)
            return
        // In Android 8.0, windowIsTranslucent cannot be used with screenOrientation=portrait
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    private fun backToHomeScreen() {
        // Recriar (ex.: trocar o tema) não é sair do launcher: fica na tela atual
        if (viewModel.isPrivateSpaceToggling || isChangingConfigurations || keepCurrentScreen) return
        messageDialog?.dismiss()
        if (navController.currentDestination?.id != R.id.mainFragment)
            navController.popBackStack(R.id.mainFragment, false)
    }

    private fun openLauncherChooser(resetFailed: Boolean) {
        if (resetFailed) {
            val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            startActivity(intent)
        }
    }

    /** Recria de tempos em tempos para limpar o cache, como o Olauncher fazia. */
    private fun restartLauncherIfStale() {
        if (prefs.launcherRestartTimestamp.hasBeenHours(4)) {
            prefs.launcherRestartTimestamp = System.currentTimeMillis()
            cacheDir.deleteRecursively()
            recreate()
        }
    }

    override fun onDestroy() {
        messageDialog?.dismiss()
        messageDialog = null
        launcherAppsCallback?.let {
            getSystemService(LauncherApps::class.java).unregisterCallback(it)
        }
        profileReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: Exception) {
            }
        }
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            Constants.REQUEST_CODE_ENABLE_ADMIN -> {
                if (resultCode == Activity.RESULT_OK)
                    prefs.lockModeOn = true
            }

            Constants.REQUEST_CODE_LAUNCHER_SELECTOR -> {
                if (resultCode == Activity.RESULT_OK)
                    resetLauncherViaFakeActivity()
            }
        }
    }
}
