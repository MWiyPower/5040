package com.example

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class SavedCredential(
  val id: String = java.util.UUID.randomUUID().toString(),
  val title: String, // e.g. "پنل ۵۰۴۰"
  val username: String,
  val password: String,
  val siteType: String // "panel", "chat", or "other"
)

class CredentialStore(context: Context) {
  private val prefs: SharedPreferences = context.getSharedPreferences("saved_credentials_prefs", Context.MODE_PRIVATE)
  private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
  private val listType = Types.newParameterizedType(List::class.java, SavedCredential::class.java)
  private val adapter = moshi.adapter<List<SavedCredential>>(listType)

  fun getCredentials(): List<SavedCredential> {
    val json = prefs.getString("credentials_list", null) ?: return emptyList()
    return try {
      adapter.fromJson(json) ?: emptyList()
    } catch (e: Exception) {
      emptyList()
    }
  }

  fun saveCredentials(list: List<SavedCredential>) {
    val json = adapter.toJson(list)
    prefs.edit().putString("credentials_list", json).apply()
  }

  fun addCredential(credential: SavedCredential) {
    val current = getCredentials().toMutableList()
    current.add(credential)
    saveCredentials(current)
  }

  fun deleteCredential(id: String) {
    val current = getCredentials().filter { it.id != id }
    saveCredentials(current)
  }
}
