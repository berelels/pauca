package app.pauca.helper

import android.content.Context
import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.MenuRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import app.pauca.data.Prefs
import app.pauca.databinding.DialogBaseBinding
import app.pauca.ui.Look

/**
 * Shows a popup menu hanging off the end edge of this view.
 * [configure] can add or tweak items before the menu is shown.
 */
fun View.showPopupMenu(
    @MenuRes menuRes: Int = 0,
    configure: (Menu) -> Unit = {},
    onItemClick: (MenuItem) -> Unit,
): PopupMenu {
    val popup = PopupMenu(context, this, Gravity.END)
    if (menuRes != 0) popup.menuInflater.inflate(menuRes, popup.menu)
    configure(popup.menu)
    popup.setOnMenuItemClickListener { item ->
        onItemClick(item)
        true
    }
    popup.show()
    return popup
}

/**
 * App dialog: shows without bringing back a hidden status bar, and blurs the
 * screen behind it on Android 12+, fading blur and dialog out together on dismiss.
 */
class OlDialog(context: Context) : AlertDialog(context) {

    private var blur: WindowBlur? = null

    fun showRespectingStatusBar() {
        val window = window
        if (window == null || Prefs(context).showStatusBar) {
            show()
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            show()
            window.hideStatusBar()
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        }
        blur = window?.let { WindowBlur(it).apply { fadeIn() } }
    }

    override fun dismiss() {
        val blur = blur ?: return super.dismiss()
        this.blur = null
        blur.fadeOut { super.dismiss() }
    }
}

/**
 * Builds a dialog using the app's own layout: a title row with a close icon,
 * an optional [message] or custom [content], and a text [action] at the end.
 * [content] receives the container so the inflated view keeps its XML margins.
 */
fun Context.createDialog(
    @StringRes title: Int,
    @StringRes action: Int,
    @StringRes message: Int = 0,
    @StringRes neutral: Int = 0,
    onNeutral: () -> Unit = {},
    onAction: () -> Unit = {},
    content: ((ViewGroup) -> View)? = null,
): OlDialog = createDialog(
    title = getString(title),
    action = getString(action),
    message = if (message != 0) getString(message) else null,
    neutral = if (neutral != 0) getString(neutral) else null,
    onNeutral = onNeutral,
    onAction = onAction,
    content = content,
)

fun Context.createDialog(
    title: CharSequence,
    action: CharSequence?,
    message: CharSequence? = null,
    neutral: CharSequence? = null,
    onNeutral: () -> Unit = {},
    onAction: () -> Unit = {},
    content: ((ViewGroup) -> View)? = null,
): OlDialog {
    val dialog = OlDialog(this)
    val binding = DialogBaseBinding.inflate(LayoutInflater.from(dialog.context))
    binding.tvTitle.text = title
    binding.tvAction.text = action
    binding.tvAction.isVisible = action != null
    if (message != null) {
        binding.tvMessage.text = message
        binding.tvMessage.isVisible = true
    }
    if (neutral != null) {
        binding.tvNeutral.text = neutral
        binding.tvNeutral.isVisible = true
    }
    content?.let {
        binding.contentContainer.addView(it(binding.contentContainer))
        binding.contentContainer.isVisible = true
    }
    dialog.setView(binding.root)
    binding.ivClose.setOnClickListener { dialog.dismiss() }
    binding.tvNeutral.setOnClickListener {
        onNeutral()
        dialog.dismiss()
    }
    binding.tvAction.setOnClickListener {
        onAction()
        dialog.dismiss()
    }
    return dialog
}

/** Uma caixa de texto com "Salvar". Usada para renomear apps, grupos e perfis. */
fun Context.showInputDialog(
    title: CharSequence,
    initial: String,
    hint: CharSequence? = null,
    onSave: (String) -> Unit,
): OlDialog {
    val input = EditText(this).apply {
        setText(initial)
        setSelection(initial.length)
        this.hint = hint
        isSingleLine = true
        imeOptions = EditorInfo.IME_ACTION_DONE
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        textSize = 20f
        setTextColor(getColorFromAttr(app.pauca.R.attr.primaryColor))
        setHintTextColor(getColorFromAttr(app.pauca.R.attr.primaryColorTrans50))
        typeface = ResourcesCompat.getFont(this@showInputDialog, app.pauca.R.font.jakarta)
        Look.tintTextInput(this, Prefs(this@showInputDialog).palette.accent)
    }
    lateinit var dialog: OlDialog
    dialog = createDialog(
        title = title,
        action = getString(app.pauca.R.string.save),
        onAction = { onSave(input.text.toString().trim()) },
        content = { container ->
            FrameLayout(container.context).apply {
                setPadding(0, 16.dpToPx(), 8.dpToPx(), 16.dpToPx())
                addView(input, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
        },
    )
    input.setOnEditorActionListener { _, actionId, event ->
        val enter = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN
        if (actionId == EditorInfo.IME_ACTION_DONE || enter) {
            onSave(input.text.toString().trim())
            dialog.dismiss()
            true
        } else false
    }
    // Abre com o teclado: a janela precisa ser focável, ao contrário dos outros diálogos
    dialog.setOnShowListener {
        input.requestFocus()
        input.postDelayed({ input.showKeyboard() }, 150)
    }
    dialog.show()
    return dialog
}

/** Lista de opções num diálogo: cada linha é um texto clicável. */
fun Context.showListDialog(
    title: CharSequence,
    message: CharSequence? = null,
    options: List<Pair<CharSequence, () -> Unit>>,
    action: CharSequence? = null,
    onAction: () -> Unit = {},
): OlDialog {
    lateinit var dialog: OlDialog
    dialog = createDialog(
        title = title,
        action = action,
        message = message,
        onAction = onAction,
        content = { container ->
            LinearLayout(container.context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
                options.forEach { (label, onClick) ->
                    addView(TextView(context).apply {
                        text = label
                        textSize = 17f
                        typeface = ResourcesCompat.getFont(context, app.pauca.R.font.jakarta)
                        setTextColor(getColorFromAttr(app.pauca.R.attr.primaryColor))
                        setPadding(0, 12.dpToPx(), 0, 12.dpToPx())
                        setOnClickListener {
                            onClick()
                            dialog.dismiss()
                        }
                    })
                }
            }
        },
    )
    dialog.showRespectingStatusBar()
    return dialog
}

/** Title with a close icon, a message and a single action. */
fun Context.showMessageDialog(
    @StringRes title: Int,
    @StringRes message: Int,
    @StringRes action: Int,
    onAction: () -> Unit,
): OlDialog {
    val dialog = createDialog(title, action, message = message, onAction = onAction)
    dialog.showRespectingStatusBar()
    return dialog
}
