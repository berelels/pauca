package app.pauca.helper

import app.pauca.data.AppModel

interface AppFilterHelper {
    fun onAppFiltered(items:List<AppModel>)
}