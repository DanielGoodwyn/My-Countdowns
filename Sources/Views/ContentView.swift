import SwiftUI
import SwiftData
import FirebaseAuth
import PhotosUI

enum SortOption: String, CaseIterable, Identifiable {
    case soonestAsc = "Soonest First"
    case soonestDesc = "Latest First"
    case alphabetical = "Alphabetical"
    case custom = "Custom Order"
    var id: String { self.rawValue }
}

struct ContentView: View {
    @Environment(\.modelContext) private var modelContext
    @Query private var items: [ResetItem] // Unsorted query, we sort in memory
    @StateObject private var profileManager = MultiProfileManager.shared
    
    @State private var isShowingAdd = false
    @State private var itemToEdit: ResetItem?
    @State private var sortOption: SortOption = .soonestAsc
    @State private var isEditingList = false // To enable EditButton
    @State private var isAddingProfile = false
    @State private var newAuthInstance: FirebaseAuth.Auth?
    @State private var isShowingPhotoPicker = false
    @State private var profileToChangePhoto: String?
    @State private var photosSelection: PhotosUI.PhotosPickerItem? = nil
    @State private var isShowingColorPicker = false
    @State private var profileToChangeColor: String? = nil
    @State private var tempColor: Color = .black

    var sortedItems: [ResetItem] {
        switch sortOption {
        case .soonestAsc:
            return items.sorted { $0.resetTime < $1.resetTime }
        case .soonestDesc:
            return items.sorted { $0.resetTime > $1.resetTime }
        case .alphabetical:
            return items.sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
        case .custom:
            return items.sorted { ($0.orderIndex ?? 0) < ($1.orderIndex ?? 0) }
        }
    }

    @ViewBuilder
    private func profileRingOverlay(for profile: UserProfile) -> some View {
        if let hex = profile.ringColor, let color = Color(hex: hex) {
            Circle().stroke(color, lineWidth: 2)
        } else {
            Color.clear
        }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Color(UIColor.systemGroupedBackground).ignoresSafeArea()
                
                VStack(spacing: 0) {
                    Picker("Sort By", selection: $sortOption) {
                        Text("Soonest").tag(SortOption.soonestAsc)
                        Text("Latest").tag(SortOption.soonestDesc)
                        Text("A-Z").tag(SortOption.alphabetical)
                        Text("Custom").tag(SortOption.custom)
                    }
                    .pickerStyle(.segmented)
                    .padding(.horizontal)
                    .padding(.top, 8)
                    
                    if !profileManager.profiles.isEmpty {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack {
                                ForEach(profileManager.profiles) { profile in
                                    Menu {
                                        Button(action: {
                                            if let appName = profile.appName as String? {
                                                profileToChangePhoto = appName
                                                isShowingPhotoPicker = true
                                            }
                                        }) {
                                            Label("Change Photo", systemImage: "photo")
                                        }
                                        Button(action: {
                                            if let appName = profile.appName as String? {
                                                profileToChangeColor = appName
                                                if let hex = profile.ringColor, let c = Color(hex: hex) {
                                                    tempColor = c
                                                } else {
                                                    tempColor = .black
                                                }
                                                isShowingColorPicker = true
                                            }
                                        }) {
                                            Label("Change Color", systemImage: "paintbrush")
                                        }
                                        Button(role: .destructive, action: {
                                            if let appName = profile.appName as String? {
                                                MultiProfileManager.shared.logout(appName: appName)
                                            }
                                        }) {
                                            Label("Log Out", systemImage: "rectangle.portrait.and.arrow.right")
                                        }
                                    } label: {
                                        HStack(spacing: 6) {
                                            if let customPhoto = profile.customPhotoBase64, let data = Data(base64Encoded: customPhoto), let uiImage = UIImage(data: data) {
                                                Image(uiImage: uiImage)
                                                    .resizable()
                                                    .scaledToFill()
                                                    .frame(width: 20, height: 20)
                                                    .clipShape(Circle())
                                                    .overlay(profileRingOverlay(for: profile))
                                            } else if let photoURL = profile.photoURL {
                                                AsyncImage(url: photoURL) { image in
                                                    image.resizable()
                                                } placeholder: {
                                                    Circle().fill(Color.gray.opacity(0.3))
                                                }
                                                .frame(width: 20, height: 20)
                                                .clipShape(Circle())
                                                .overlay(profileRingOverlay(for: profile))
                                            } else {
                                                Circle()
                                                    .fill(Color.blue)
                                                    .frame(width: 20, height: 20)
                                                    .overlay(Text(String(profile.email.prefix(1)).uppercased()).font(.caption2).foregroundColor(.white))
                                                    .overlay(profileRingOverlay(for: profile))
                                            }
                                            
                                            Text(profile.email)
                                                .font(.caption)
                                                .foregroundColor(.primary)
                                        }
                                        .padding(.horizontal, 8)
                                        .padding(.vertical, 4)
                                        .background(Color(UIColor.tertiarySystemGroupedBackground))
                                        .cornerRadius(12)
                                    }
                                }
                                
                                Button(action: { isAddingProfile = true }) {
                                    HStack(spacing: 4) {
                                        Image(systemName: "plus")
                                            .font(.caption)
                                        Text("Add Account")
                                            .font(.caption)
                                    }
                                    .foregroundColor(.blue)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 4)
                                    .background(Color(UIColor.tertiarySystemGroupedBackground))
                                    .cornerRadius(12)
                                }
                            }
                            .padding(.horizontal)
                            .padding(.top, 8)
                        }
                    }
                    if profileManager.profiles.isEmpty {
                        VStack(spacing: 20) {
                            Spacer()
                            Image(systemName: "person.crop.circle.badge.exclamationmark")
                                .font(.system(size: 60))
                                .foregroundColor(.secondary)
                            Text("Not Logged In")
                                .font(.title2)
                                .fontWeight(.semibold)
                            Text("Please log in or create an account to save and manage your my countdownss.")
                                .multilineTextAlignment(.center)
                                .foregroundColor(.secondary)
                                .padding(.horizontal, 40)
                            Button(action: { isAddingProfile = true }) {
                                Text("Sign In / Sign Up")
                                    .fontWeight(.semibold)
                                    .foregroundColor(.white)
                                    .padding()
                                    .frame(maxWidth: .infinity)
                                    .background(Color.blue)
                                    .cornerRadius(10)
                            }
                            .padding(.horizontal, 40)
                            .padding(.top, 10)
                            Spacer()
                        }
                    } else {
                        List {
                            ForEach(sortedItems) { item in
                                ResetItemRow(item: item)
                                    .contentShape(Rectangle())
                                    .onTapGesture {
                                        itemToEdit = item
                                    }
                                    .swipeActions(edge: .trailing) {
                                        Button(role: .destructive) {
                                            deleteItem(item)
                                        } label: {
                                            Label("Delete", systemImage: "trash")
                                        }
                                    }
                                    .listRowInsets(EdgeInsets())
                                    .listRowSeparator(.hidden)
                                    .listRowBackground(Color.clear)
                            }
                            .onMove(perform: sortOption == .custom ? moveItems : nil)
                        }
                        .listStyle(.plain)
                        .background(Color(UIColor.secondarySystemGroupedBackground))
                        .clipShape(RoundedRectangle(cornerRadius: 15))
                        .padding()
                    }
                }
            }
            .navigationTitle("My Countdowns")
            .toolbar {
                if sortOption == .custom {
                    ToolbarItem(placement: .navigationBarLeading) {
                        EditButton()
                    }
                }

                ToolbarItem(placement: .navigationBarTrailing) {
                    if !profileManager.profiles.isEmpty {
                        Button(action: { isShowingAdd = true }) {
                            Label("Add Item", systemImage: "plus")
                        }
                    }
                }
            }
            .photosPicker(isPresented: $isShowingPhotoPicker, selection: $photosSelection, matching: .images)
            .onChange(of: photosSelection) { _, newItem in
                if let appName = profileToChangePhoto {
                    Task {
                        if let data = try? await newItem?.loadTransferable(type: Data.self) {
                            FirestoreManager.shared.updateProfilePhoto(data: data, profileId: appName)
                        }
                        profileToChangePhoto = nil
                    }
                }
            }
            .onChange(of: sortOption) { _, newValue in
                FirestoreManager.shared.updateSortOption(newValue.id)
            }
            .sheet(isPresented: $isShowingColorPicker) {
                NavigationStack {
                    Form {
                        ColorPicker("Profile Ring Color", selection: $tempColor, supportsOpacity: false)
                    }
                    .navigationTitle("Change Color")
                    .toolbar {
                        ToolbarItem(placement: .confirmationAction) {
                            Button("Save") {
                                if let appName = profileToChangeColor, let hex = tempColor.toHex() {
                                    FirestoreManager.shared.updateProfileRingColor(hex: hex, profileId: appName)
                                }
                                isShowingColorPicker = false
                            }
                        }
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Cancel") { isShowingColorPicker = false }
                        }
                    }
                }
                .presentationDetents([.height(250)])
            }
            .sheet(isPresented: $isAddingProfile) {
                if let auth = newAuthInstance {
                    LoginView(authInstance: auth) {
                        isAddingProfile = false
                    }
                    .onDisappear {
                        newAuthInstance = nil
                    }
                } else {
                    ProgressView().onAppear {
                        newAuthInstance = MultiProfileManager.shared.addProfile()
                    }
                }
            }
            .sheet(isPresented: $isShowingAdd) {
                AddResetView()
            }
            .sheet(item: $itemToEdit) { item in
                AddResetView(itemToEdit: item)
            }
            .onAppear {
                startAllListeners()
            }
            .onChange(of: profileManager.profiles.count) { _, _ in
                startAllListeners()
            }
        }
    }
    
    private func startAllListeners() {
        FirestoreManager.shared.startListening(modelContext: modelContext)
        FirestoreManager.shared.startListeningToSwatches(modelContext: modelContext)
        FirestoreManager.shared.startListeningToSettings { newSortString in
            if let newOption = SortOption.allCases.first(where: { $0.id == newSortString }), newOption != sortOption {
                sortOption = newOption
            }
        }
    }
    
    private func deleteItem(_ item: ResetItem) {
        NotificationManager.shared.cancelNotification(for: item)
        FirestoreManager.shared.deleteItem(item)
        modelContext.delete(item)
    }
    
    private func moveItems(from source: IndexSet, to destination: Int) {
        var itemsArray = sortedItems
        itemsArray.move(fromOffsets: source, toOffset: destination)
        
        for (index, item) in itemsArray.enumerated() {
            item.orderIndex = index
            FirestoreManager.shared.saveItem(item)
        }
    }
}

struct ResetItemRow: View {
    let item: ResetItem
    @State private var timeRemaining: String = ""
    
    // A timer to update the countdown every minute
    let timer = Timer.publish(every: 60, on: .main, in: .common).autoconnect()
    
    var body: some View {
        VStack(spacing: 0) {
            Rectangle()
                .fill(item.color)
                .frame(height: 4)
                
            HStack(spacing: 16) {
                if let data = item.imageData, let uiImage = UIImage(data: data) {
                    Image(uiImage: uiImage)
                        .resizable()
                        .scaledToFill()
                        .frame(width: 50, height: 50)
                        .background(Color.white)
                        .clipShape(Circle())
                        .overlay(Circle().stroke(item.color, lineWidth: 2))
                } else {
                    Circle()
                        .fill(item.color)
                        .frame(width: 50, height: 50)
                        .background(Color.white)
                        .clipShape(Circle())
                        .overlay(Text(String(item.name.prefix(1))).font(.title).foregroundColor(.white))
                        .overlay(Circle().stroke(item.color, lineWidth: 2))
                }
                
                VStack(alignment: .leading, spacing: 4) {
                    Text(item.name)
                        .font(.headline)
                    
                    Text(item.resetTime, style: .date)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        + Text(" at ")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        + Text(item.resetTime, style: .time)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    
                    Text(timeRemaining)
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundColor(item.resetTime > Date() ? .primary : .red)
                }
                Spacer()
            }
            .padding(.vertical, 12)
            .padding(.horizontal, 16)
            
            Rectangle()
                .fill(item.color)
                .frame(height: 4)
        }
        .background(item.color.opacity(0.2))
        .onAppear {
            updateTimeRemaining()
        }
        .onReceive(timer) { _ in
            updateTimeRemaining()
        }
    }
    
    private func updateTimeRemaining() {
        let now = Date()
        if now >= item.resetTime {
            timeRemaining = "Countdown complete!"
            return
        }
        
        let adjustedResetTime = item.resetTime.addingTimeInterval(59)
        let components = Calendar.current.dateComponents([.day, .hour, .minute], from: now, to: adjustedResetTime)
        var parts: [String] = []
        if let d = components.day, d > 0 { parts.append("\(d)d") }
        if let h = components.hour, h > 0 { parts.append("\(h)h") }
        if let m = components.minute, m >= 0 { parts.append("\(m)m") } // show 0m if less than an hour
        
        timeRemaining = parts.joined(separator: " ") + " left"
    }
}
