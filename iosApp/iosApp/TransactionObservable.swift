import shared

class TransactionObservable: ObservableObject {
    let vm: TransactionViewModel
    @Published var transactions: [Transaction] = []

    private var handle: Kotlinx_coroutines_coreJob?

    init() {
        vm = TransactionViewModel(repo: TransactionRepository())
        handle = FlowUtilsKt.collect(vm.transactions) { data in
            self.transactions = data as! [Transaction]
        }
    }

    func refresh() {
        vm.refresh()
    }

    deinit {
        handle?.cancel()
    }
}
