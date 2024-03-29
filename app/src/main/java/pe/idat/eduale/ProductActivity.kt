package pe.idat.eduale

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.GridLayoutManager
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import pe.idat.eduale.adapter.CartAdapter
import pe.idat.eduale.adapter.ProductAdapter
import pe.idat.eduale.databinding.ActivityProductBinding
import pe.idat.eduale.model.ProductModel
import pe.idat.eduale.network.ProductService
import pe.idat.eduale.network.RetroInstance
import pe.idat.eduale.room.cart.CartApp
import pe.idat.eduale.room.cart.CartModel
import pe.idat.eduale.room.cart.OnItemListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.content.Intent as Intent

class ProductActivity : AppCompatActivity(), SearchView.OnQueryTextListener, OnProductListener,
    OnItemListener {

    private lateinit var binding: ActivityProductBinding
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var myAdapter: ProductAdapter
    private lateinit var gridLayoutManager: GridLayoutManager
    private lateinit var cartAdapter: CartAdapter
    var productsList = mutableListOf<ProductModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cartAdapter = CartAdapter(mutableListOf(), this@ProductActivity)

        binding.recyclerProducts.setHasFixedSize(true)
        gridLayoutManager = GridLayoutManager(this, 2)
        binding.recyclerProducts.layoutManager = gridLayoutManager

        binding.txtSearch.setOnQueryTextListener(this)

        //Enviar datos de cliente y usuario a carrito
        binding.btnShoppingCart.setOnClickListener {
            val value = Intent(this, CartActivity::class.java)

            val objetoIntent: Intent = intent
            val ClienteID = objetoIntent.getStringExtra("ClienteID")
            val UsuarioID = objetoIntent.getStringExtra("UsuarioID")
            value.putExtra("ClienteID", ClienteID)
            value.putExtra("UsuarioID", UsuarioID)
            startActivity(value)
        }

        getMyData()

        //Drawer menu
        binding.apply {
            toggle = ActionBarDrawerToggle(
                this@ProductActivity,
                drawerLayout,
                R.string.open,
                R.string.close
            )
            drawerLayout.addDrawerListener(toggle)
            toggle.syncState()

            supportActionBar?.setDisplayHomeAsUpEnabled(true)

            navView.setNavigationItemSelectedListener {
                when (it.itemId) {
                    R.id.home -> {
                        Toast.makeText(this@ProductActivity, "Home", Toast.LENGTH_SHORT).show()
                    }
                    R.id.userInformation -> {
                        val value = Intent(this@ProductActivity, UserEditPasswordActivity::class.java)

                        val objetoIntent: Intent = intent
                        val ClienteID = objetoIntent.getStringExtra("ClienteID")
                        val UsuarioID = objetoIntent.getStringExtra("UsuarioID")
                        value.putExtra("ClienteID", ClienteID)
                        value.putExtra("UsuarioID", UsuarioID)
                        startActivity(value)
                    }
                    R.id.clientInformation -> {
                        val value = Intent(this@ProductActivity, ClientInformationActivity::class.java)

                        val objetoIntent: Intent = intent
                        val ClienteID = objetoIntent.getStringExtra("ClienteID")
                        val UsuarioID = objetoIntent.getStringExtra("UsuarioID")
                        value.putExtra("ClienteID", ClienteID)
                        value.putExtra("UsuarioID", UsuarioID)
                        startActivity(value)
                    }
                    R.id.orderInformation -> {
                        val value = Intent(this@ProductActivity, OrderPendientActivity::class.java)

                        val objetoIntent: Intent = intent
                        val ClienteID = objetoIntent.getStringExtra("ClienteID")
                        val UsuarioID = objetoIntent.getStringExtra("UsuarioID")
                        value.putExtra("ClienteID", ClienteID)
                        value.putExtra("UsuarioID", UsuarioID)
                        startActivity(value)
                    }
                    R.id.logout -> {
                        val logout = Intent(this@ProductActivity, LoginActivity::class.java)
                        finish()
                        startActivity(logout)
                    }
                }
                true
            }
        }
    }

    //Drawer menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            true
        }
        return super.onOptionsItemSelected(item)
    }

    //Listado de productos
    private fun getMyData() {
        val retrofitData =
            RetroInstance().getRetroInstance().create(ProductService::class.java)

        retrofitData.getProductList().enqueue(object : Callback<List<ProductModel>?> {
            override fun onResponse(
                call: Call<List<ProductModel>?>,
                response: Response<List<ProductModel>?>
            ) {
                val responseBody = response.body()

                val products = responseBody ?: emptyList()

                productsList.addAll(products)

                myAdapter = ProductAdapter(productsList, this@ProductActivity)
                myAdapter.notifyDataSetChanged()
                binding.recyclerProducts.adapter = myAdapter

            }
            override fun onFailure(call: Call<List<ProductModel>?>, t: Throwable) {
                Log.d("MainActivity", "onFailure: " + t.message)
            }

        })
    }

    //Búsqueda de productos
    private fun searchProducts(query: String) {
        val retrofitData =
            RetroInstance().getRetroInstance().create(ProductService::class.java)

        retrofitData.getProductByName(query).enqueue(object : Callback<List<ProductModel>?> {
            override fun onResponse(
                call: Call<List<ProductModel>?>,
                response: Response<List<ProductModel>?>
            ) {
                val responseBody = response.body()

                val products = responseBody ?: emptyList()

                productsList.clear()
                productsList.addAll(products)
                myAdapter = ProductAdapter(productsList, this@ProductActivity)
                myAdapter.notifyDataSetChanged()
                binding.recyclerProducts.adapter = myAdapter

            }

            override fun onFailure(call: Call<List<ProductModel>?>, t: Throwable) {
                Log.d("MainActivity", "onFailure: " + t.message)
            }

        })

    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        if (!query.isNullOrBlank()) {
            searchProducts(query)
        }
        return true
    }

    override fun onQueryTextChange(query: String?): Boolean {
        if (query.isNullOrBlank()) {
            productsList.clear()
            getMyData()
        }
        return true
    }


    //Product button click
    override fun onProductButtonClick(position: Int) {
        val product = productsList.get(position)

        val cantidad = 1
        val precio = product.precioventa
        val subtotal = cantidad * precio!!

        val cart = CartModel(
            nombre = product.nombre + " - " + product.descripcion,
            marca = product.marca!!.nombre!!,
            precio = precio,
            cantidad = cantidad,
            stock = product.stock!!,
            imagen = product.imagen!!,
            subtotal = subtotal,
            productId = product.productoid!!
        )
        registerItem(cart)
        Toast.makeText(this, "Añadido al carrito", Toast.LENGTH_SHORT).show()
    }

    private fun registerItem(cartModel: CartModel) {
        doAsync {
            CartApp.database.cartDao().addItem(cartModel)

            uiThread {
                cartAdapter.newItem(cartModel)
            }
        }
    }

    //Detail product
    override fun onProductClick(position: Int) {
        val product = productsList.get(position)

        val value = Intent(this,ProductDetailActivity::class.java)

        val objetoIntent: Intent = intent
        val ClienteID = objetoIntent.getStringExtra("ClienteID")
        val UsuarioID = objetoIntent.getStringExtra("UsuarioID")

        value.putExtra("ClienteID", ClienteID)
        value.putExtra("UsuarioID", UsuarioID)

        value.putExtra("productoid", product.productoid.toString())
        value.putExtra("nombreDesc", product.nombre + " " + product.descripcion)
        value.putExtra("marca", product.marca!!.nombre!!)
        value.putExtra("precio", product.precioventa.toString())
        value.putExtra("stock", product.stock.toString())
        value.putExtra("imagen", product.imagen)
        value.putExtra("categoria", product.categoria!!.nombre)
        value.putExtra("marca", product.marca!!.nombre)

        startActivity(value)
    }

    //Init cart view
    override fun onDeleteClick(position: Int) {
        TODO("Not yet implemented")
    }

    override fun onAddClick(position: Int) {
        TODO("Not yet implemented")
    }

    override fun onSubtractClick(position: Int) {
        TODO("Not yet implemented")
    }
}