package com.example.demomlkit.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.demomlkit.R
import com.example.demomlkit.databinding.CategoryCustomLayoutBinding
import java.io.File

class RecordedVideoAdapter(private val videoList : ArrayList<File>, private val onVideoClickInterface : OnVideoClickInterface) : RecyclerView.Adapter<RecordedVideoAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(CategoryCustomLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(videoList[position])
        holder.itemView.setOnClickListener {
            onVideoClickInterface.onClick(position)
            notifyDataSetChanged()
        }
    }

    override fun getItemCount() = videoList.size

    class ViewHolder(private val binding: CategoryCustomLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(s: File) {
        //    binding.tvCat.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
            binding.tvCat.text = s.name
        }
    }

    interface OnVideoClickInterface{
        fun onClick(position: Int)
    }
}