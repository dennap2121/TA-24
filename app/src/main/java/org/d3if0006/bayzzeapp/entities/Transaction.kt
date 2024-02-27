
data class Transaction(
    var transactionId: String = "",
    var amount: Double = 0.0,
    var date: String = ""
) {
    // Required empty constructor for Firestore
    constructor() : this("", 0.0, "")
}
