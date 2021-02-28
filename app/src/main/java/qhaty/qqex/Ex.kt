package qhaty.qqex

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.view.View
import kotlinx.coroutines.*
import qhaty.qqex.method.GetDB
import qhaty.qqex.util.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.experimental.xor
import kotlin.properties.Delegates

class Ex {
    private var progress: Progress by Delegates.observable(
        Progress(0, "开始导出...")
    ) { _, _, new -> onProgressChange(new) }

    fun startEx(context: Context, keyGenText: String? = null) {
        GlobalScope.launch {
            progress.change(10, "读取数据库...")
            val dbFileTask = async { GetDB(context).getDataBase() }
            if (dbFileTask.await() == null) {
                progress.change(1000, "无法读取数据库\n请手动导入")
                return@launch
            }

            val dbFileList = dbFileTask.await()!!
            progress.change(50, "导出数据库...")
            val allChat: ArrayList<CodedChat>
            val new = async(Dispatchers.IO) { addDByPath(dbFileList[0], keyGenText) }
            val old = if (dbFileList.size > 1) async(Dispatchers.IO) { addDByPath(dbFileList[1]) } else null
            val newR = new.await()
            val oldR = old?.await()
            when {
                newR == null -> {
                    if (oldR == null) {
                        progress = Progress(1000, "QQ无本地聊天记录")
                        return@launch
                    }
                    allChat = oldR
                }
                oldR == null -> allChat = newR
                else -> allChat = (newR + oldR) as ArrayList<CodedChat>
            }

            progress = Progress(200, "解析数据库...")
            val decodedChat = chatsDecode(allChat)
            progress.change("保存网页到本地中...")
            toHtml(decodedChat, context)
            progress = Progress(1000, "保存成功")
            withContext(Dispatchers.Main) {
                saveHtmlFile?.let { sendToViewHtml(context, it) }
                toast("文件保存至:Android/data/qhaty.qqex/files/Save")
            }
        }
    }

    @SuppressLint("Recycle")
    private fun addDByPath(libFile: File, keyGenText: String? = null): ArrayList<CodedChat>? {
        val chats = arrayListOf<CodedChat>()
        val sql = SQLiteDatabase.openDatabase(libFile.absolutePath, null, 0)
        val friendOrTroop = if (Data.friendOrGroup) "friend" else "troop"
        val sqlDo = "SELECT _id,msgData,msgtype,senderuin,time FROM mr_${friendOrTroop}_" +
                "${encodeMD5(Data.friendQQ).toUpperCase(Locale.ROOT)}_New"
        try {
            val cursor = try {
                sql.rawQuery(sqlDo, null)
            } catch (e: java.lang.Exception) {
                null
            } ?: return null
            if (cursor.count > 1) cursor.moveToFirst()
            if (keyGenText != null) {
                dbLastData = cursor.getBlob(1)
                decodeKey(keyGenText)
            }
            do {
                val data = cursor.getBlob(1)
                val type = cursor.getInt(2)
                val sender = cursor.getString(3)
                val time = cursor.getInt(4)
                chats += CodedChat(time, type, sender, data)
            } while (cursor.moveToNext())
            cursor.close()
        } catch (e: java.lang.Exception) {
        }
        sql.close()
        return chats
    }

    private suspend fun chatsDecode(allChat: List<CodedChat>): ArrayList<Chat> {
        return withContext(Dispatchers.Default) {
            val allChatDecode = arrayListOf<Chat>()
            val allCount = allChat.size
            val qqSavedSet: Set<String> = QQNickNameParse.dataSet
            val qqMap = hashMapOf<String, String>()
            val regex1 = Regex(""".*?--QQEX--""")
            val regex2 = Regex("""--QQEX--(.*?)-""")
            qqSavedSet.forEach {
                val r0 = it.replace("--QQS--", "").replace("-QQE--", "")
                val qq = regex1.find(r0)?.value?.replace("--QQEX--", "")
                val name = regex2.find(r0)?.value?.replace("--QQEX--", "")?.replace("-", "")
                if (!qq.isNullOrBlank() && !name.isNullOrBlank()) qqMap[qq] = name
            }
            progress.change("数据库解码...")
            for (i in allChat.indices) {
                val time = allChat[i].time
                val type = allChat[i].type
                var fixedQQ: String = fix(allChat[i].sender)
                for ((k, v) in qqMap) fixedQQ = fixedQQ.replace(k, v)
                val sender = fixedQQ
                val data = fix(allChat[i].msg)
                allChatDecode += Chat(time, type, sender, data)
                if (i % 20 == 0) progress.change(((i.toFloat() / allCount) * 100 + 200).toInt())
            }
            allChatDecode.sortBy { it.time }
            return@withContext allChatDecode
        }
    }

    private suspend fun toHtml(allChatDecode: ArrayList<Chat>, context: Context) {
        withContext(Dispatchers.Default) {
            progress.change("导出网页...")
            val head = "<head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" /></head>"
            var appendHtml = ""
            var appendStr = ""
            for (i in allChatDecode.indices) {
                if (i == 0) appendHtml += head
                val item = allChatDecode[i]
                try {
                    val htmlByTypeStr = htmlStrByType(item.type)
                    val msg = if (htmlByTypeStr != " ") htmlByTypeStr else {
                        appendStr += item.msg
                        item.msg
                    }
                    appendHtml = "$appendHtml<font color=\"blue\">${getDateString(item.time)}" +
                            "</font>-----<font color=\"green\">${item.sender}</font>" +
                            "</br>$msg</br></br>"
                } catch (e: Exception) {
                    continue
                }
                if (i % 20 == 0) { //每20条保存一次
                    appendTextToAppDownload(context, Data.friendQQ, appendHtml)
                    appendHtml = ""
                    appendTextToAppData(context, Data.friendQQ, appendStr)
                    appendStr = ""
                    progress.change(((i.toFloat() / allChatDecode.size) * 650 + 300).toInt())
                } else if (i == allChatDecode.size - 1) {
                    appendTextToAppDownload(context, Data.friendQQ, appendHtml)
                    appendTextToAppData(context, Data.friendQQ, appendStr)
                }
            }
        }
    }

    companion object {
        fun onProgressChange(new: Progress) {
            runOnUI {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        ProgressView.progressView?.setProgress(new.progress, true)
                    } else {
                        ProgressView.progressView?.progress = new.progress
                    }
                    ProgressView.progressText?.text = "${new.msg} ${new.progress.toFloat() / 10}%"
                    if (new.progress > 990) {
                        ProgressView.dialog?.apply {
                            setCanceledOnTouchOutside(true)
                            setCancelable(true)
                        }
                        ProgressView.cirProgress?.visibility = View.GONE
                    }
                } catch (e: java.lang.Exception) {
                }
            }
        }
    }
}

@Throws(IOException::class)
fun initImgFile(path: String) {
    val file = File(path)
    if (!file.parentFile?.exists()!!) {
        file.parentFile?.mkdirs()
    }
    val imgFiles = File(file.parentFile!!.absolutePath + "/images")
    if (!imgFiles.exists()) {
        imgFiles.mkdirs()
    }
    if (file.exists()) {
        file.delete()
    }
    file.createNewFile()
}

fun getDateString(date: Int): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(date.toLong() * 1000L))
}

fun fix(password: String): String {
    var str = ""
    val key = mmkv["key", ""]
    for (i in password.indices) str += chr(ord(password[i]) xor ord(key[i % key.length]))
    return str
}

fun fix(password: ByteArray): String {
    var rowByte = byteArrayOf()
    val key = mmkv["key", ""]
    for (i in password.indices) rowByte += (password[i] xor ord(key[i % key.length]).toByte())
    return String(rowByte)
}

fun unfix(input: String, password: ByteArray) {
    val rowByte = input.toByteArray()
    var key0 = ""
    var key1 = ""
    var key2 = ""
    var key3 = ""
    var byte: Byte
    try {
        for (i in password.indices) {
            byte = password[i] xor rowByte[i]
            key0 += chr(byte.toInt())
        }
        for (i in 4 until key0.length) {
            if (2 * i > key0.length - 1) break
            for (j in 1..i) key1 += key0[j]
            for (j in i..2 * i) key2 += key0[j]
            for (j in 2 * i until key0.length) key3 += key0[j]
            var flag = true
            for (j in key1.indices) {
                if ((j < key2.length && key1[j] != key2[j]) || j < key3.length && key1[j] != key3[j]) {
                    flag = false
                    break
                }
            }
            if (flag) break
        }
    } catch (e: java.lang.Exception) {

    }
}

fun htmlStrByType(type: Int): String = when (type) {
    -2000 -> "<font color=\"#30b9d4\">[图片]</font>"
    -2002 -> "<font color=\"#30b9d4\">[语音]</font>"
    -2005 -> "<font color=\"#30b9d4\">[文件]</font>"
    -2009 -> "<font color=\"#30b9d4\">[QQ电话]</font>"
    -2011 -> "<font color=\"#30b9d4\">[分享]或[收藏]或[位置]或[联系人]</font>"
    -2025 -> "<font color=\"#30b9d4\">[红包]或[转账]</font>"
    -2039 -> "<font color=\"#30b9d4\">[厘米秀]</font>"
    -3009 -> "<font color=\"#30b9d4\">[文件]</font>"
    -5012 -> "<font color=\"#30b9d4\">[戳一戳]</font>"
    -1000 -> " "
    -1051 -> " "
    else -> "<font color=\"#30b9d4\">[其他消息]</font>"
}

var dbLastData = byteArrayOf()
fun decodeKey(str: String): String {
    val msgEnc = str.toByteArray()
    var keyS = ""
    for (i in str.indices) keyS += chr((dbLastData[i] xor msgEnc[i]).toInt())
    var realK = ""
    var nextK = ""
    var restK = ""
    for (i in 4 until keyS.length) {
        for (j in 0..i) realK += keyS[j]
        for (j in i..2 * i) {
            try {
                nextK += keyS[j]
            } catch (e: java.lang.Exception) {
            }
        }
        if (2 * i < keyS.length) for (j in 2 * i until keyS.length) restK += keyS[j]
        var flagLoop = true
        for (j in realK.indices) {
            if ((j < nextK.length && realK[j] != nextK[j])
                || (j < restK.length && realK[j] != restK[j])
            ) {
                flagLoop = false
                break
            }
        }
        if (flagLoop) break
    }
    Data.key = realK
    return realK
}

fun ord(char: Char) = char.toInt()
fun chr(int: Int) = int.toChar()