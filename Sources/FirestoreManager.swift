import Foundation
import FirebaseFirestore
import FirebaseAuth
import SwiftUI
import SwiftData
import FirebaseCore

class FirestoreManager {
    static let shared = FirestoreManager()
    
    private var listenerRegistrations: [String: ListenerRegistration] = [:]
    
    private init() {}
    
    // Push all local items to Firestore (useful for first-time login / migration)
    func pushLocalItemsToFirestore(_ items: [ResetItem], targetProfileId: String? = nil) {
        let activeProfile = MultiProfileManager.shared.profiles.first { targetProfileId != nil ? $0.appName == targetProfileId : true }
        guard let p = activeProfile, let app = FirebaseApp.app(name: p.appName) else { return }
        
        let db = Firestore.firestore(app: app)
        let batch = db.batch()
        
        for item in items {
            let docRef = db.collection("users").document(p.uid).collection("resets").document(item.id.uuidString)
            var data: [String: Any] = [
                "name": item.name,
                "hexColor": item.hexColor,
                "resetTime": item.resetTime.timeIntervalSince1970 * 1000,
                "orderIndex": item.orderIndex ?? 0
            ]
            if let imgData = item.imageData {
                if let uiImage = UIImage(data: imgData), let compressed = uiImage.jpegData(compressionQuality: 0.5) {
                    data["imageDataBase64"] = compressed.base64EncodedString()
                } else {
                    data["imageDataBase64"] = imgData.base64EncodedString()
                }
            }
            batch.setData(data, forDocument: docRef, merge: true)
        }
        
        batch.commit { error in
            if let error = error {
                print("Error pushing local items to Firestore: \(error)")
            } else {
                print("Successfully pushed local items to Firestore")
            }
        }
    }
    
    // Keep local SwiftData in sync with Firestore
    func startListening(modelContext: ModelContext) {
        stopListening()
        
        let activeAppNames = MultiProfileManager.shared.profiles.map { $0.appName }
        
        // Clean up items from logged out profiles
        let itemDescriptor = FetchDescriptor<ResetItem>()
        if let localItems = try? modelContext.fetch(itemDescriptor) {
            for item in localItems {
                let pid = item.profileId ?? "[DEFAULT]"
                if !activeAppNames.contains(pid) {
                    NotificationManager.shared.cancelNotification(for: item)
                    modelContext.delete(item)
                }
            }
            try? modelContext.save()
        }
        
        for profile in MultiProfileManager.shared.profiles {
            guard let app = FirebaseApp.app(name: profile.appName) else { continue }
            let db = Firestore.firestore(app: app)
            
            let listener = db.collection("users").document(profile.uid).collection("resets")
                .addSnapshotListener { querySnapshot, error in
                    guard let snapshot = querySnapshot else { return }
                    
                    let descriptor = FetchDescriptor<ResetItem>()
                    guard let localItems = try? modelContext.fetch(descriptor) else { return }
                    
                    snapshot.documentChanges.forEach { change in
                        let data = change.document.data()
                        let idString = change.document.documentID
                        guard let id = UUID(uuidString: idString) else { return }
                        
                        let name = data["name"] as? String ?? ""
                        let hexColor = data["hexColor"] as? String ?? "#0000FF"
                        let resetTimeInterval = data["resetTime"] as? TimeInterval ?? Date().timeIntervalSince1970 * 1000
                        let resetTime = Date(timeIntervalSince1970: resetTimeInterval / 1000)
                        let orderIndex = data["orderIndex"] as? Int ?? 0
                        
                        var imageData: Data? = nil
                        if let base64String = data["imageDataBase64"] as? String {
                            imageData = Data(base64Encoded: base64String)
                        }
                        
                        if change.type == .added || change.type == .modified {
                            if let existing = localItems.first(where: { $0.id == id }) {
                                if existing.name != name || existing.resetTime != resetTime || existing.hexColor != hexColor || existing.orderIndex != orderIndex || existing.profileId != profile.appName {
                                    existing.name = name
                                    existing.resetTime = resetTime
                                    existing.hexColor = hexColor
                                    existing.orderIndex = orderIndex
                                    existing.imageData = imageData
                                    existing.profileId = profile.appName
                                    NotificationManager.shared.scheduleNotification(for: existing)
                                }
                            } else {
                                let newItem = ResetItem(id: id, name: name, resetTime: resetTime, hexColor: hexColor, imageData: imageData, orderIndex: orderIndex, profileId: profile.appName)
                                modelContext.insert(newItem)
                                NotificationManager.shared.scheduleNotification(for: newItem)
                            }
                        } else if change.type == .removed {
                            if let existing = localItems.first(where: { $0.id == id }) {
                                NotificationManager.shared.cancelNotification(for: existing)
                                modelContext.delete(existing)
                            }
                        }
                    }
                    try? modelContext.save()
                }
            listenerRegistrations[profile.appName] = listener
        }
    }
    
    func stopListening() {
        listenerRegistrations.values.forEach { $0.remove() }
        listenerRegistrations.removeAll()
    }
    
    func saveItem(_ item: ResetItem) {
        let profileId = item.profileId ?? MultiProfileManager.shared.profiles.first?.appName ?? "[DEFAULT]"
        guard let p = MultiProfileManager.shared.profiles.first(where: { $0.appName == profileId }), let app = FirebaseApp.app(name: p.appName) else { return }
        let db = Firestore.firestore(app: app)
        
        var data: [String: Any] = [
            "name": item.name,
            "hexColor": item.hexColor,
            "resetTime": item.resetTime.timeIntervalSince1970 * 1000,
            "orderIndex": item.orderIndex ?? 0
        ]
        
        if let imgData = item.imageData {
            if let uiImage = UIImage(data: imgData), let compressed = uiImage.jpegData(compressionQuality: 0.5) {
                data["imageDataBase64"] = compressed.base64EncodedString()
            } else {
                data["imageDataBase64"] = imgData.base64EncodedString()
            }
        }
        
        db.collection("users").document(p.uid).collection("resets").document(item.id.uuidString).setData(data, merge: true)
    }
    
    func deleteItem(_ item: ResetItem) {
        let profileId = item.profileId ?? MultiProfileManager.shared.profiles.first?.appName ?? "[DEFAULT]"
        guard let p = MultiProfileManager.shared.profiles.first(where: { $0.appName == profileId }), let app = FirebaseApp.app(name: p.appName) else { return }
        let db = Firestore.firestore(app: app)
        db.collection("users").document(p.uid).collection("resets").document(item.id.uuidString).delete()
    }
    
    // MARK: - Profile Syncing
    func updateProfilePhoto(data: Data, profileId: String) {
        guard let p = MultiProfileManager.shared.profiles.first(where: { $0.appName == profileId }), let app = FirebaseApp.app(name: p.appName) else { return }
        let db = Firestore.firestore(app: app)
        
        let base64String: String
        if let uiImage = UIImage(data: data), let compressed = uiImage.jpegData(compressionQuality: 0.5) {
            base64String = compressed.base64EncodedString()
        } else {
            base64String = data.base64EncodedString()
        }
        
        db.collection("users").document(p.uid).collection("settings").document("profile").setData(["photoBase64": base64String], merge: true)
    }
    
    func updateProfileRingColor(hex: String, profileId: String) {
        guard let p = MultiProfileManager.shared.profiles.first(where: { $0.appName == profileId }), let app = FirebaseApp.app(name: p.appName) else { return }
        let db = Firestore.firestore(app: app)
        db.collection("users").document(p.uid).collection("settings").document("profile").setData(["ringColor": hex], merge: true)
    }
    
    // MARK: - Swatch Syncing
    
    private var swatchRegistrations: [String: ListenerRegistration] = [:]
    
    func pushLocalSwatchesToFirestore(_ swatches: [ColorSwatch], targetProfileId: String? = nil) {
        let activeProfile = MultiProfileManager.shared.profiles.first { targetProfileId != nil ? $0.appName == targetProfileId : true }
        guard let p = activeProfile, let app = FirebaseApp.app(name: p.appName) else { return }
        
        let db = Firestore.firestore(app: app)
        let batch = db.batch()
        
        for swatch in swatches {
            let docRef = db.collection("users").document(p.uid).collection("swatches").document(swatch.id.uuidString)
            let data: [String: Any] = [
                "name": swatch.name,
                "hexColor": swatch.hexColor
            ]
            batch.setData(data, forDocument: docRef, merge: true)
        }
        
        batch.commit()
    }
    
    func startListeningToSwatches(modelContext: ModelContext) {
        stopListeningToSwatches()
        
        let activeAppNames = MultiProfileManager.shared.profiles.map { $0.appName }
        
        let swatchDescriptor = FetchDescriptor<ColorSwatch>()
        if let localSwatches = try? modelContext.fetch(swatchDescriptor) {
            for swatch in localSwatches {
                let pid = swatch.profileId ?? "[DEFAULT]"
                if !activeAppNames.contains(pid) {
                    modelContext.delete(swatch)
                }
            }
            try? modelContext.save()
        }
        
        for profile in MultiProfileManager.shared.profiles {
            guard let app = FirebaseApp.app(name: profile.appName) else { continue }
            let db = Firestore.firestore(app: app)
            
            let listener = db.collection("users").document(profile.uid).collection("swatches")
                .addSnapshotListener { querySnapshot, error in
                    guard let snapshot = querySnapshot else { return }
                    
                    let descriptor = FetchDescriptor<ColorSwatch>()
                    guard let localSwatches = try? modelContext.fetch(descriptor) else { return }
                    
                    snapshot.documentChanges.forEach { change in
                        let data = change.document.data()
                        let idString = change.document.documentID
                        guard let id = UUID(uuidString: idString) else { return }
                        
                        let name = data["name"] as? String ?? ""
                        let hexColor = data["hexColor"] as? String ?? "#0000FF"
                        
                        if change.type == .added || change.type == .modified {
                            if let existing = localSwatches.first(where: { $0.id == id }) {
                                if existing.name != name || existing.hexColor != hexColor || existing.profileId != profile.appName {
                                    existing.name = name
                                    existing.hexColor = hexColor
                                    existing.profileId = profile.appName
                                }
                            } else {
                                let newSwatch = ColorSwatch(id: id, name: name, hexColor: hexColor, profileId: profile.appName)
                                modelContext.insert(newSwatch)
                            }
                        } else if change.type == .removed {
                            if let existing = localSwatches.first(where: { $0.id == id }) {
                                modelContext.delete(existing)
                            }
                        }
                    }
                    try? modelContext.save()
                }
            swatchRegistrations[profile.appName] = listener
        }
    }
    
    func stopListeningToSwatches() {
        swatchRegistrations.values.forEach { $0.remove() }
        swatchRegistrations.removeAll()
    }
    
    func saveSwatch(_ swatch: ColorSwatch) {
        let profileId = swatch.profileId ?? MultiProfileManager.shared.profiles.first?.appName ?? "[DEFAULT]"
        guard let p = MultiProfileManager.shared.profiles.first(where: { $0.appName == profileId }), let app = FirebaseApp.app(name: p.appName) else { return }
        let db = Firestore.firestore(app: app)
        
        let data: [String: Any] = [
            "name": swatch.name,
            "hexColor": swatch.hexColor
        ]
        db.collection("users").document(p.uid).collection("swatches").document(swatch.id.uuidString).setData(data, merge: true)
    }
    
    func deleteSwatch(_ swatch: ColorSwatch) {
        let profileId = swatch.profileId ?? MultiProfileManager.shared.profiles.first?.appName ?? "[DEFAULT]"
        guard let p = MultiProfileManager.shared.profiles.first(where: { $0.appName == profileId }), let app = FirebaseApp.app(name: p.appName) else { return }
        let db = Firestore.firestore(app: app)
        db.collection("users").document(p.uid).collection("swatches").document(swatch.id.uuidString).delete()
    }
    
    // MARK: - Settings Syncing
    
    private var settingsRegistrations: [String: ListenerRegistration] = [:]
    
    func startListeningToSettings(onSortChange: @escaping (String) -> Void) {
        stopListeningToSettings()
        
        for profile in MultiProfileManager.shared.profiles {
            guard let app = FirebaseApp.app(name: profile.appName) else { continue }
            let db = Firestore.firestore(app: app)
            
            let listener = db.collection("users").document(profile.uid).collection("settings").document("preferences")
                .addSnapshotListener { snapshot, error in
                    guard let data = snapshot?.data(), let sortString = data["sortOption"] as? String else { return }
                    
                    var iosFormat = "Soonest First"
                    switch sortString {
                    case "soonestAsc": iosFormat = "Soonest First"
                    case "soonestDesc": iosFormat = "Latest First"
                    case "alphabetical": iosFormat = "Alphabetical"
                    case "custom": iosFormat = "Custom Order"
                    default: iosFormat = sortString
                    }
                    
                    DispatchQueue.main.async {
                        onSortChange(iosFormat)
                    }
                }
            settingsRegistrations[profile.appName] = listener
        }
    }
    
    func stopListeningToSettings() {
        settingsRegistrations.values.forEach { $0.remove() }
        settingsRegistrations.removeAll()
    }
    
    func updateSortOption(_ sortString: String) {
        var webFormat = "soonestAsc"
        switch sortString {
        case "Soonest First": webFormat = "soonestAsc"
        case "Latest First": webFormat = "soonestDesc"
        case "Alphabetical": webFormat = "alphabetical"
        case "Custom Order": webFormat = "custom"
        default: webFormat = sortString
        }
        
        for profile in MultiProfileManager.shared.profiles {
            guard let app = FirebaseApp.app(name: profile.appName) else { continue }
            let db = Firestore.firestore(app: app)
            db.collection("users").document(profile.uid).collection("settings").document("preferences").setData(["sortOption": webFormat], merge: true)
        }
    }
}
