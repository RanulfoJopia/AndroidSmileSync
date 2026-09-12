package com.example.smilesync.utils

import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.JsonObject
import retrofit2.Response

object UiUtils {

    fun toast(ctx: Context, msg: String) =
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

    fun toastLong(ctx: Context, msg: String) =
        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()

    fun showLoading(pb: ProgressBar) { pb.visibility = View.VISIBLE }
    fun hideLoading(pb: ProgressBar) { pb.visibility = View.GONE }

    fun confirm(ctx: Context, title: String, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(ctx)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Confirm") { _, _ -> onConfirm() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /** Extract the Laravel error message from a failed Response body. */
    fun <T> errorMessage(response: Response<T>): String {
        return try {
            val body = response.errorBody()?.string() ?: return "Unexpected error"
            val json = Gson().fromJson(body, JsonObject::class.java)
            json.getAsJsonObject("error")?.get("message")?.asString
                ?: json.get("message")?.asString
                ?: "Error ${response.code()}"
        } catch (e: Exception) {
            "Error ${response.code()}"
        }
    }
}
