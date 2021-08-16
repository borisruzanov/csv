package com.boris.expert.csvmagic.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.boris.expert.csvmagic.R
import com.boris.expert.csvmagic.model.Fonts

class FontAdapter(var context: Context, private var fontList:List<Fonts>):RecyclerView.Adapter<FontAdapter.ItemViewHolder>() {


    interface OnItemClickListener{
        fun onItemClick(position: Int)
    }
    private var mListener: OnItemClickListener?=null
    private var isIconUpdate:Boolean = false
    companion object{
        var selected_position = -1
    }

    fun setOnItemClickListener(listener: OnItemClickListener){
        this.mListener = listener
    }

    fun updateIcon(flag:Boolean)
    {
        isIconUpdate = flag
    }

    class ItemViewHolder(itemView: View, mListener: OnItemClickListener) : RecyclerView.ViewHolder(itemView)
    {
        val image: AppCompatImageView = itemView.findViewById(R.id.font_item)
        val icon : AppCompatImageView = itemView.findViewById(R.id.selected_icon)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.font_family_item_row,
            parent,
            false
        )
        return ItemViewHolder(view, mListener!!)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val font = fontList[position]

        Glide.with(context).load(font.fontImage).into(holder.image)
        if (selected_position == position && isIconUpdate)
        {
            holder.icon.visibility = View.VISIBLE
        }
        else
        {
            holder.icon.visibility = View.INVISIBLE
        }

        holder.image.setOnClickListener {
            val previousItem: Int = selected_position
            selected_position = position

            notifyItemChanged(previousItem)
            notifyItemChanged(position)

            mListener!!.onItemClick(position)
        }
    }

    override fun getItemCount(): Int {
        return fontList.size
    }

}