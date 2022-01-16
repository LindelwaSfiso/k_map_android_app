package org.xhanka.k_map.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.xhanka.k_map.R

class RulesAdapter : ListAdapter<String, RulesAdapter.RulesVH>(RULE_COMPARATOR) {

    init {
        submitList(karnaughSteps)
    }

    class RulesVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var ruleTextView: TextView = itemView.findViewById(R.id.ruleTextView)
    }

    companion object {
        val RULE_COMPARATOR = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }
        }

        val karnaughSteps = listOf(
            "While grouping, you can make groups of 2^n number where n = 0, 1, 2, 3 ...",
            "You can either make groups of 1's or 0's but not both.",
            "Grouping of 1's lead to Sum of Product form and grouping of 0's lead to Product of Sum form.",
            "While grouping, the groups of 1's should not contain and 0 and the group 0f 0's should not contain any 1.",
            "The function output for 0's grouping should be complemented as G'.",
            "Groups can be made vertically and horizontally but not diagonally.",
            "Groups made should be as large as possible even if the overlap.",
            "All the like terms should be in  a group even if they overlap.",
            "Uppermost and lowermost squares can be made into a group together as they are adjacent (1-bit difference). Same goes for the corner square.",
            "Each group represent a term in the Boolean expression. Larger the group, smaller and simple the term.",
            "Don't care '-'/'X' should also be included while grouping to make a larger possible group."
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RulesVH {
        return RulesVH(
            LayoutInflater.from(parent.context).inflate(
                R.layout.rule_item, parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: RulesVH, position: Int) {
        holder.ruleTextView.text = getItem(position)
    }
}