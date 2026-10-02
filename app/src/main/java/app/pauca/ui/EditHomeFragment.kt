package app.pauca.ui

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.pauca.MainViewModel
import app.pauca.R
import app.pauca.data.Constants
import app.pauca.data.HomeGroup
import app.pauca.data.HomeItem
import app.pauca.data.HomeStore
import app.pauca.data.Palette
import app.pauca.data.Prefs
import app.pauca.data.Profile
import app.pauca.databinding.FragmentEditHomeBinding
import app.pauca.helper.createDialog
import app.pauca.helper.dpToPx
import app.pauca.helper.showInputDialog
import app.pauca.helper.showPopupMenu
import java.util.Collections

/**
 * Edita os cartões de um perfil, como a tela "Personal Apps" do Dumb Phone:
 * grupos numerados, cada app com "⋯" (renomear, trocar, remover) e alça para arrastar
 * — inclusive de um grupo para outro.
 */
class EditHomeFragment : BaseFragment() {

    private var _binding: FragmentEditHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var prefs: Prefs
    private lateinit var palette: Palette
    private lateinit var profile: Profile
    private lateinit var adapter: RowsAdapter
    private lateinit var touchHelper: ItemTouchHelper

    private sealed class Row {
        data class Header(val group: HomeGroup, val number: Int) : Row()
        data class Item(val item: HomeItem) : Row()
        data object Footer : Row()
    }

    private val rows = mutableListOf<Row>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        palette = prefs.palette.forScreens
        binding.root.applyScreenStyle(palette)

        adapter = RowsAdapter()
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter
        touchHelper = ItemTouchHelper(DragCallback())
        touchHelper.attachToRecyclerView(binding.list)

        binding.count.setTextColor(palette.muted)
        binding.addApps.setTextColor(palette.accentInk)
        binding.addApps.background = Look.pressable(palette.accent, 27, palette.accentInk)
        Look.applyFont(binding.addApps, Constants.Font.JAKARTA, 700)
        binding.addApps.text = "+  " + getString(R.string.add_apps)
        binding.addApps.setOnClickListener { openPicker(profile.groups.lastOrNull()?.id) }
    }

    override fun onResume() {
        super.onResume()
        // O seletor de apps salva direto no HomeStore; recarrega ao voltar dele
        val data = HomeStore.load(requireContext())
        profile = data.profile(arguments?.getString(Constants.Key.PROFILE_ID))
        if (profile.groups.isEmpty()) profile.groups.add(HomeGroup())
        binding.topBar.style(
            palette,
            title = getString(R.string.edit_title, profile.name),
            onBack = { close() },
            onDone = { close() },
        )
        rebuildRows()
    }

    private fun close() {
        viewModel.refreshHome(false)
        findNavController().popBackStack()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun rebuildRows() {
        rows.clear()
        profile.groups.forEachIndexed { i, group ->
            rows.add(Row.Header(group, i + 1))
            group.items.forEach { rows.add(Row.Item(it)) }
        }
        rows.add(Row.Footer)
        adapter.notifyDataSetChanged()
        val total = profile.allItems.size
        binding.count.text = resources.getQuantityString(R.plurals.apps_on_home, total, total)
    }

    /** Depois de arrastar: cada app pertence ao último cabeçalho acima dele. */
    private fun commitOrder() {
        val byGroup = linkedMapOf<String, MutableList<HomeItem>>()
        var current: MutableList<HomeItem>? = null
        rows.forEach { row ->
            when (row) {
                is Row.Header -> current = mutableListOf<HomeItem>().also { byGroup[row.group.id] = it }
                is Row.Item -> current?.add(row.item)
                Row.Footer -> {}
            }
        }
        profile.groups.forEach { group ->
            group.items.clear()
            group.items.addAll(byGroup[group.id].orEmpty())
        }
        save()
    }

    private fun save() {
        HomeStore.save(requireContext(), HomeStore.load(requireContext()))
        rebuildRows()
    }

    // Ações

    private fun openPicker(groupId: String?, mode: Int = Constants.PickerMode.HOME, itemId: String? = null) {
        viewModel.getAppList(includeHiddenApps = true)
        findNavController().navigate(
            R.id.action_editHomeFragment_to_appPickerFragment,
            bundleOf(
                Constants.Key.PROFILE_ID to profile.id,
                Constants.Key.GROUP_ID to groupId,
                Constants.Key.MODE to mode,
                Constants.Key.ITEM_ID to itemId,
            )
        )
    }

    private fun showItemMenu(anchor: View, item: HomeItem) {
        anchor.showPopupMenu(configure = { menu ->
            menu.add(0, 1, 0, R.string.rename)
            menu.add(0, 2, 1, R.string.replace_app)
            menu.add(0, 3, 2, R.string.remove)
        }) {
            when (it.itemId) {
                1 -> requireContext().showInputDialog(getString(R.string.rename), item.label) { name ->
                    if (name.isNotEmpty()) {
                        item.label = name
                        save()
                    }
                }

                2 -> openPicker(null, Constants.PickerMode.REPLACE, item.id)
                3 -> {
                    profile.groups.forEach { g -> g.items.remove(item) }
                    save()
                }
            }
        }
    }

    private fun showGroupMenu(anchor: View, group: HomeGroup) {
        val index = profile.groups.indexOf(group)
        anchor.showPopupMenu(configure = { menu ->
            menu.add(0, 1, 0, R.string.add_apps)
            if (index > 0) menu.add(0, 2, 1, R.string.move_up)
            if (index < profile.groups.lastIndex) menu.add(0, 3, 2, R.string.move_down)
            if (profile.groups.size > 1) menu.add(0, 4, 3, R.string.delete_group)
        }) {
            when (it.itemId) {
                1 -> openPicker(group.id)
                2 -> {
                    Collections.swap(profile.groups, index, index - 1)
                    save()
                }

                3 -> {
                    Collections.swap(profile.groups, index, index + 1)
                    save()
                }

                4 -> confirmDeleteGroup(group)
            }
        }
    }

    private fun confirmDeleteGroup(group: HomeGroup) {
        if (group.items.isEmpty()) {
            profile.groups.remove(group)
            save()
            return
        }
        requireContext().createDialog(
            title = getString(R.string.delete_group),
            action = getString(R.string.delete),
            message = resources.getQuantityString(R.plurals.delete_group_message, group.items.size, group.items.size),
            onAction = {
                profile.groups.remove(group)
                save()
            },
        ).showRespectingStatusBar()
    }

    private fun addGroup() {
        profile.groups.add(HomeGroup())
        save()
        binding.list.smoothScrollToPosition(rows.lastIndex)
    }

    // Lista

    private inner class RowsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemCount() = rows.size

        override fun getItemViewType(position: Int) = when (rows[position]) {
            is Row.Header -> 0
            is Row.Item -> 1
            Row.Footer -> 2
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = when (viewType) {
                0 -> headerView()
                1 -> itemView()
                else -> footerView()
            }
            return object : RecyclerView.ViewHolder(view) {}
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val view = holder.itemView
            when (val row = rows[position]) {
                is Row.Header -> {
                    view.findViewById<TextView>(R.id.label).text = getString(R.string.group_number, row.number)
                    view.findViewById<View>(R.id.add).setOnClickListener { openPicker(row.group.id) }
                    view.findViewById<View>(R.id.more).setOnClickListener { showGroupMenu(it, row.group) }
                }

                is Row.Item -> {
                    val label = view.findViewById<TextView>(R.id.label)
                    label.text = row.item.label
                    label.setOnClickListener { showItemMenu(it, row.item) }
                    view.findViewById<View>(R.id.more).setOnClickListener { showItemMenu(it, row.item) }
                    view.findViewById<View>(R.id.drag).setOnTouchListener { _, event ->
                        if (event.actionMasked == MotionEvent.ACTION_DOWN) touchHelper.startDrag(holder)
                        false
                    }
                }

                Row.Footer -> view.setOnClickListener { addGroup() }
            }
        }

        private fun headerView(): View = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8.dpToPx(), 18.dpToPx(), 0, 4.dpToPx())
            layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(TextView(context).apply {
                id = R.id.label
                textSize = 13f
                letterSpacing = 0.08f
                isAllCaps = true
                setTextColor(palette.muted)
                Look.applyFont(this, Constants.Font.JAKARTA, 700)
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(iconButton(R.drawable.ic_add, R.id.add, palette.muted, R.string.add_apps))
            addView(iconButton(R.drawable.ic_more, R.id.more, palette.muted, R.string.more))
        }

        private fun itemView(): View = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 4.dpToPx(), 0, 4.dpToPx())
            layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(TextView(context).apply {
                id = R.id.label
                textSize = 17f
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setTextColor(palette.text)
                Look.applyFont(this, Constants.Font.JAKARTA, 500)
                setPadding(18.dpToPx(), 14.dpToPx(), 18.dpToPx(), 14.dpToPx())
                background = Look.pressable(palette.card, 16, palette.text)
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 6.dpToPx()
            })
            addView(iconButton(R.drawable.ic_more, R.id.more, palette.accent, R.string.more))
            addView(iconButton(R.drawable.ic_drag, R.id.drag, palette.muted, R.string.drag))
        }

        private fun footerView(): View = TextView(requireContext()).apply {
            text = "+  " + getString(R.string.new_group)
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(palette.accent)
            Look.applyFont(this, Constants.Font.JAKARTA, 600)
            setPadding(0, 16.dpToPx(), 0, 16.dpToPx())
            background = Look.rounded(0, 16, ColorUtils.setAlphaComponent(palette.accent, 0x66))
            layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 20.dpToPx()
            }
        }

        private fun iconButton(icon: Int, viewId: Int, tint: Int, description: Int) = ImageView(requireContext()).apply {
            id = viewId
            setImageResource(icon)
            imageTintList = ColorStateList.valueOf(tint)
            contentDescription = getString(description)
            setPadding(9.dpToPx(), 9.dpToPx(), 9.dpToPx(), 9.dpToPx())
            background = Look.pressable(0, 20, palette.text)
            layoutParams = LinearLayout.LayoutParams(42.dpToPx(), 42.dpToPx())
        }
    }

    private inner class DragCallback : ItemTouchHelper.Callback() {
        override fun isLongPressDragEnabled() = false
        override fun isItemViewSwipeEnabled() = false

        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int =
            if (rows.getOrNull(viewHolder.bindingAdapterPosition) is Row.Item)
                makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
            else 0

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            val from = viewHolder.bindingAdapterPosition
            val to = target.bindingAdapterPosition
            // Não passa acima do primeiro cabeçalho nem abaixo de "novo grupo"
            if (to <= 0 || to >= rows.lastIndex) return false
            Collections.swap(rows, from, to)
            adapter.notifyItemMoved(from, to)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) viewHolder?.itemView?.alpha = 0.85f
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            viewHolder.itemView.alpha = 1f
            recyclerView.post { commitOrder() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
