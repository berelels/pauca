package app.pauca

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.pauca.data.AppModel
import app.pauca.data.Constants
import app.pauca.data.Prefs
import app.pauca.focus.FocusManager
import app.pauca.helper.Pause
import app.pauca.helper.PauseRequest
import app.pauca.helper.SingleLiveEvent
import app.pauca.helper.Weather
import app.pauca.helper.formattedTimeSpent
import app.pauca.helper.getAppsList
import app.pauca.helper.getPrivateSpaceApps
import app.pauca.helper.getPrivateSpaceUserHandle
import app.pauca.helper.hasBeenMinutes
import app.pauca.helper.isDefaultHome
import app.pauca.helper.isPackageInstalled
import app.pauca.helper.isPrivateSpaceLocked
import app.pauca.helper.Language
import app.pauca.helper.showToast
import app.pauca.helper.usageStats.EventLogWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar


class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext by lazy { application.applicationContext }
    private val prefs = Prefs(appContext)

    val firstOpen = MutableLiveData<Boolean>()
    val refreshHome = MutableLiveData<Boolean>()
    val updateSwipeApps = MutableLiveData<Any>()
    val appList = MutableLiveData<List<AppModel>?>()
    val hiddenApps = MutableLiveData<List<AppModel>?>()
    val isDefaultHome = MutableLiveData<Boolean>()
    val launcherResetFailed = MutableLiveData<Boolean>()
    val screenTimeValue = MutableLiveData<String>()

    val privateSpaceApps = MutableLiveData<List<AppModel>?>()
    val privateSpaceLocked = MutableLiveData<Boolean>()
    val privateSpaceAvailable = MutableLiveData<Boolean>()

    // Suppress backToHomeScreen during Private Space lock/unlock auth
    var isPrivateSpaceToggling = false

    val showDialog = SingleLiveEvent<String>()
    val resetLauncherLiveData = SingleLiveEvent<Unit?>()
    val pauseRequest = SingleLiveEvent<PauseRequest>()
    val weatherChanged = MutableLiveData<Unit>()
    private var weatherLoading = false

    fun selectedApp(appModel: AppModel, flag: Int) {
        if (appModel is AppModel.PrivateSpaceHeader) return
        when (flag) {
            Constants.FLAG_LAUNCH_APP -> open(appModel)
            Constants.FLAG_HIDDEN_APPS -> if (appModel is AppModel.App) open(appModel)


            Constants.FLAG_SET_SWIPE_LEFT_APP -> saveSwipeApp(appModel, isLeft = true)
            Constants.FLAG_SET_SWIPE_RIGHT_APP -> saveSwipeApp(appModel, isLeft = false)
            Constants.FLAG_SET_CLOCK_APP -> saveClockApp(appModel)
            Constants.FLAG_SET_CALENDAR_APP -> saveCalendarApp(appModel)
            Constants.FLAG_SET_SCREEN_TIME_APP -> saveScreenTimeApp(appModel)
        }
    }

    /** Único caminho de abertura de apps (tela inicial, gaveta, gestos): é aqui que a pausa entra. */
    private fun open(appModel: AppModel) {
        val count = Pause.check(prefs, appModel.appPackage)
        if (count != null) pauseRequest.value = PauseRequest(appModel, count)
        else launch(appModel)
    }

    /** "Abrir mesmo assim": o app fica liberado pelo resto do dia. */
    fun openAnyway(appModel: AppModel) {
        Pause.allowToday(prefs, appModel.appPackage)
        launch(appModel)
    }

    private fun launch(appModel: AppModel) {
        Pause.countOpen(appContext, prefs, appModel.appPackage, appModel.appLabel)
        when (appModel) {
            is AppModel.PinnedShortcut -> launchShortcut(appModel)
            is AppModel.App -> launchApp(appModel.appPackage, appModel.activityClassName, appModel.user)
            else -> {}
        }
    }

    /** Busca o tempo se passou o intervalo (ou se [force]); em falha, fica o último valor. */
    fun refreshWeather(force: Boolean = false) {
        if (!prefs.weatherOn || prefs.weatherCity.isEmpty() || weatherLoading) return
        if (!force && !prefs.weatherAt.hasBeenMinutes(Weather.REFRESH_MINUTES)) return
        weatherLoading = true
        viewModelScope.launch {
            val reading = withContext(Dispatchers.IO) { Weather.fetch(prefs.weatherLat, prefs.weatherLon) }
            weatherLoading = false
            reading ?: return@launch
            prefs.weatherTemp = reading.celsius.toString()
            prefs.weatherCode = reading.code
            prefs.weatherAt = System.currentTimeMillis()
            weatherChanged.value = Unit
        }
    }

    private var citySearch: kotlinx.coroutines.Job? = null

    /** Busca cidades pelo nome; a busca anterior ainda em andamento é cancelada. [onResult] null é falha de rede. */
    fun searchCities(query: String, onResult: (List<Weather.City>?) -> Unit) {
        citySearch?.cancel()
        citySearch = viewModelScope.launch {
            val lang = Language.locale(appContext).language
            onResult(withContext(Dispatchers.IO) { Weather.findCities(query, lang) })
        }
    }

    fun setWeatherCity(city: Weather.City) {
        prefs.weatherCity = city.name
        prefs.weatherLat = city.lat.toString()
        prefs.weatherLon = city.lon.toString()
        prefs.weatherAt = 0L
        refreshWeather(force = true)
    }

    private fun launchShortcut(appModel: AppModel.PinnedShortcut) {
        val launcher = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val query = LauncherApps.ShortcutQuery().apply {
            setPackage(appModel.appPackage)
            setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
        }
        try {
            val shortcut = launcher.getShortcuts(query, appModel.user)
                ?.find { it.id == appModel.shortcutId }
            if (shortcut == null) {
                appContext.showToast(appContext.getString(R.string.shortcut_not_found))
                return
            }
            launcher.startShortcut(shortcut, null, null)
        } catch (_: Exception) {
            appContext.showToast(appContext.getString(R.string.unable_to_open_shortcut))
        }
    }

    private fun saveSwipeApp(appModel: AppModel, isLeft: Boolean) {
        when (appModel) {
            is AppModel.PrivateSpaceHeader -> return
            is AppModel.App -> {
                if (isLeft) {
                    prefs.appNameSwipeLeft = appModel.appLabel
                    prefs.appPackageSwipeLeft = appModel.appPackage
                    prefs.appUserSwipeLeft = appModel.user.toString()
                    prefs.appActivityClassNameSwipeLeft = appModel.activityClassName
                    prefs.isShortcutSwipeLeft = false
                    prefs.shortcutIdSwipeLeft = ""
                } else {
                    prefs.appNameSwipeRight = appModel.appLabel
                    prefs.appPackageSwipeRight = appModel.appPackage
                    prefs.appUserSwipeRight = appModel.user.toString()
                    prefs.appActivityClassNameRight = appModel.activityClassName
                    prefs.isShortcutSwipeRight = false
                    prefs.shortcutIdSwipeRight = ""
                }
            }

            is AppModel.PinnedShortcut -> {
                if (isLeft) {
                    prefs.appNameSwipeLeft = appModel.appLabel
                    prefs.appPackageSwipeLeft = appModel.appPackage
                    prefs.appUserSwipeLeft = appModel.user.toString()
                    prefs.appActivityClassNameSwipeLeft = null
                    prefs.isShortcutSwipeLeft = true
                    prefs.shortcutIdSwipeLeft = appModel.shortcutId
                } else {
                    prefs.appNameSwipeRight = appModel.appLabel
                    prefs.appPackageSwipeRight = appModel.appPackage
                    prefs.appUserSwipeRight = appModel.user.toString()
                    prefs.appActivityClassNameRight = null
                    prefs.isShortcutSwipeRight = true
                    prefs.shortcutIdSwipeRight = appModel.shortcutId
                }
            }
        }
        updateSwipeApps()
    }

    private fun saveClockApp(appModel: AppModel) {
        if (appModel is AppModel.App) {
            prefs.clockAppPackage = appModel.appPackage
            prefs.clockAppUser = appModel.user.toString()
            prefs.clockAppClassName = appModel.activityClassName
        }
    }

    private fun saveCalendarApp(appModel: AppModel) {
        if (appModel is AppModel.App) {
            prefs.calendarAppPackage = appModel.appPackage
            prefs.calendarAppUser = appModel.user.toString()
            prefs.calendarAppClassName = appModel.activityClassName
        }
    }

    private fun saveScreenTimeApp(appModel: AppModel) {
        if (appModel is AppModel.App) {
            prefs.screenTimeAppPackage = appModel.appPackage
            prefs.screenTimeAppUser = appModel.user.toString()
            prefs.screenTimeAppClassName = appModel.activityClassName
        }
    }

    fun firstOpen(value: Boolean) {
        firstOpen.postValue(value)
    }

    fun refreshHome(appCountUpdated: Boolean) {
        refreshHome.value = appCountUpdated
    }

    private fun updateSwipeApps() {
        updateSwipeApps.postValue(Unit)
    }

    private fun launchApp(packageName: String, activityClassName: String?, userHandle: UserHandle) {
        val launcher = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val activityInfo = launcher.getActivityList(packageName, userHandle)

        val isActivityValid = activityClassName.isNullOrBlank().not()
                && activityInfo.any { it.componentName.className == activityClassName }

        val component = if (isActivityValid)
            ComponentName(packageName, activityClassName)
        else {
            when (activityInfo.size) {
                0 -> {
                    appContext.showToast(appContext.getString(R.string.app_not_found))
                    return
                }

                1 -> ComponentName(packageName, activityInfo[0].name)
                else -> ComponentName(packageName, activityInfo[activityInfo.size - 1].name)
            }.also { prefs.updateAppActivityClassName(packageName, it.className) }
        }

        try {
            launcher.startMainActivity(component, userHandle, null, null)
        } catch (e: SecurityException) {
            try {
                launcher.startMainActivity(component, android.os.Process.myUserHandle(), null, null)
            } catch (e: Exception) {
                appContext.showToast(appContext.getString(R.string.unable_to_open_app))
            }
        } catch (e: Exception) {
            appContext.showToast(appContext.getString(R.string.unable_to_open_app))
        }
    }

    fun getAppList(includeHiddenApps: Boolean = false) {
        viewModelScope.launch {
            val apps = getAppsList(appContext, prefs, includeRegularApps = true, includeHiddenApps)
            appList.value = if (prefs.focusActive && prefs.focusHideApps && !includeHiddenApps) {
                // No foco, a gaveta mostra só o que pode ser aberto
                val allowed = FocusManager.allowedPackages(appContext)
                apps.filter { it.appPackage in allowed }
            } else apps
        }
        getPrivateSpaceAppList()
    }

    fun getHiddenApps() {
        viewModelScope.launch {
            hiddenApps.value =
                getAppsList(appContext, prefs, includeRegularApps = false, includeHiddenApps = true)
        }
    }

    fun isDefaultHome() {
        isDefaultHome.value = isDefaultHome(appContext)
    }

    fun getTodaysScreenTime() {
        if (prefs.screenTimeLastUpdated.hasBeenMinutes(1).not()) return

        val eventLogWrapper = EventLogWrapper(
            appContext
        )
        // Start of today in millis
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val timeSpent = eventLogWrapper.aggregateSimpleUsageStats(
            eventLogWrapper.aggregateForegroundStats(
                eventLogWrapper.getForegroundStatsByTimestamps(startTime, endTime)
            )
        )
        val viewTimeSpent = appContext.formattedTimeSpent(timeSpent)
        screenTimeValue.postValue(viewTimeSpent)
        prefs.screenTimeLastUpdated = endTime
    }

    fun getPrivateSpaceAppList() {
        viewModelScope.launch {
            val handle = getPrivateSpaceUserHandle(appContext)
            privateSpaceAvailable.value = handle != null
            if (handle != null) {
                privateSpaceLocked.value = isPrivateSpaceLocked(appContext, handle)
                privateSpaceApps.value = getPrivateSpaceApps(appContext, prefs)
            } else {
                privateSpaceLocked.value = true
                privateSpaceApps.value = emptyList()
            }
        }
    }

    fun openPrivateSpaceSettings() {
        try {
            val intent = Intent("android.settings.PRIVATE_SPACE_SETTINGS")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                appContext.startActivity(intent)
            } catch (_: Exception) {
                appContext.showToast(appContext.getString(R.string.unable_to_open_app))
            }
        }
    }

    fun togglePrivateSpaceLock() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        val handle = getPrivateSpaceUserHandle(appContext) ?: return
        try {
            isPrivateSpaceToggling = true
            val userManager = appContext.getSystemService(Context.USER_SERVICE) as UserManager
            val currentlyLocked = userManager.isQuietModeEnabled(handle)
            userManager.requestQuietModeEnabled(!currentlyLocked, handle)
        } catch (e: Exception) {
            isPrivateSpaceToggling = false
            e.printStackTrace()
        }
    }

    fun setDefaultClockApp() {
        viewModelScope.launch {
            try {
                Constants.CLOCK_APP_PACKAGES.firstOrNull { appContext.isPackageInstalled(it) }?.let { packageName ->
                    appContext.packageManager.getLaunchIntentForPackage(packageName)?.component?.className?.let {
                        prefs.clockAppPackage = packageName
                        prefs.clockAppClassName = it
                        prefs.clockAppUser = android.os.Process.myUserHandle().toString()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
