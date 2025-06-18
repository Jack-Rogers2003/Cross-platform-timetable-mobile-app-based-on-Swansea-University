package com.example.mobileapp


import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.graphics.Color
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

class RecyclerviewAdapter(private val sections: MutableList<Pair<String, MutableList<List<String>>>>, private val context: Context) :
    RecyclerView.Adapter<ViewHolder>() {

    private val expandedSections = HashSet<Int>()

    private val VIEW_TYPE_HEADER = 0
    private val VIEW_TYPE_ITEM = 1

    override fun getItemViewType(position: Int): Int {
        return if (getItemAt(position) is String) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.row_header, parent, false)
            SectionViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.row_layout, parent, false)
            ItemViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder is SectionViewHolder) {
            val sectionIndex = getSectionIndex(position)
            holder.sectionHeader.text = sections[sectionIndex].first


            // Set arrow rotation based on expanded state
            val isExpanded = expandedSections.contains(sectionIndex)
            holder.arrowIcon.rotation = if (isExpanded) 180f else 0f

            // Handle click to toggle expansion
            holder.itemView.setOnClickListener {
                if (isExpanded) {
                    expandedSections.remove(sectionIndex)
                    animateArrow(holder.arrowIcon, 180f, 0f)
                } else {
                    expandedSections.add(sectionIndex)
                    animateArrow(holder.arrowIcon, 0f, 180f)
                }

                notifyDataSetChanged()  // Ensure RecyclerView updates first

                // Wait until layout updates before scrolling
                holder.itemView.post {
                    val recyclerView = (holder.itemView.context as? AppCompatActivity)
                        ?.findViewById<RecyclerView>(R.id.recyclerView)

                    recyclerView?.adapter?.let { adapter ->
                        val safePosition = holder.adapterPosition.coerceAtMost(adapter.itemCount - 1)
                        if (safePosition >= 0) {
                            recyclerView.smoothScrollToPosition(safePosition)
                        }
                    }
                }
            }


        } else if (holder is ItemViewHolder) {
            val item = getItemAt(position) as List<*>
            print(item[0])
            val spannableText = SpannableStringBuilder()
            val modules = context.getSharedPreferences("modules", MODE_PRIVATE).getString("modules", "")?.split(",")

            if (modules != null) {
                for (line in item) {
                    val lineAsString = line.toString()
                    val spannableLine = SpannableString(lineAsString)

                    // Check for each module and apply colour
                    for (module in modules) {
                        if (lineAsString.contains(module)) {
                            val colour =
                                context.getSharedPreferences("modulecolours", MODE_PRIVATE)
                                    .getInt(module, Color.BLACK)
                            spannableLine.setSpan(
                                ForegroundColorSpan(colour),
                                0,
                                lineAsString.length,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                            break // Once a match is found, no need to check further modules
                        }
                    }
                    spannableText.append(spannableLine).append("\n")
                }
            }
            holder.textTitle.text = spannableText
        }
    }


    override fun getItemCount(): Int {
        var count = sections.size
        sections.forEachIndexed { index, pair ->
            if (expandedSections.contains(index)) {
                count += pair.second.size
            }
        }
        return count
    }

    private fun getItemAt(position: Int): Any {
        var currentPosition = 0
        for (i in sections.indices) {
            if (currentPosition == position) return sections[i].first
            currentPosition++

            if (expandedSections.contains(i)) {
                val items = sections[i].second
                if (position < currentPosition + items.size) {
                    return items[position - currentPosition]
                }
                currentPosition += items.size
            }
        }
        return ""
    }

    private fun getSectionIndex(position: Int): Int {
        var currentPosition = 0
        for (i in sections.indices) {
            if (currentPosition == position) return i
            currentPosition++

            if (expandedSections.contains(i)) {
                currentPosition += sections[i].second.size
            }
        }
        return -1
    }

    private fun animateArrow(arrow: ImageView, from: Float, to: Float) {
        val rotate = RotateAnimation(
            from, to, RotateAnimation.RELATIVE_TO_SELF, 0.5f,
            RotateAnimation.RELATIVE_TO_SELF, 0.5f
        )
        rotate.duration = 200
        rotate.fillAfter = true
        arrow.startAnimation(rotate)
    }

    class SectionViewHolder(view: View) : ViewHolder(view) {
        val sectionHeader: TextView = view.findViewById(R.id.textSectionHeader)
        val arrowIcon: ImageView = view.findViewById(R.id.arrowIcon)
    }

    class ItemViewHolder(view: View) : ViewHolder(view) {
        val textTitle: TextView = view.findViewById(R.id.moduleName)
    }
}



