package com.rift.core.icons

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.graphics.drawable.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class IconPackInfo(
    val packageName: String,
    val name: String,
    val icon: Bitmap? = null,
)

/**
 * Robust manager for discovering, parsing, and loading icons from standard Android Icon Packs.
 * Supports standard intents (ADW, Nova, Lawnchair, Go Launcher, Apex) and both XML and asset appfilter formats.
 */
@Singleton
class IconPackManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    companion object {
        private val ICON_PACK_ACTIONS = listOf(
            "org.adw.launcher.THEMES",
            "com.novalauncher.THEME",
            "com.gau.go.launcherex.theme",
            "com.dlto.atom.launcher.THEME",
            "app.lawnchair.icons.action.ICON_PACK",
            "com.fede.launcher.THEME_ICONPACK",
            "com.teslacoilsw.launcher.THEME",
            "com.anddoes.launcher.THEME",
        )
    }

    // Cache of loaded appfilter maps: iconPackPackage -> (cleanComponentKey -> drawableName)
    private val appFilters = ConcurrentHashMap<String, Map<String, String>>()
    // In-memory icon cache for loaded icon pack icons: "iconPack/component" -> Bitmap
    private val iconPackIconCache = ConcurrentHashMap<String, Bitmap>()

    /**
     * Scans and returns all installed Icon Packs on the device.
     */
    suspend fun getInstalledIconPacks(): List<IconPackInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packages = mutableSetOf<String>()
        val result = mutableListOf<IconPackInfo>()

        for (action in ICON_PACK_ACTIONS) {
            val intent = Intent(action)
            val activities = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
            for (resolveInfo in activities) {
                val pkg = resolveInfo.activityInfo?.packageName ?: continue
                if (packages.add(pkg)) {
                    val name = resolveInfo.loadLabel(pm).toString()
                    val icon = runCatching {
                        resolveInfo.loadIcon(pm).toBitmap(width = 96, height = 96, config = Bitmap.Config.ARGB_8888)
                    }.getOrNull()
                    result.add(IconPackInfo(packageName = pkg, name = name, icon = icon))
                }
            }
        }
        result.sortedBy { it.name }
    }

    /**
     * Parses appfilter.xml from the icon pack APK.
     */
    suspend fun loadAppFilter(iconPackPackage: String): Map<String, String> = withContext(Dispatchers.IO) {
        appFilters[iconPackPackage]?.let { return@withContext it }

        val filterMap = mutableMapOf<String, String>()
        runCatching {
            val iconPackContext = context.createPackageContext(iconPackPackage, Context.CONTEXT_IGNORE_SECURITY)
            val am = iconPackContext.assets
            var inputStream: InputStream? = runCatching { am.open("appfilter.xml") }.getOrNull()

            if (inputStream == null) {
                val resId = iconPackContext.resources.getIdentifier("appfilter", "xml", iconPackPackage)
                if (resId != 0) {
                    val parser = iconPackContext.resources.getXml(resId)
                    parseXml(parser, filterMap)
                    appFilters[iconPackPackage] = filterMap
                    return@withContext filterMap
                }
            }

            if (inputStream != null) {
                inputStream.use { stream ->
                    val factory = XmlPullParserFactory.newInstance()
                    val parser = factory.newPullParser()
                    parser.setInput(stream, "UTF-8")
                    parseXml(parser, filterMap)
                }
            }
        }

        appFilters[iconPackPackage] = filterMap
        filterMap
    }

    private fun parseXml(parser: XmlPullParser, outMap: MutableMap<String, String>) {
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                val component = parser.getAttributeValue(null, "component")
                val drawable = parser.getAttributeValue(null, "drawable")
                if (!component.isNullOrBlank() && !drawable.isNullOrBlank()) {
                    val cleanKey = component
                        .removePrefix("ComponentInfo{")
                        .removeSuffix("}")
                        .trim()
                    outMap[cleanKey] = drawable
                    val slashIdx = cleanKey.indexOf('/')
                    if (slashIdx > 0) {
                        val pkgOnly = cleanKey.substring(0, slashIdx)
                        if (!outMap.containsKey(pkgOnly)) {
                            outMap[pkgOnly] = drawable
                        }
                    }
                }
            }
            eventType = parser.next()
        }
    }

    /**
     * Loads the themed icon for an app from the specified icon pack.
     */
    suspend fun loadIcon(
        iconPackPackage: String,
        componentName: ComponentName,
        targetSizePx: Int = 256,
    ): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "$iconPackPackage/${componentName.packageName}/${componentName.className}"
        iconPackIconCache[cacheKey]?.let { return@withContext it }

        val filter = loadAppFilter(iconPackPackage)
        val fullKey = "${componentName.packageName}/${componentName.className}"
        val drawableName = filter[fullKey] ?: filter[componentName.packageName] ?: return@withContext null

        runCatching {
            val iconPackContext = context.createPackageContext(iconPackPackage, Context.CONTEXT_IGNORE_SECURITY)
            val resId = iconPackContext.resources.getIdentifier(drawableName, "drawable", iconPackPackage)
            if (resId != 0) {
                val drawable = iconPackContext.resources.getDrawable(resId, iconPackContext.theme)
                val bmp = Bitmap.createBitmap(targetSizePx, targetSizePx, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, targetSizePx, targetSizePx)
                drawable.draw(canvas)
                iconPackIconCache[cacheKey] = bmp
                bmp
            } else {
                null
            }
        }.getOrNull()
    }

    fun clearCache() {
        appFilters.clear()
        iconPackIconCache.clear()
    }
}
