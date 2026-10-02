package app.pauca.ui

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.text.Spannable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import app.pauca.MainViewModel
import app.pauca.R
import app.pauca.data.AppCategory
import app.pauca.data.AppModel
import app.pauca.data.Constants
import app.pauca.data.Palette
import app.pauca.data.Prefs
import app.pauca.databinding.FragmentAppDrawerBinding
import app.pauca.helper.deletePinnedShortcut
import app.pauca.helper.dpToPx
import app.pauca.helper.hideKeyboard
import app.pauca.helper.isEinkDisplay
import app.pauca.helper.isSystemAnimationsDisabled
import app.pauca.helper.isSystemApp
import app.pauca.helper.openAppInfo
import app.pauca.helper.openSearch
import app.pauca.helper.openUrl
import app.pauca.helper.showKeyboard
import app.pauca.helper.showListDialog
import app.pauca.helper.showPopupMenu
import app.pauca.helper.showToast
import app.pauca.helper.uninstall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppDrawerFragment : BaseFragment() {

    private lateinit var prefs: Prefs
    private lateinit var palette: Palette
    private var categories: Map<String, String> = emptyMap()
    private lateinit var adapter: AppDrawerAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private var searchTextView: TextView? = null
    private var cachedIsCjkKeyboard: Boolean? = null

    private var flag = Constants.FLAG_LAUNCH_APP
    private var canRename = false
    private var currentAppList: List<AppModel>? = null
    private var currentPrivateSpaceApps: List<AppModel>? = null
    private var currentPrivateSpaceLocked: Boolean = true
    private var currentPrivateSpaceAvailable: Boolean = false

    private val viewModel: MainViewModel by activityViewModels()
    private var _binding: FragmentAppDrawerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAppDrawerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        palette = prefs.palette
        arguments?.let {
            flag = it.getInt(Constants.Key.FLAG, Constants.FLAG_LAUNCH_APP)
            canRename = it.getBoolean(Constants.Key.RENAME, false)
        }

        initViews()
        initSearch()
        initAdapter()
        initObservers()
        initClickListeners()
    }

    private fun initViews() {
        if (flag == Constants.FLAG_HIDDEN_APPS)
            binding.search.queryHint = getString(R.string.hidden_apps)
        else if (flag in Constants.FLAG_SET_SWIPE_LEFT_APP..Constants.FLAG_SET_SCREEN_TIME_APP)
            binding.search.queryHint = getString(R.string.select_an_app)
        else if (prefs.focusActive && prefs.focusHideApps)
            binding.search.queryHint = getString(R.string.focus_drawer_hint)
        try {
            searchTextView = binding.search.findViewById(R.id.search_src_text)
            searchTextView?.apply {
                gravity = prefs.appLabelAlignment
                textSize = 26f
                isCursorVisible = true
                setTextColor(palette.text)
                setHintTextColor(palette.muted)
                Look.applyFont(this, prefs.appFont, 400, 26f)
                Look.tintTextInput(this, palette.accent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Sobre o papel de parede, um véu leve para o texto continuar legível
        binding.drawerRoot.setBackgroundColor(if (palette.showsWallpaper) 0x33000000 else palette.bg)
        binding.drawerMenu.imageTintList = ColorStateList.valueOf(palette.text)
        binding.drawerMenu.background = Look.pressable(0, 22, palette.text)
        binding.drawerMenu.isVisible = flag == Constants.FLAG_LAUNCH_APP
        binding.drawerMenu.setOnClickListener { showDrawerMenu(it) }
        binding.searchFab.imageTintList = ColorStateList.valueOf(palette.text)
        binding.searchFab.background = Look.pressable(
            if (palette.showsWallpaper) 0x59000000 else palette.card, 28, palette.text
        )
        binding.searchFab.setOnClickListener {
            binding.search.requestFocus()
            binding.search.showKeyboard()
        }
        binding.categoryScroll.isVisible = flag != Constants.FLAG_HIDDEN_APPS

        ViewCompat.setOnApplyWindowInsetsListener(binding.drawerRoot) { _, insets ->
            val bars = insets.getInsetsIgnoringVisibility(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            binding.drawerContent.updatePadding(top = bars.top)
            binding.searchFab.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin = bars.bottom + 24.dpToPx() }
            // A lista rola por baixo da lupa e da barra de navegação até o último app ficar visível
            binding.recyclerView.updatePadding(bottom = bars.bottom + 96.dpToPx())
            // Com o teclado aberto a lupa não tem função
            binding.searchFab.isVisible = !imeVisible
            insets
        }
    }

    private fun showDrawerMenu(anchor: View) {
        anchor.showPopupMenu(configure = { menu ->
            menu.add(0, 1, 0, R.string.hidden_apps)
            menu.add(0, 2, 1, R.string.settings)
        }) {
            when (it.itemId) {
                1 -> {
                    if (prefs.hiddenApps.isEmpty()) requireContext().showToast(getString(R.string.no_hidden_apps))
                    else {
                        viewModel.getHiddenApps()
                        findNavController().navigate(R.id.appListFragment, bundleOf(Constants.Key.FLAG to Constants.FLAG_HIDDEN_APPS))
                    }
                }

                2 -> findNavController().navigate(R.id.action_appListFragment_to_settingsFragment2)
            }
        }
    }

    // Categorias

    /** Calcula a categoria de cada app fora da thread principal e monta os botões. */
    private fun refreshCategories(apps: List<AppModel>) {
        val context = requireContext().applicationContext
        val overrides = prefs.appCategories
        val packages = apps.filterIsInstance<AppModel.App>().map { it.appPackage }.filter { it.isNotEmpty() }.distinct()
        viewLifecycleOwner.lifecycleScope.launch {
            val map = withContext(Dispatchers.Default) {
                packages.associateWith { AppCategory.of(context, it, overrides) }
            }
            if (_binding == null) return@launch
            categories = map
            if (adapter.selectedCategory != null && adapter.selectedCategory !in map.values) adapter.selectedCategory = null
            renderChips()
            adapter.filter.filter(binding.search.query)
        }
    }

    private fun renderChips() {
        val container = binding.categoryChips
        container.removeAllViews()
        val present = categories.values.toSet()
        val ids = listOf<String?>(null) + AppCategory.ORDER.filter { it in present }
        if (ids.size <= 2) {
            // Uma categoria só não ajuda a filtrar nada
            binding.categoryScroll.isVisible = false
            return
        }
        binding.categoryScroll.isVisible = flag != Constants.FLAG_HIDDEN_APPS
        ids.forEach { id ->
            val selected = adapter.selectedCategory == id
            container.addView(TextView(requireContext()).apply {
                text = getString(if (id == null) R.string.category_all else AppCategory.label(id))
                textSize = 14f
                setPadding(16.dpToPx(), 9.dpToPx(), 16.dpToPx(), 9.dpToPx())
                val chipText = if (palette.showsWallpaper) 0xFF1A1713.toInt() else palette.bg
                setTextColor(if (selected) chipText else palette.text)
                background = Look.pressable(
                    if (selected) palette.text else if (palette.showsWallpaper) 0x59000000 else palette.card,
                    20, palette.text,
                )
                Look.applyFont(this, Constants.Font.JAKARTA, if (selected) 600 else 500)
                setOnClickListener {
                    adapter.selectedCategory = id
                    renderChips()
                    adapter.filter.filter(binding.search.query)
                    binding.recyclerView.scrollToPosition(0)
                }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = 8.dpToPx()
            })
        }
    }

    /** Toque longo › Categoria: escolher à mão (ou voltar para a automática). */
    private fun chooseCategory(app: AppModel) {
        val current = prefs.appCategories[app.appPackage]
        val options = (listOf<String?>(null) + AppCategory.ORDER).map { id ->
            val label = getString(if (id == null) R.string.category_auto else AppCategory.label(id))
            val mark = if (id == current) "✓  " else "     "
            (mark + label) as CharSequence to {
                prefs.appCategories = prefs.appCategories.toMutableMap().apply {
                    if (id == null) remove(app.appPackage) else put(app.appPackage, id)
                }
                currentAppList?.let { refreshCategories(it) }
                Unit
            }
        }
        requireContext().showListDialog(title = app.appLabel, options = options)
    }

    private fun initSearch() {
        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query?.startsWith("!") == true)
                    requireContext().openUrl(Constants.URL_DUCK_SEARCH + query.replace(" ", "%20"))
                else if (adapter.itemCount == 0)
                    requireContext().openSearch(query?.trim())
                else
                    adapter.launchFirstInList()
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                try {
                    adapter.allowAutoLaunch = !isSearchComposing()
                    adapter.filter.filter(newText)
                    binding.appRename.visibility =
                        if (canRename && newText.isNotBlank()) View.VISIBLE else View.GONE
                    return true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return false
            }
        })
    }

    private fun isSearchComposing(): Boolean {
        val text = searchTextView?.text
        if (text !is Spannable) return false
        val start = BaseInputConnection.getComposingSpanStart(text)
        val end = BaseInputConnection.getComposingSpanEnd(text)
        if (start !in 0 until end) return false
        return isCjkKeyboard()
    }

    private fun isCjkKeyboard(): Boolean {
        cachedIsCjkKeyboard?.let { return it }
        val result = try {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val subtype = imm.currentInputMethodSubtype
            val language = when {
                subtype == null -> ""
                subtype.languageTag.isNotEmpty() -> subtype.languageTag // e.g. "zh-CN", "ja-JP", "en-US"
                else -> subtype.locale // deprecated fallback, e.g. "zh_CN"
            }
            language.startsWith("zh") || language.startsWith("ja") || language.startsWith("ko")
        } catch (e: Exception) {
            false
        }
        cachedIsCjkKeyboard = result
        return result
    }

    private fun initAdapter() {
        adapter = AppDrawerAdapter(
            flag,
            prefs.appLabelAlignment,
            appClickListener = { appModel ->
                viewModel.selectedApp(appModel, flag)
                if (flag == Constants.FLAG_LAUNCH_APP || flag == Constants.FLAG_HIDDEN_APPS)
                    findNavController().popBackStack(R.id.mainFragment, false)
                else
                    findNavController().popBackStack()
            },
            appInfoListener = {
                openAppInfo(
                    requireContext(),
                    it.user,
                    it.appPackage
                )
                findNavController().popBackStack(R.id.mainFragment, false)
            },
            appDeleteListener = { appModel ->
                when (appModel) {
                    is AppModel.PrivateSpaceHeader -> {}
                    is AppModel.PinnedShortcut ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                            requireContext().deletePinnedShortcut(
                                packageName = appModel.appPackage,
                                shortcutIdToDelete = appModel.shortcutId,
                                user = appModel.user,
                            )
                        }

                    is AppModel.App -> {
                        if (appModel.user != Process.myUserHandle()) {
                            openAppInfo(requireContext(), appModel.user, appModel.appPackage)
                        } else if (requireContext().isSystemApp(appModel.appPackage, appModel.user)) {
                            requireContext().showToast(getString(R.string.system_app_cannot_delete))
                            openAppInfo(requireContext(), appModel.user, appModel.appPackage)
                        } else {
                            requireContext().uninstall(appModel.appPackage)
                        }
                    }
                }
                viewModel.getAppList()
            },
            appHideListener = { appModel, position ->
                if (appModel is AppModel.PinnedShortcut) {
                    requireContext().showToast(R.string.cannot_hide_shortcut)
                    return@AppDrawerAdapter
                }
                adapter.appFilteredList.removeAt(position)
                adapter.notifyItemRemoved(position)
                adapter.appsList.remove(appModel)

                val newSet = mutableSetOf<String>()
                newSet.addAll(prefs.hiddenApps)
                if (flag == Constants.FLAG_HIDDEN_APPS)
                    newSet.remove(appModel.appPackage + "|" + appModel.user.toString())
                else
                    newSet.add(appModel.appPackage + "|" + appModel.user.toString())

                prefs.hiddenApps = newSet
                if (newSet.isEmpty())
                    findNavController().popBackStack()
                if (prefs.firstHide) {
                    binding.search.hideKeyboard()
                    prefs.firstHide = false
                    viewModel.showDialog.postValue(Constants.Dialog.HIDDEN)
                    findNavController().navigate(R.id.action_appListFragment_to_settingsFragment2)
                }
                viewModel.getAppList()
                viewModel.getHiddenApps()
            },
            appRenameListener = { appModel, renameLabel ->
                val identifier = when (appModel) {
                    is AppModel.PinnedShortcut -> appModel.identity
                    is AppModel.App -> appModel.appPackage
                    else -> return@AppDrawerAdapter
                }
                prefs.setAppRenameLabel(identifier, renameLabel)
                viewModel.getAppList()
            },
            privateSpaceToggleListener = {
                viewModel.togglePrivateSpaceLock()
            },
            privateSpaceSettingsListener = {
                viewModel.openPrivateSpaceSettings()
                findNavController().popBackStack(R.id.mainFragment, false)
            },
            appCategoryListener = { chooseCategory(it) },
            palette = palette,
            font = prefs.appFont,
        )
        adapter.categoryOf = { app -> categories[app.appPackage] }

        linearLayoutManager = object : LinearLayoutManager(requireContext()) {
            override fun scrollVerticallyBy(
                dx: Int,
                recycler: Recycler,
                state: RecyclerView.State,
            ): Int {
                val scrollRange = super.scrollVerticallyBy(dx, recycler, state)
                val overScroll = dx - scrollRange
                if (overScroll < -10 && binding.recyclerView.scrollState == RecyclerView.SCROLL_STATE_DRAGGING)
                    checkMessageAndExit()
                return scrollRange
            }
        }

        binding.recyclerView.layoutManager = linearLayoutManager
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addOnScrollListener(getRecyclerViewOnScrollListener())
        binding.recyclerView.itemAnimator = null
        if (requireContext().isEinkDisplay())
            binding.recyclerView.overScrollMode = View.OVER_SCROLL_NEVER
        else if (requireContext().isSystemAnimationsDisabled().not())
            binding.recyclerView.layoutAnimation =
                AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_anim_from_bottom)
    }

    private fun initObservers() {
        viewModel.firstOpen.observe(viewLifecycleOwner) {
        }
        if (flag == Constants.FLAG_HIDDEN_APPS) {
            viewModel.hiddenApps.observe(viewLifecycleOwner) {
                it?.let {
                    adapter.setAppList(it.toMutableList())
                }
            }
        } else {
            viewModel.appList.observe(viewLifecycleOwner) {
                currentAppList = it
                updateCombinedAppList()
                it?.let { apps -> refreshCategories(apps) }
            }
            if (flag == Constants.FLAG_LAUNCH_APP) {
                viewModel.privateSpaceAvailable.observe(viewLifecycleOwner) {
                    currentPrivateSpaceAvailable = it
                    updateCombinedAppList()
                }
                viewModel.privateSpaceLocked.observe(viewLifecycleOwner) {
                    currentPrivateSpaceLocked = it
                    updateCombinedAppList()
                }
                viewModel.privateSpaceApps.observe(viewLifecycleOwner) {
                    currentPrivateSpaceApps = it
                    updateCombinedAppList()
                }
            }
        }
    }

    private fun updateCombinedAppList() {
        val apps = currentAppList ?: return
        val combined = apps.toMutableList()

        if (flag == Constants.FLAG_LAUNCH_APP && currentPrivateSpaceAvailable) {
            combined.add(AppModel.PrivateSpaceHeader(isLocked = currentPrivateSpaceLocked))
            if (!currentPrivateSpaceLocked) {
                currentPrivateSpaceApps?.let { combined.addAll(it) }
            }
        }

        adapter.setAppList(combined)
        adapter.filter.filter(binding.search.query)
    }

    private fun initClickListeners() {
        binding.appRename.setOnClickListener {
            val name = binding.search.query.toString().trim()
            if (name.isEmpty()) {
                requireContext().showToast(getString(R.string.type_a_new_app_name_first))
                binding.search.showKeyboard()
                return@setOnClickListener
            }

            findNavController().popBackStack()
        }
    }

    private fun getRecyclerViewOnScrollListener(): RecyclerView.OnScrollListener {
        return object : RecyclerView.OnScrollListener() {

            var onTop = false

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {

                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        onTop = !recyclerView.canScrollVertically(-1)
                        if (onTop)
                            binding.search.hideKeyboard()
                    }

                    RecyclerView.SCROLL_STATE_IDLE -> {
                        if (!recyclerView.canScrollVertically(1))
                            binding.search.hideKeyboard()
                        else if (!recyclerView.canScrollVertically(-1))
                            if (!onTop && isRemoving.not())
                                binding.search.showKeyboard(prefs.autoShowKeyboard)
                    }
                }
            }
        }
    }

    private fun checkMessageAndExit() {
        findNavController().popBackStack()
    }

    override fun onStart() {
        super.onStart()
        cachedIsCjkKeyboard = null
        binding.search.showKeyboard(prefs.autoShowKeyboard)
    }

    override fun onStop() {
        binding.search.hideKeyboard()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchTextView = null
        _binding = null
    }
}
