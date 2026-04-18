package com.saigonphantomlabs.language

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.saigonphantomlabs.chess.R

class LanguageBottomSheet : BottomSheetDialogFragment() {

    private val languages = listOf(
        Pair("en", "English"),
        Pair("id", "Bahasa Indonesia"),
        Pair("ms", "Bahasa Melayu"),
        Pair("de", "Deutsch"),
        Pair("es", "Español"),
        Pair("fr", "Français"),
        Pair("pt", "Português"),
        Pair("vi", "Tiếng Việt"),
        Pair("tr", "Türkçe"),
        Pair("km", "ភាសាខ្មែរ"),
        Pair("lo", "ລາວ"),
        Pair("th", "ไทย"),
        Pair("ja", "日本語"),
        Pair("zh", "中文"),
        Pair("ko", "한국어")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_language, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvLanguages: RecyclerView = view.findViewById(R.id.rvLanguages)
        rvLanguages.layoutManager = LinearLayoutManager(requireContext())
        
        val currentLang = LanguageManager.getLanguage(requireContext())
        rvLanguages.adapter = LanguageAdapter(languages, currentLang) { selectedLangCode ->
            LanguageManager.setLanguage(requireContext(), selectedLangCode)
            dismiss()
            requireActivity().recreate()
        }
    }

    private inner class LanguageAdapter(
        private val list: List<Pair<String, String>>,
        private val currentLang: String,
        private val onLanguageSelected: (String) -> Unit
    ) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

        inner class LanguageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvName: TextView = itemView.findViewById(R.id.tvLanguageName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_language, parent, false)
            return LanguageViewHolder(view)
        }

        override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
            val (code, name) = list[position]
            holder.tvName.text = name

            if (code == currentLang) {
                holder.tvName.setTypeface(null, android.graphics.Typeface.BOLD)
                holder.tvName.setTextColor(androidx.core.content.ContextCompat.getColor(holder.itemView.context, R.color.game_gold_primary))
            } else {
                holder.tvName.setTypeface(null, android.graphics.Typeface.NORMAL)
                holder.tvName.setTextColor(androidx.core.content.ContextCompat.getColor(holder.itemView.context, R.color.game_text_muted))
            }

            holder.itemView.setOnClickListener {
                onLanguageSelected(code)
            }
        }

        override fun getItemCount() = list.size
    }
}
