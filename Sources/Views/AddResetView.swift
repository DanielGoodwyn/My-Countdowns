import SwiftUI
import SwiftData
import PhotosUI

struct AddResetView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    
    var itemToEdit: ResetItem?
    
    @State private var name: String
    @State private var selectedColor: Color
    @State private var selectedProfileId: String
    
    @AppStorage("preferRelativeTime") private var isRelativeTime: Bool = false
    @State private var selectedDate: Date
    @State private var relativeDays: Int = 1
    @State private var relativeHours: Int = 0
    @State private var relativeMinutes: Int = 0
    
    @State private var photosSelection: PhotosPickerItem? = nil
    @State private var imageData: Data?
    
    init(itemToEdit: ResetItem? = nil) {
        self.itemToEdit = itemToEdit
        _name = State(initialValue: itemToEdit?.name ?? "")
        _selectedColor = State(initialValue: itemToEdit?.color ?? .blue)
        let defaultTime = itemToEdit?.resetTime ?? Date().addingTimeInterval(86400)
        _selectedDate = State(initialValue: defaultTime)
        _imageData = State(initialValue: itemToEdit?.imageData)
        _selectedProfileId = State(initialValue: itemToEdit?.profileId ?? MultiProfileManager.shared.profiles.first?.appName ?? "[DEFAULT]")
        
        if let item = itemToEdit {
            let diff = item.resetTime.timeIntervalSinceNow + 59
            if diff > 0 {
                _relativeDays = State(initialValue: Int(diff) / 86400)
                _relativeHours = State(initialValue: (Int(diff) % 86400) / 3600)
                _relativeMinutes = State(initialValue: (Int(diff) % 3600) / 60)
            } else {
                _relativeDays = State(initialValue: 0)
                _relativeHours = State(initialValue: 0)
                _relativeMinutes = State(initialValue: 0)
            }
        }
    }
    
    var body: some View {
        NavigationStack {
            Form {
                Section(header: Text("Details")) {
                    TextField("Countdown Label", text: $name)
                    
                    if MultiProfileManager.shared.profiles.count > 1 {
                        Picker("Save to Account", selection: $selectedProfileId) {
                            ForEach(MultiProfileManager.shared.profiles) { profile in
                                Text(profile.email).tag(profile.appName)
                            }
                        }
                    }
                    
                    HStack {
                        Text("Color")
                        Spacer()
                        ColorPicker("", selection: $selectedColor, supportsOpacity: false)
                            .labelsHidden()
                        
                        NavigationLink(destination: SwatchPickerView(selectedColor: $selectedColor, currentProfileId: selectedProfileId)) {
                            Text("Swatches")
                                .font(.caption)
                                .foregroundColor(.blue)
                        }
                    }
                }
                
                Section(header: Text("Reset Time")) {
                    Picker("Time Mode", selection: $isRelativeTime) {
                        Text("Exact Date/Time").tag(false)
                        Text("Duration Left").tag(true)
                    }
                    .pickerStyle(.segmented)
                    
                    if isRelativeTime {
                        HStack {
                            VStack {
                                Text("Days").font(.caption).foregroundColor(.secondary)
                                Picker("Days", selection: $relativeDays) {
                                    ForEach(0..<30) { Text("\($0)").tag($0) }
                                }.pickerStyle(.wheel).frame(height: 100)
                            }
                            VStack {
                                Text("Hours").font(.caption).foregroundColor(.secondary)
                                Picker("Hours", selection: $relativeHours) {
                                    ForEach(0..<24) { Text("\($0)").tag($0) }
                                }.pickerStyle(.wheel).frame(height: 100)
                            }
                            VStack {
                                Text("Minutes").font(.caption).foregroundColor(.secondary)
                                Picker("Minutes", selection: $relativeMinutes) {
                                    ForEach(0..<60) { Text("\($0)").tag($0) }
                                }.pickerStyle(.wheel).frame(height: 100)
                            }
                        }
                    } else {
                        DatePicker("Target Time", selection: $selectedDate, displayedComponents: [.date, .hourAndMinute])
                            .datePickerStyle(.graphical)
                    }
                }
                
                Section(header: Text("Logo / Image")) {
                    PhotosPicker(selection: $photosSelection, matching: .images, photoLibrary: .shared()) {
                        HStack {
                            Text("Select Logo")
                            Spacer()
                            if let imageData = imageData, let uiImage = UIImage(data: imageData) {
                                Image(uiImage: uiImage)
                                    .resizable()
                                    .scaledToFill()
                                    .frame(width: 40, height: 40)
                                    .clipShape(RoundedRectangle(cornerRadius: 8))
                            } else {
                                Image(systemName: "photo")
                                    .foregroundColor(.gray)
                            }
                        }
                    }
                    .onChange(of: photosSelection) { oldItem, newItem in
                        Task {
                            if let data = try? await newItem?.loadTransferable(type: Data.self) {
                                imageData = data
                            }
                        }
                    }
                    
                    if imageData != nil {
                        Button("Remove Logo", role: .destructive) {
                            imageData = nil
                            photosSelection = nil
                        }
                    }
                }
            }
            .navigationTitle(itemToEdit == nil ? "New Reset" : "Edit Reset")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        save()
                    }
                    .disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
    }
    
    private func save() {
        let hexColor = selectedColor.toHex() ?? "0000FF"
        
        let finalDate: Date
        if isRelativeTime {
            let totalSeconds = (relativeDays * 86400) + (relativeHours * 3600) + (relativeMinutes * 60)
            finalDate = Date().addingTimeInterval(TimeInterval(totalSeconds))
        } else {
            finalDate = selectedDate
        }
        
        if let item = itemToEdit {
            let oldProfileId = item.profileId
            
            if let oldId = oldProfileId, oldId != selectedProfileId {
                // Delete from old profile's Firestore before modifying the profileId
                FirestoreManager.shared.deleteItem(item)
            }
            
            item.name = name
            item.hexColor = hexColor
            item.resetTime = finalDate
            item.imageData = imageData
            item.profileId = selectedProfileId
            
            NotificationManager.shared.scheduleNotification(for: item)
            FirestoreManager.shared.saveItem(item)
        } else {
            let newItem = ResetItem(name: name, resetTime: finalDate, hexColor: hexColor, imageData: imageData, profileId: selectedProfileId)
            modelContext.insert(newItem)
            NotificationManager.shared.scheduleNotification(for: newItem)
            FirestoreManager.shared.saveItem(newItem)
        }
        
        dismiss()
    }
}
