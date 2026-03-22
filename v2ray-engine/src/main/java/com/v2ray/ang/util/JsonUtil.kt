package com.v2ray.ang.util

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.v2ray.ang.AppConfig
import java.lang.reflect.Type

object JsonUtil {
    private val gson = Gson()

    fun toJson(src: Any?): String = gson.toJson(src)

    fun <T> fromJson(src: String, cls: Class<T>): T? = gson.fromJson(src, cls)

    /**
     * Pretty JSON for configs; [Double] serialized via [Double.toInt] so core does not see fractional numbers where ints are required.
     * Nullable values: `x?.let { toJsonPretty(it) }`.
     */
    fun toJsonPretty(src: Any): String {
        val gsonPre = GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(
                object : TypeToken<Double>() {}.type,
                JsonSerializer { value: Double?, _: Type?, _: JsonSerializationContext? ->
                    JsonPrimitive(value?.toInt())
                },
            )
            .create()
        return gsonPre.toJson(src)
    }

    /** Nullable JSON text: `s?.let { parseString(it) }`. */
    fun parseString(src: String): JsonObject? =
        try {
            JsonParser.parseString(src).asJsonObject
        } catch (e: JsonSyntaxException) {
            Log.e(AppConfig.TAG, "Failed to parse JSON string", e)
            null
        } catch (e: IllegalStateException) {
            Log.e(AppConfig.TAG, "JSON root is not an object", e)
            null
        }
}
