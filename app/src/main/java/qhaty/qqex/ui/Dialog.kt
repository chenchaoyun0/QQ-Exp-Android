package qhaty.qqex.ui

import android.app.Activity
import androidx.lifecycle.LifecycleCoroutineScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import kotlinx.coroutines.launch
import qhaty.qqex.R
import qhaty.qqex.util.*

fun Activity.selfKeyDialog() = MaterialDialog(this).show {
    title(R.string.self_key)
    var str = mmkv["self-key", ""]
    input(prefill = str) { _, s -> str = s.toString() }
    positiveButton(R.string.ok) {
        if (str.isBlank()) toast(R.string.not_input_key)
        else mmkv["self-key"] = str
    }
    negativeButton(R.string.cancel)
}

fun Activity.expWithRebuildDialog(
    lifecycleScope: LifecycleCoroutineScope,
    callback: suspend () -> Unit
) = MaterialDialog(this).show {
    title(R.string.notice)
    message(R.string.has_local_db)
    positiveButton(R.string.ok) {
        lifecycleScope.launch {
            delDB(applicationContext)
            callback.invoke()
        }
    }
    negativeButton(R.string.cancel) { lifecycleScope.launch { callback.invoke() } }
}

fun Activity.uploadEnd(
    cause: Long,
    res: String,
    lifecycleScope: LifecycleCoroutineScope,
    callback: suspend () -> Unit
) =
    MaterialDialog(this).show {
        title(R.string.notice)
        val minutes = cause / 1000 / 60
        val seconds = cause / 1000 % 60
        message(R.string.upload_end, "上传完成啦！${res}! 总共耗时:${minutes}分${seconds}秒")
        positiveButton(R.string.ok) {
            lifecycleScope.launch {
                callback.invoke()
            }
        }
    }
