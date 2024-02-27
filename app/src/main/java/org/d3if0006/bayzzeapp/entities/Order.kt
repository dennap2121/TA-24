import android.os.Parcel
import android.os.Parcelable
import java.util.Date

data class Order(
    val id: String,
    val deliveryType: String,
    val total: Double,
    val createdAt: Date,
    val products: List<OrderProduct>,
    val pengiriman: Double,
    val subtotal: Double,
    val name: String,
    val userId: String,
    val address: String,
    val notes: String,
    val status: String,
    ) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readDouble(),
        Date(parcel.readLong()),
        parcel.createTypedArrayList(OrderProduct.CREATOR) ?: emptyList(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(deliveryType)
        parcel.writeDouble(total)
        parcel.writeLong(createdAt.time)
        parcel.writeTypedList(products)
        parcel.writeDouble(pengiriman)
        parcel.writeDouble(subtotal)
        parcel.writeString(name)
        parcel.writeString(userId)
        parcel.writeString(address)
        parcel.writeString(notes)
        parcel.writeString(status)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Order> {
        override fun createFromParcel(parcel: Parcel): Order {
            return Order(parcel)
        }

        override fun newArray(size: Int): Array<Order?> {
            return arrayOfNulls(size)
        }
    }
}

data class OrderProduct(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val productImage: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readString() ?: "",
        )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(productId)
        parcel.writeString(productName)
        parcel.writeInt(quantity)
        parcel.writeString(productImage)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<OrderProduct> {
        override fun createFromParcel(parcel: Parcel): OrderProduct {
            return OrderProduct(parcel)
        }

        override fun newArray(size: Int): Array<OrderProduct?> {
            return arrayOfNulls(size)
        }
    }
}
