import SwiftUI
import UIKit
import Shared

/// Bridges the Kotlin `MainViewController()` (a Compose UIViewController) into SwiftUI.
/// The top-level Kotlin function in MainViewController.kt is exposed to Swift, under the
/// file-name-derived class `MainViewControllerKt`.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            // Let Compose own the keyboard avoidance, as it does on Android.
            .ignoresSafeArea(.keyboard)
    }
}
