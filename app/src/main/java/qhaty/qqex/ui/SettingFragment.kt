package qhaty.qqex.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jaredrummler.android.shell.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import qhaty.qqex.R
import qhaty.qqex.databinding.FragSettingBinding
import qhaty.qqex.databinding.ItemSettingBinding
import qhaty.qqex.util.get
import qhaty.qqex.util.mmkv
import qhaty.qqex.util.set

class SettingFragment : BaseFragment() {
    private lateinit var binding: FragSettingBinding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.settingRv.adapter = SettingAdapter(layoutInflater, items)
        binding.settingRv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }

    private val items = listOf(SettingItemData(R.string.use_root, true, mmkv["root", false]) {
        if (it!!.isChecked) {
            lifecycleScope.launch(Dispatchers.Default) {
                if (!Shell.SU.available()) withContext(Dispatchers.Main) { it.isChecked = false }
                else mmkv["root"] = true
            }
        }
    }, SettingItemData(R.string.self_key, true, mmkv["self_key", false]) {
        mmkv["self_key"] = it!!.isChecked
    })

}

class SettingItemData(@StringRes var textId: Int, var hadSwitch: Boolean, var default: Boolean, var onClick: (view: CompoundButton?) -> Unit)
class SettingViewholder(val binding: ItemSettingBinding) : RecyclerView.ViewHolder(binding.root)

class SettingAdapter(private val inflater: LayoutInflater, private val dataList: List<SettingItemData>) : RecyclerView.Adapter<SettingViewholder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = SettingViewholder(ItemSettingBinding.inflate(inflater, parent, false))
    override fun onBindViewHolder(holder: SettingViewholder, position: Int) {
        val data = dataList[position]
        holder.binding.apply {
            settingTv.setText(data.textId)
            settingWitch.isChecked = data.default
            settingWitch.visibility = if (data.hadSwitch) View.VISIBLE else View.GONE
            if (data.hadSwitch) settingWitch.setOnCheckedChangeListener { v, _ -> data.onClick(v) }
            else settingTv.setOnClickListener { data.onClick(null) }
        }
    }

    override fun getItemCount(): Int = dataList.size

}