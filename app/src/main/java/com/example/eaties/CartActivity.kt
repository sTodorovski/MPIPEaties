package com.example.eaties

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.eaties.adapter.MyCartAdapter
import com.example.eaties.eventbus.UpdateCartEvent
import com.example.eaties.listener.ICartLoadListener
import com.example.eaties.model.CartModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class CartActivity : AppCompatActivity(), ICartLoadListener { // tochno

    var cartLoadListener: ICartLoadListener?= null

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        if(EventBus.getDefault().hasSubscriberForEvent(UpdateCartEvent::class.java))
            EventBus.getDefault().removeStickyEvent(UpdateCartEvent::class.java)
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onUpdateCartEvent(event: UpdateCartEvent) {
        loadCartFromFirebase()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)
        init()
        loadCartFromFirebase()
        supportActionBar?.hide()
    }

    private fun loadCartFromFirebase() { // tochno
        val cartModels : MutableList<CartModel> = ArrayList()
        FirebaseDatabase.getInstance()
            .getReference("Cart")
            .child("UNIQUE_USER_ID")
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for(cartSnapshot in snapshot.children) {
                        val cartModel = cartSnapshot.getValue(CartModel::class.java)
                        cartModel!!.key = cartSnapshot.key
                        cartModels.add(cartModel)
                    }
                    cartLoadListener!!.onLoadCartSuccess(cartModels)
                }

                override fun onCancelled(error: DatabaseError) {
                    cartLoadListener!!.onLoadCartFailed(error.message)
                }
            })
    }

    private fun init() { // tochno
        cartLoadListener = this
        val layoutManager = LinearLayoutManager(this)
        val recycler_cart = findViewById<RecyclerView>(R.id.recycler_cart)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        var btnFinish = findViewById<ImageView>(R.id.btnFinish)
        var msg = "Your order is on its way!"
        recycler_cart.layoutManager = layoutManager
        recycler_cart.addItemDecoration(DividerItemDecoration(this, layoutManager.orientation))
        btnBack!!.setOnClickListener { finish() }
        btnFinish!!.setOnClickListener {
            FirebaseDatabase.getInstance()
                .getReference("Cart")
                .child("UNIQUE_USER_ID")
                .removeValue()
                .addOnSuccessListener { EventBus.getDefault().postSticky(UpdateCartEvent()) }
            NotificationHelper(this,msg).Notification()
            Toast.makeText(this,"Sending you a notification",Toast.LENGTH_SHORT).show()
        }
    }

    override fun onLoadCartSuccess(cartModelList: List<CartModel>) { //tochno
        var sum = 0.0
        for(cartModel in cartModelList) {
            sum += cartModel.totalPrice
        }

        val txtTotal = findViewById<TextView>(R.id.txtTotal)
        txtTotal.text = StringBuilder("$").append(sum)
        val adapter = MyCartAdapter(this, cartModelList)
        val recycler_cart = findViewById<RecyclerView>(R.id.recycler_cart)
        recycler_cart!!.adapter = adapter
    }

    override fun onLoadCartFailed(message: String?) {

    }
}