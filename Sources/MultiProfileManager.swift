import Foundation
import FirebaseCore
import FirebaseAuth
import FirebaseFirestore
import Combine

struct UserProfile: Identifiable, Equatable {
    var id: String { uid }
    let uid: String
    let email: String
    let photoURL: URL?
    let appName: String
    var customPhotoBase64: String?
    var ringColor: String?
}

class MultiProfileManager: ObservableObject {
    static let shared = MultiProfileManager()
    
    @Published var profiles: [UserProfile] = []
    
    private var authStateListeners: [String: AuthStateDidChangeListenerHandle] = [:]
    private var profilePhotoListeners: [String: ListenerRegistration] = [:]
    
    private init() {
        // Initialization happens in configure()
    }
    
    func configure() {
        guard authStateListeners.isEmpty else { return }
        
        // Load saved app names from UserDefaults
        var savedApps = UserDefaults.standard.stringArray(forKey: "activeAppNames") ?? ["[DEFAULT]"]
        
        // Always listen to [DEFAULT] unconditionally so that LoginView can work for the primary account
        if !savedApps.contains("[DEFAULT]") {
            savedApps.append("[DEFAULT]")
        }
        
        for appName in savedApps {
            if appName == "[DEFAULT]" {
                setupListener(for: "[DEFAULT]")
            } else {
                if let options = FirebaseApp.app()?.options {
                    FirebaseApp.configure(name: appName, options: options)
                    setupListener(for: appName)
                }
            }
        }
    }
    
    private func setupListener(for appName: String) {
        guard let app = FirebaseApp.app(name: appName) else { return }
        let auth = Auth.auth(app: app)
        
        let handle = auth.addStateDidChangeListener { [weak self] _, user in
            guard let self = self else { return }
            DispatchQueue.main.async {
                if let user = user {
                    if self.profiles.contains(where: { $0.uid == user.uid && $0.appName != appName }) {
                        do { try auth.signOut() } catch {}
                        return
                    }
                    
                    let profile = UserProfile(uid: user.uid, email: user.email ?? "Unknown", photoURL: user.photoURL, appName: appName)
                    if let index = self.profiles.firstIndex(where: { $0.appName == appName }) {
                        self.profiles[index] = profile
                    } else {
                        self.profiles.append(profile)
                    }
                    
                    self.setupProfilePhotoListener(for: appName, uid: user.uid)
                } else {
                    self.profiles.removeAll(where: { $0.appName == appName })
                    self.profilePhotoListeners[appName]?.remove()
                    self.profilePhotoListeners.removeValue(forKey: appName)
                }
                self.saveActiveApps()
            }
        }
        authStateListeners[appName] = handle
    }
    
    private func setupProfilePhotoListener(for appName: String, uid: String) {
        guard let app = FirebaseApp.app(name: appName) else { return }
        let db = Firestore.firestore(app: app)
        
        profilePhotoListeners[appName]?.remove()
        let listener = db.collection("users").document(uid).collection("settings").document("profile")
            .addSnapshotListener { [weak self] snapshot, _ in
                guard let self = self, let data = snapshot?.data() else { return }
                DispatchQueue.main.async {
                    if let index = self.profiles.firstIndex(where: { $0.appName == appName }) {
                        self.profiles[index].customPhotoBase64 = data["photoBase64"] as? String
                        self.profiles[index].ringColor = data["ringColor"] as? String
                    }
                }
            }
        profilePhotoListeners[appName] = listener
    }
    
    private func saveActiveApps() {
        let activeApps = profiles.map { $0.appName }
        // Ensure default is always tracked if it exists
        var appsToSave = activeApps
        if authStateListeners.keys.contains("[DEFAULT]") && !appsToSave.contains("[DEFAULT]") {
            if Auth.auth().currentUser != nil {
               appsToSave.append("[DEFAULT]")
            }
        }
        UserDefaults.standard.set(appsToSave, forKey: "activeAppNames")
    }
    
    func addProfile() -> Auth {
        let newAppName = "profile_\(UUID().uuidString)"
        if let options = FirebaseApp.app()?.options {
            FirebaseApp.configure(name: newAppName, options: options)
            setupListener(for: newAppName)
        }
        return Auth.auth(app: FirebaseApp.app(name: newAppName)!)
    }
    
    func logout(appName: String) {
        guard let app = FirebaseApp.app(name: appName) else { return }
        let auth = Auth.auth(app: app)
        do {
            try auth.signOut()
            
            // For dynamic profiles, clean up the listener. For [DEFAULT], keep listening so we can log back in.
            if appName != "[DEFAULT]" {
                if let handle = authStateListeners[appName] {
                    auth.removeStateDidChangeListener(handle)
                    authStateListeners.removeValue(forKey: appName)
                }
            }
            
            profiles.removeAll(where: { $0.appName == appName })
            saveActiveApps()
        } catch {
            print("Error signing out of \(appName): \(error)")
        }
    }
    
    func getFirestore(for appName: String) -> Firestore? {
        guard let app = FirebaseApp.app(name: appName) else { return nil }
        return Firestore.firestore(app: app)
    }
}
