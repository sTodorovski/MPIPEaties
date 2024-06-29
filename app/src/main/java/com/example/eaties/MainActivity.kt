package com.example.eaties
import android.content.Intent
import android.media.Image
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.eaties.adapter.MyFoodAdapter
import com.example.eaties.eventbus.UpdateCartEvent
import com.example.eaties.listener.ICartLoadListener
import com.example.eaties.listener.IFoodLoadListener
import com.example.eaties.model.CartModel
import com.example.eaties.model.FoodModel
import com.example.eaties.ui.theme.EatiesTheme
import com.example.eaties.utils.SpaceItemDecoration
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nex3z.notificationbadge.NotificationBadge
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import com.google.firebase.auth.ktx.auth

class MainActivity : ComponentActivity(), IFoodLoadListener, ICartLoadListener {
    lateinit var foodLoadListener: IFoodLoadListener
    lateinit var cartLoadListener: ICartLoadListener
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var mAuth: FirebaseAuth

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
    fun onUpdateCartEvent(event:UpdateCartEvent) {
        countCartFromFirebase()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
        loadFoodFromFirebase()
        countCartFromFirebase()
        mAuth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("243922566776-a8cr0ob4fsm12ra0eseh8f62ro0ptfig.apps.googleusercontent.com")
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        val auth = Firebase.auth
        val user = auth.currentUser

        val logoutButton = findViewById<ImageView>(R.id.logoutButton)
        logoutButton.setOnClickListener {
            signOutAndStartSignInActivity()
        }
    }

    private fun signOutAndStartSignInActivity() {
        mAuth.signOut()

        mGoogleSignInClient.signOut().addOnCompleteListener(this) {
            // Optional: Update UI or show a message to the user
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun countCartFromFirebase() {
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
                    cartLoadListener.onLoadCartSuccess(cartModels)
                }

                override fun onCancelled(error: DatabaseError) {
                    cartLoadListener.onLoadCartFailed(error.message)
                }
            })
    }

    private fun loadFoodFromFirebase() {
        val foodModels : MutableList<FoodModel> = ArrayList()
        FirebaseDatabase.getInstance()
            .getReference("Food")
            .addListenerForSingleValueEvent(object:ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.exists()) {
                        for(foodSnapshot in snapshot.children) {
                            val foodModel = foodSnapshot.getValue(FoodModel::class.java)
                            foodModel!!.key = foodSnapshot.key
                            foodModels.add(foodModel)
                        }
                        foodLoadListener.onFoodLoadSuccess(foodModels)
                    } else {
                        foodLoadListener.onFoodLoadFailed("Food item does not exist!")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    foodLoadListener.onFoodLoadFailed(error.message)
                }
            })
    }

    private fun init() {
        foodLoadListener = this
        cartLoadListener = this

        val cartButton = findViewById<ImageView>(R.id.cartButton)
        val mapButton = findViewById<ImageView>(R.id.mapButton)
        val recycler_food = findViewById<RecyclerView>(R.id.recycler_food)
        val gridLayoutManager = GridLayoutManager(this, 2)
        //val cameraButton = findViewById<ImageView>(R.id.cameraButton)
        val cameraButtonReview = findViewById<ImageView>(R.id.cameraButtonReview)
        //val loginButton = findViewById<ImageView>(R.id.loginButton)
        recycler_food.layoutManager = gridLayoutManager
        recycler_food.addItemDecoration(SpaceItemDecoration())
        cartButton.setOnClickListener{ startActivity(Intent(this, CartActivity::class.java)) }
        mapButton.setOnClickListener{ startActivity(Intent(this, MapActivity::class.java)) }
        //cameraButton.setOnClickListener{ startActivity(Intent(this, CameraActivity::class.java)) }
        cameraButtonReview.setOnClickListener{ startActivity(Intent(this, ReviewActivity::class.java)) }
        //loginButton.setOnClickListener{ startActivity(Intent(this, LoginActivity::class.java)) }
    }

    override fun onFoodLoadSuccess(foodModelList: List<FoodModel>?) {
        val adapter = MyFoodAdapter(this, foodModelList!!, cartLoadListener)
        val recycler_food = findViewById<RecyclerView>(R.id.recycler_food)
        recycler_food.adapter = adapter
    }

    override fun onFoodLoadFailed(message:String?) {

    }

    override fun onLoadCartSuccess(cartModelList: List<CartModel>) {
        val badge = findViewById<NotificationBadge>(R.id.badge)
        var cartSum = 0
        for(cartModel in cartModelList) cartSum += cartModel.quantity
        badge!!.setNumber(cartSum)
    }

    override fun onLoadCartFailed(message: String?) {

    }
}
