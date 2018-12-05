package com.mai.xapkinstaller.adapter

import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.mai.xapkinstaller.model.XApkFile
import com.mai.xapkinstaller.R

class XApkAdapter(data: ArrayList<XApkFile>, onChildClickListener: OnChildClickListener, onLongClickListener: OnLongClickListener) : RecyclerView.Adapter<XApkAdapter.XApkViewHolder>() {

    private var mData = data
    private var mOnChildClickListener: OnChildClickListener = onChildClickListener
    private var mOnLongClickListener: OnLongClickListener = onLongClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): XApkViewHolder {
        return XApkViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.xapk_item, parent, false))
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    override fun onBindViewHolder(holder: XApkViewHolder, position: Int) {
        //ICON不为空则显示
        if (mData[position].icon != null)
            holder.icon.setImageDrawable(mData[position].icon)
        //文件名
        holder.title.text = mData[position].fileName
        //应用名
        holder.name.text = mData[position].appName
        //文件大小
        holder.size.text = mData[position].size
        //安装键
        holder.installBtn.setOnClickListener {
            mOnChildClickListener.onInstall(holder, position)
        }
        //刪除鍵
        holder.deleteBtn.setOnClickListener {
            mOnChildClickListener.onDelete(holder, position)
        }
        holder.view.setOnLongClickListener {
            mOnLongClickListener.onLongClick(position)
            true
        }
    }

    class XApkViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var view:View = view.findViewById(R.id.container)
        var icon: AppCompatImageView = view.findViewById(R.id.ivIcon)
        var title: TextView = view.findViewById(R.id.tvTitle)
        var name: TextView = view.findViewById(R.id.tvName)
        var size: TextView = view.findViewById(R.id.tvSize)
        var installBtn: TextView = view.findViewById(R.id.install)
        var deleteBtn: TextView = view.findViewById(R.id.delete)
    }

    interface OnChildClickListener {
        fun onInstall(holder: XApkViewHolder, position: Int)
        fun onDelete(holder: XApkViewHolder, position: Int)
    }

    interface OnLongClickListener{
        fun onLongClick(position: Int)
    }
}