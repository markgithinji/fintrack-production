import SwiftUI
import shared

struct TransactionScreen: View {
    @StateObject private var observable = TransactionObservable()

    var body: some View {
        List(observable.transactions, id: \.id) { tx in
            Text("\(tx.category): \(tx.amount)")
        }
        .onAppear { observable.refresh() }
    }
}
