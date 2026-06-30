import SwiftUI

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                // Compose Multiplatform draws edge-to-edge and manages its own insets,
                // mirroring the Android enableEdgeToEdge() host.
                .ignoresSafeArea(.all)
        }
    }
}
