import SwiftUI
import SwiftData
import UserNotifications
import FirebaseCore
import FirebaseAuth
import GoogleSignIn

@main
struct UsageResetApp: App {
    var sharedModelContainer: ModelContainer = {
        let schema = Schema([
            ResetItem.self,
            ColorSwatch.self
        ])
        let modelConfiguration = ModelConfiguration(schema: schema, isStoredInMemoryOnly: false)

        do {
            return try ModelContainer(for: schema, configurations: [modelConfiguration])
        } catch {
            fatalError("Could not create ModelContainer: \(error)")
        }
    }()
    
    @ObservedObject private var profileManager = MultiProfileManager.shared

    init() {
        requestNotificationPermission()
        FirebaseApp.configure()
        // Wait for profile manager to initialize
        MultiProfileManager.shared.configure()
    }

    var body: some Scene {
        WindowGroup {
            Group {
                ContentView()
            }
            .onOpenURL { url in
                GIDSignIn.sharedInstance.handle(url)
            }
        }
        .modelContainer(sharedModelContainer)

#if targetEnvironment(macCatalyst)
        MenuBarExtra("My Countdowns", systemImage: "timer") {
            ContentView()
                .modelContainer(sharedModelContainer)
                .frame(width: 400, height: 600)
        }
        .menuBarExtraStyle(.window)
#endif
    }
    
    private func requestNotificationPermission() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
            if let error = error {
                print("Notification permission error: \(error)")
            }
        }
    }
}
