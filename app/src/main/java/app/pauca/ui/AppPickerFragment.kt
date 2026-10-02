package app.pauca.ui

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.pauca.MainViewModel
import app.pauca.R
import app.pauca.data.AppModel
import app.pauca.data.Constants
import app.pauca.data.HomeGroup
import app.pauca.data.HomeItem
import app.pauca.data.HomeStore
import app.pauca.data.Palette
import app.pauca.data.Prefs
import app.pauca.data.Profile
import app.pauca.databinding.FragmentAppPickerBinding
import app.pauca.focus.FocusManager
import app.pauca.helper.dpToPx
import app.pauca.helper.hideKeyboard
import java.text.Normalizer

/**
 * Lista de todos os apps com marcação, como o "Choose Essential Apps" do Dumb Phone.
 *  - HOME: marca/desmarca os apps de um perfil (entram no grupo escolhido).
 *  - REPLACE: toque único troca o app de um item, mantendo o lugar.
 *  - FOCUS_ALLOWED: quais apps podem notificar e abrir durante o foco.
 */
class AppPickerFragment : BaseFragment() {

    private var _binding: FragmentAppPickerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var prefs: Prefs
    private lateinit var palette: Palette
    private lateinit var profile: Profile
    private var mode = Constants.PickerMode.HOME
    private var groupId: String? = null
    private var itemId: String? = null

    private var allApps: List<AppModel> = emptyList()
    private var shown: List<AppModel> = emptyList()
    private val adapter = AppsAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAppPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        palette = prefs.palette.forScreens
        mode = arguments?.getInt(Constants.Key.MODE, Constants.PickerMode.HOME) ?: Constants.PickerMode.HOME
        groupId = arguments?.getString(Constants.Key.GROUP_ID)
        itemId = arguments?.getString(Constants.Key.ITEM_ID)
        profile = HomeStore.load(requireContext()).profile(arguments?.getString(Constants.Key.PROFILE_ID))

        binding.root.applyScreenStyle(palette)
        val title = when (mode) {
            Constants.PickerMode.REPLACE -> R.string.replace_app
            Constants.PickerMode.FOCUS_ALLOWED -> R.string.focus_allowed_apps
            else -> R.string.choose_apps
        }
        binding.topBar.style(palette, getString(title), onBack = { finish() }, onDone = { finish() }.takeIf { mode != Constants.PickerMode.REPLACE })

        binding.search.setTextColor(palette.text)
        binding.search.setHintTextColor(palette.muted)
        Look.tintTextInput(binding.search, palette.accent)
        binding.search.background = Look.rounded(palette.card, 24)
        binding.search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) = applyFilter()
        })

        binding.hint.isVisible = mode == Constants.PickerMode.FOCUS_ALLOWED
        binding.hint.text = getString(R.string.focus_allowed_hint)
        binding.hint.setTextColor(palette.muted)
        binding.count.setTextColor(palette.muted)

        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter
        binding.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) binding.search.hideKeyboard()
            }
        })

        viewModel.appList.observe(viewLifecycleOwner) { list ->
            allApps = list.orEmpty().filter { it !is AppModel.PrivateSpaceHeader }
            applyFilter()
        }
    }

    private fun finish() {
        binding.search.hideKeyboard()
        if (mode == Constants.PickerMode.FOCUS_ALLOWED) FocusManager.refresh(requireContext())
        viewModel.refreshHome(false)
        findNavController().popBackStack()
    }

    private fun normalize(text: String) =
        Normalizer.normalize(text, Normalizer.Form.NFD).replace("\\p{Mn}+".toRegex(), "").lowercase()

    @SuppressLint("NotifyDataSetChanged")
    private fun applyFilter() {
        val query = normalize(binding.search.text.toString().trim())
        shown = if (query.isEmpty()) allApps else allApps.filter { normalize(it.appLabel).contains(query) }
        adapter.notifyDataSetChanged()
        updateCount()
    }

    private fun updateCount() {
        val count = when (mode) {
            Constants.PickerMode.FOCUS_ALLOWED -> prefs.focusAllowed.size
            Constants.PickerMode.REPLACE -> -1
            else -> profile.allItems.size
        }
        binding.count.isVisible = count >= 0
        if (count >= 0) binding.count.text = resources.getQuantityString(R.plurals.apps_selected, count, count)
    }

    // Estado de cada linha

    private fun key(app: AppModel): String = when (app) {
        is AppModel.PinnedShortcut -> app.identity
        else -> "${app.appPackage}|${app.user}"
    }

    private val homePackages: Set<String> get() = profile.allItems.map { it.pkg }.toSet()

    private fun isSelected(app: AppModel): Boolean = when (mode) {
        Constants.PickerMode.FOCUS_ALLOWED -> app.appPackage in prefs.focusAllowed || app.appPackage in homePackages
        Constants.PickerMode.REPLACE -> false
        else -> profile.allItems.any { it.key == key(app) }
    }

    /** No foco, os apps da tela inicial já passam: aparecem marcados e travados. */
    private fun isLocked(app: AppModel) =
        mode == Constants.PickerMode.FOCUS_ALLOWED && app.appPackage in homePackages

    private fun onAppClicked(app: AppModel, position: Int) {
        when (mode) {
            Constants.PickerMode.REPLACE -> {
                replaceItem(app)
                finish()
                return
            }

            Constants.PickerMode.FOCUS_ALLOWED -> {
                if (isLocked(app)) return
                val allowed = prefs.focusAllowed.toMutableSet()
                if (!allowed.add(app.appPackage)) allowed.remove(app.appPackage)
                prefs.focusAllowed = allowed
            }

            else -> {
                if (isSelected(app)) profile.groups.forEach { g -> g.items.removeAll { it.key == key(app) } }
                else targetGroup().items.add(toItem(app))
                HomeStore.save(requireContext(), HomeStore.load(requireContext()))
            }
        }
        adapter.notifyItemChanged(position)
        updateCount()
    }

    private fun targetGroup(): HomeGroup =
        profile.groups.firstOrNull { it.id == groupId }
            ?: profile.groups.lastOrNull()
            ?: HomeGroup().also { profile.groups.add(it) }

    private fun replaceItem(app: AppModel) {
        for (group in profile.groups) {
            val index = group.items.indexOfFirst { it.id == itemId }
            if (index >= 0) {
                group.items[index] = toItem(app).copy(id = group.items[index].id)
                break
            }
        }
        HomeStore.save(requireContext(), HomeStore.load(requireContext()))
    }

    private fun toItem(app: AppModel) = HomeItem(
        label = app.appLabel,
        pkg = app.appPackage,
        activity = (app as? AppModel.App)?.activityClassName,
        user = app.user.toString(),
        shortcutId = (app as? AppModel.PinnedShortcut)?.shortcutId,
    )

    private inner class AppsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemCount() = shown.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val row = LinearLayout(parent.context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(16.dpToPx(), 0, 12.dpToPx(), 0)
                minimumHeight = 56.dpToPx()
                background = Look.pressable(0, 16, palette.text)
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                addView(TextView(context).apply {
                    id = R.id.label
                    textSize = 17f
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    setTextColor(palette.text)
                    Look.applyFont(this, Constants.Font.JAKARTA, 500)
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                addView(ImageView(context).apply {
                    id = R.id.check
                    setPadding(3.dpToPx(), 3.dpToPx(), 3.dpToPx(), 3.dpToPx())
                }, LinearLayout.LayoutParams(28.dpToPx(), 28.dpToPx()))
            }
            return object : RecyclerView.ViewHolder(row) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val app = shown[position]
            val row = holder.itemView
            row.findViewById<TextView>(R.id.label).text = app.appLabel
            val check = row.findViewById<ImageView>(R.id.check)
            val selected = isSelected(app)
            if (mode == Constants.PickerMode.REPLACE) {
                check.isVisible = false
            } else if (selected) {
                check.isVisible = true
                check.setImageResource(R.drawable.ic_tick)
                check.imageTintList = ColorStateList.valueOf(palette.accentInk)
                check.background = Look.rounded(palette.accent, 14)
            } else {
                check.isVisible = true
                check.setImageResource(R.drawable.ic_circle)
                check.imageTintList = ColorStateList.valueOf(palette.muted)
                check.background = null
            }
            row.alpha = if (isLocked(app)) 0.55f else 1f
            row.setOnClickListener { onAppClicked(app, holder.bindingAdapterPosition) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
