package com.example.eaties.adapter

import android.content.Context
import android.text.Layout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.eaties.R
import com.example.eaties.eventbus.UpdateCartEvent
import com.example.eaties.listener.ICartLoadListener
import com.example.eaties.listener.IRecyclerClickListener
import com.example.eaties.model.CartModel
import com.example.eaties.model.FoodModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.greenrobot.eventbus.EventBus

class MyFoodAdapter(
    private val context: Context,
    private val list:List<FoodModel>,
    private val cartListener: ICartLoadListener
    ): RecyclerView.Adapter<MyFoodAdapter.MyFoodViewHolder>() {

        class MyFoodViewHolder(itemView:View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
            var imageView: ImageView?=null
            var txtName: TextView?=null
            var txtPrice: TextView?=null
            private var clickListener:IRecyclerClickListener? = null

            fun setClickListener(clickListener: IRecyclerClickListener) {
                this.clickListener = clickListener
            }

            init {
                imageView = itemView.findViewById(R.id.foodImageView) as ImageView
                txtName = itemView.findViewById(R.id.txtName) as TextView
                txtPrice = itemView.findViewById(R.id.txtPrice) as TextView

                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                clickListener!!.onItemClickListener(v, adapterPosition)
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyFoodViewHolder {
        return MyFoodViewHolder(LayoutInflater.from(context)
            .inflate(R.layout.layout_food_item, parent, false))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: MyFoodViewHolder, position: Int) {
        Glide.with(context)
            .load(list[position].image)
            .into(holder.imageView!!)
        holder.txtName!!.text = StringBuilder().append(list[position].name)
        holder.txtPrice!!.text = StringBuilder("$").append(list[position].price)

        holder.setClickListener(object:IRecyclerClickListener{
            override fun onItemClickListener(view: View?, position: Int) {
                addToCart(list[position])
            }

        })
    }

    private fun addToCart(foodModel: FoodModel) {
        val userCart = FirebaseDatabase.getInstance()
            .getReference("Cart")
            .child("UNIQUE_USER_ID")

        userCart.child(foodModel.key!!)
            .addListenerForSingleValueEvent(object:ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.exists()) {
                        val cartModel = snapshot.getValue(CartModel::class.java)
                        val updateData: MutableMap<String, Any> = HashMap()
                        cartModel!!.quantity = cartModel!!.quantity + 1
                        updateData["quantity"] = cartModel!!.quantity
                        updateData["totalPrice"] = cartModel!!.quantity * cartModel.price!!.toFloat()

                        userCart.child(foodModel.key!!)
                            .updateChildren(updateData)
                            .addOnSuccessListener {
                                EventBus.getDefault().postSticky(UpdateCartEvent())
                                cartListener.onLoadCartFailed("Successfully added to cart!")
                            }
                            .addOnFailureListener {
                                e -> cartListener.onLoadCartFailed(e.message)
                            }
                    } else {
                        val cartModel = CartModel()
                        cartModel.key = foodModel.key
                        cartModel.name = foodModel.name
                        cartModel.image = foodModel.image
                        cartModel.price = foodModel.price
                        cartModel.quantity = 1
                        cartModel.totalPrice = foodModel.price!!.toFloat()

                        userCart.child(foodModel.key!!)
                            .setValue(cartModel)
                            .addOnSuccessListener {
                                EventBus.getDefault().postSticky(UpdateCartEvent())
                                cartListener.onLoadCartFailed("Successfully added to cart!")
                            }
                            .addOnFailureListener{
                                e -> cartListener.onLoadCartFailed(e.message)
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    cartListener.onLoadCartFailed(error.message)
                }

            })
    }


}