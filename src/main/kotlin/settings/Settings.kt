package settings

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.*

object Settings {

    private lateinit var date: MutableMap<String, String>

    val passwordPostgresql: String
        get() {
            if (!::date.isInitialized) readSettings()
            return date["passwordPostgresql"] ?: ""
        }

    private fun mapToProperties(mapProp: MutableMap<String, String>, path: String) {
        val fos = FileOutputStream(File(path))
        val prop = Properties()

        for (entity in mapProp) {
            prop.setProperty(entity.key, entity.value)
        }

        prop.store(fos, null)
        fos.close()
    }

    private fun mapFromProperties(path: String): MutableMap<String, String> {
        val result: MutableMap<String, String> = mutableMapOf()
        val fis = try {
            FileInputStream(path)
        } catch (e: FileNotFoundException) {
            return result
        }
        val prop = Properties()
        prop.load(fis)
        fis.close()

        val enumKeys = prop.keys()
        while (enumKeys.hasMoreElements()) {
            val key = enumKeys.nextElement() as String
            val value = prop.getProperty(key)
            result[key] = value
        }
        return result
    }

    private fun readSettings() {

        date = mutableMapOf("passwordGit" to "", "passwordPostgresql" to "")

        val pathProperties = "src\\main\\resources\\local.properties"
        if (!File(pathProperties).exists()) mapToProperties(date, pathProperties)

        date = mapFromProperties(pathProperties)

    }
}