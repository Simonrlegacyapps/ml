package com.google.mlkit.vision.demo.record

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.mlkit.vision.demo.R

internal class CatAdapter(private var catList : ArrayList<String>, val onCatClickInterface : CatAdapter.OnCatClickInterface) :
    RecyclerView.Adapter<CatAdapter.MyViewHolder>() {
    var pos = -1

    internal inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var tvCat: TextView = view.findViewById(R.id.tvCat)
    }
    @NonNull
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.category_custom_layout, parent, false)
        return MyViewHolder(itemView)
    }
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val category = catList[position]
        holder.tvCat.text = category
        if (position == pos) {
            holder.tvCat.setTextColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.teal_200
                )
            )
        } else {
            holder.tvCat.setTextColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.white
                )
            )
        }
        holder.itemView.setOnClickListener {
            pos = position
            onCatClickInterface.onClick(catList[position])
            notifyDataSetChanged()
        }
    }
    override fun getItemCount(): Int {
        return catList.size
    }

    interface OnCatClickInterface{
        fun onClick(cat: String)
    }
}