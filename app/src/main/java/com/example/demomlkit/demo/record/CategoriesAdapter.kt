/*
package com.google.mlkit.vision.demo.record

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.mlkit.vision.demo.R
import com.google.mlkit.vision.demo.databinding.CategoryCustomLayoutBinding

class CategoriesAdapter(private val catList : ArrayList<String>, val onCatClickInterface : OnCatClickInterface) : RecyclerView.Adapter<CategoriesAdapter.ViewHolder>() {
    var pos = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(CategoryCustomLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(catList[position], pos)
        holder.itemView.setOnClickListener {
            pos = position
            onCatClickInterface.onClick(catList[position])
            notifyDataSetChanged()
        }
    }

    override fun getItemCount() = catList.size

    class ViewHolder(private val binding: CategoryCustomLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(s: String, pos: Int) {
            binding.tvCat.text = s

            if (absoluteAdapterPosition == pos) {
                binding.tvCat.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.teal_200
                    )
                )
            } else {
                binding.tvCat.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.white
                    )
                )
            }
        }
    }

    interface OnCatClickInterface{
        fun onClick(cat: String)
    }
}*/
