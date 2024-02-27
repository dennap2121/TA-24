import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.d3if0006.bayzzeapp.R

class ProductDetailModalBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_PRODUCT = "product"

        fun newInstance(product: Product): ProductDetailModalBottomSheet {
            val fragment = ProductDetailModalBottomSheet()
            val args = Bundle()
            args.putParcelable(ARG_PRODUCT, product)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var product: Product

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.modal_bottom_sheet_product_detail, container, false)
        product = arguments?.getParcelable(ARG_PRODUCT) ?: return rootView

        // Initialize views and display product details
        val productNameTextView: TextView = rootView.findViewById(R.id.productNameTextView)
        val productPriceTextView: TextView = rootView.findViewById(R.id.productPriceTextView)
        val productDescriptionTextView: TextView = rootView.findViewById(R.id.productDescriptionTextView)
        val addToCartButton: Button = rootView.findViewById(R.id.addToCartButton)
        val quantityTextView: TextView = rootView.findViewById(R.id.quantityTextView)
        val minusButton: Button = rootView.findViewById(R.id.minusButton)
        val plusButton: Button = rootView.findViewById(R.id.plusButton)

        productNameTextView.text = product.name
        productDescriptionTextView.text = product.description
        productPriceTextView.text = product.price.toString()

        productNameTextView.text = product.name
        productPriceTextView.text = product.price.toString()

        var quantity = 1

        // Increment quantity when plus button is clicked
        plusButton.setOnClickListener {
            quantity++
            quantityTextView.text = quantity.toString()
        }

        // Decrement quantity when minus button is clicked, but ensure it doesn't go below 1
        minusButton.setOnClickListener {
            if (quantity > 1) {
                quantity--
                quantityTextView.text = quantity.toString()
            }
        }

        addToCartButton.setOnClickListener {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                addToCart(uid, product, quantity)
            } else {
                Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            }
        }

        return rootView
    }

    private fun addToCart(uid: String, product: Product, quantity: Int) {
        val cartItem = hashMapOf(
            "productId" to product.id,
            "quantity" to quantity,
            "uid" to uid
        )

        val db = FirebaseFirestore.getInstance()
        db.collection("carts")
            .add(cartItem)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Success add item to cart", Toast.LENGTH_SHORT).show()
                dismiss() // Close the modal bottom sheet
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to add item to cart", Toast.LENGTH_SHORT).show()
            }
    }
}
