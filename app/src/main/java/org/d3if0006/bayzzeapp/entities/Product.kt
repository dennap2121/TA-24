import android.os.Parcel
import android.os.Parcelable

data class Product(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    var quantity: Int = 0,
    val description: String = "",
    val image: String = "",
    val category: String = "",
    val isRecommended: Boolean = false // Added isRecommended field
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readDouble(),
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte() // Read isRecommended as a Boolean
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeDouble(price)
        parcel.writeInt(quantity)
        parcel.writeString(description)
        parcel.writeString(image)
        parcel.writeString(category)
        parcel.writeByte(if (isRecommended) 1 else 0) // Write isRecommended as a Byte
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Product> {
        override fun createFromParcel(parcel: Parcel): Product {
            return Product(parcel)
        }

        override fun newArray(size: Int): Array<Product?> {
            return arrayOfNulls(size)
        }
    }
}