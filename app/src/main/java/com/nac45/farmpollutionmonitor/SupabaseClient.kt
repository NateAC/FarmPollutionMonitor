package com.nac45.farmpollutionmonitor

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

/**
 * This creates a single Supabase client that the whole app shares.
 * Its defined at the top level for global use!
 */
val supabase = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL, // Both stored in local.properties
    supabaseKey = BuildConfig.SUPABASE_ANON_KEY
) {
    // If the JSON parser doesn't recognise any DB fields it will just ignore them.
    // So if my Supabase table has extra columns (for when I add Edore's data that aren't in my data class, it skips them instead of crashing! ^_^
    defaultSerializer = KotlinXSerializer(Json {
        ignoreUnknownKeys = true

    })
    // Installs Postgrest to let me query my database with kotlin instead of SQL.
    install(Postgrest)
}