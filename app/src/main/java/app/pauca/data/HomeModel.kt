package app.pauca.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Um app (ou atalho fixado) na tela inicial, com o texto escolhido pelo usuário. */
data class HomeItem(
    val id: String = newId(),
    var label: String,
    val pkg: String,
    val activity: String?,
    val user: String,
    val shortcutId: String? = null,
) {
    val isShortcut: Boolean get() = !shortcutId.isNullOrEmpty()

    /** Mesma chave usada pelo seletor de apps para saber o que já está na tela. */
    val key: String
        get() = if (isShortcut) shortcutIdentity(pkg, shortcutId!!, user) else "$pkg|$user"

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("label", label)
        .put("pkg", pkg)
        .put("activity", activity ?: "")
        .put("user", user)
        .put("shortcutId", shortcutId ?: "")

    companion object {
        fun fromJson(o: JSONObject) = HomeItem(
            id = o.optString("id").ifEmpty { newId() },
            label = o.optString("label"),
            pkg = o.optString("pkg"),
            activity = o.optString("activity").ifEmpty { null },
            user = o.optString("user"),
            shortcutId = o.optString("shortcutId").ifEmpty { null },
        )
    }
}

/** Um cartão da tela inicial. */
data class HomeGroup(
    val id: String = newId(),
    val items: MutableList<HomeItem> = mutableListOf(),
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("items", JSONArray().apply { items.forEach { put(it.toJson()) } })

    companion object {
        fun fromJson(o: JSONObject): HomeGroup {
            val arr = o.optJSONArray("items") ?: JSONArray()
            return HomeGroup(
                id = o.optString("id").ifEmpty { newId() },
                items = MutableList(arr.length()) { HomeItem.fromJson(arr.getJSONObject(it)) },
            )
        }
    }
}

/** Um conjunto de cartões (Pessoal, Trabalho...). Pode ligar o modo foco ao ser escolhido. */
data class Profile(
    val id: String = newId(),
    var name: String,
    val groups: MutableList<HomeGroup> = mutableListOf(HomeGroup()),
    var focusOnSwitch: Boolean = false,
) {
    val allItems: List<HomeItem> get() = groups.flatMap { it.items }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("focusOnSwitch", focusOnSwitch)
        .put("groups", JSONArray().apply { groups.forEach { put(it.toJson()) } })

    companion object {
        fun fromJson(o: JSONObject): Profile {
            val arr = o.optJSONArray("groups") ?: JSONArray()
            return Profile(
                id = o.optString("id").ifEmpty { newId() },
                name = o.optString("name"),
                focusOnSwitch = o.optBoolean("focusOnSwitch"),
                groups = MutableList(arr.length()) { HomeGroup.fromJson(arr.getJSONObject(it)) },
            )
        }
    }
}

class HomeData(
    val profiles: MutableList<Profile>,
    var activeId: String,
) {
    val active: Profile
        get() = profiles.firstOrNull { it.id == activeId } ?: profiles.first().also { activeId = it.id }

    fun profile(id: String?): Profile = profiles.firstOrNull { it.id == id } ?: active

    fun toJson(): String = JSONObject()
        .put("version", 1)
        .put("activeId", activeId)
        .put("profiles", JSONArray().apply { profiles.forEach { put(it.toJson()) } })
        .toString()

    companion object {
        fun fromJson(json: String): HomeData? = try {
            val o = JSONObject(json)
            val arr = o.getJSONArray("profiles")
            val profiles = MutableList(arr.length()) { Profile.fromJson(arr.getJSONObject(it)) }
            if (profiles.isEmpty()) null
            else HomeData(profiles, o.optString("activeId", profiles.first().id))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

internal fun newId(): String = UUID.randomUUID().toString().substring(0, 8)
