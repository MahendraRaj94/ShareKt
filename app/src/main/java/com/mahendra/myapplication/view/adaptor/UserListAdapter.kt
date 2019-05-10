package com.mahendra.myapplication.view.adaptor

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mahendra.myapplication.R
import kotlinx.android.synthetic.main.user_list_item.view.*

class UserListAdapter(val items : List<UserItem>) : RecyclerView.Adapter<UserListAdapter.UserHolder>() {
    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): UserHolder {
        return UserHolder(LayoutInflater.from(p0.context).inflate(R.layout.user_list_item,p0,false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(p0: UserHolder, p1: Int) {
        p0.itemView.tvName
    }

    class UserHolder(view: View) : RecyclerView.ViewHolder(view) {
    }

}