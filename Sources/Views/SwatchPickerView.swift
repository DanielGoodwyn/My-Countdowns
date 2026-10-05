import SwiftUI
import SwiftData

struct SwatchPickerView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \ColorSwatch.name) private var swatches: [ColorSwatch]
    
    @Binding var selectedColor: Color
    var currentProfileId: String
    
    @State private var showingEditSwatch = false
    @State private var editingSwatchName = ""
    @State private var editingSwatchColor = Color.blue
    @State private var editingSwatchProfiles = Set<String>()
    @State private var editingSwatchOriginal: ColorSwatch?
    
    init(selectedColor: Binding<Color>, currentProfileId: String) {
        self._selectedColor = selectedColor
        self.currentProfileId = currentProfileId
    }
    
    var activeAppNames: [String] {
        MultiProfileManager.shared.profiles.map { $0.appName }
    }
    
    var filteredSwatches: [ColorSwatch] {
        swatches.filter { activeAppNames.contains($0.profileId ?? "[DEFAULT]") }
    }
    
    struct UniqueSwatch: Identifiable {
        let id = UUID()
        let swatch: ColorSwatch
        let emails: [String]
    }
    
    var uniqueSwatches: [UniqueSwatch] {
        var dict: [String: UniqueSwatch] = [:]
        for swatch in filteredSwatches {
            let key = "\(swatch.name)-\(swatch.hexColor.lowercased())"
            let email = MultiProfileManager.shared.profiles.first(where: { $0.appName == (swatch.profileId ?? "[DEFAULT]") })?.email ?? ""
            if let existing = dict[key] {
                if !email.isEmpty && !existing.emails.contains(email) {
                    dict[key] = UniqueSwatch(swatch: existing.swatch, emails: existing.emails + [email])
                }
            } else {
                dict[key] = UniqueSwatch(swatch: swatch, emails: email.isEmpty ? [] : [email])
            }
        }
        return dict.values.sorted { $0.swatch.name < $1.swatch.name }
    }
    
    var body: some View {
        List {
            Section(header: Text("Saved Swatches")) {
                if filteredSwatches.isEmpty {
                    Text("No swatches saved yet.")
                        .foregroundColor(.secondary)
                } else {
                    ForEach(uniqueSwatches) { uniqueItem in
                        let swatch = uniqueItem.swatch
                        HStack {
                            Circle()
                                .fill(swatch.color)
                                .frame(width: 30, height: 30)
                            VStack(alignment: .leading) {
                                Text(swatch.name)
                                if !uniqueItem.emails.isEmpty {
                                    Text(uniqueItem.emails.joined(separator: ", "))
                                        .font(.caption2)
                                        .foregroundColor(.secondary)
                                }
                            }
                            Spacer()
                            if selectedColor.toHex() == swatch.color.toHex() {
                                Image(systemName: "checkmark")
                                    .foregroundColor(.blue)
                            }
                        }
                        .contentShape(Rectangle())
                        .onTapGesture {
                            selectedColor = swatch.color
                        }
                        .onLongPressGesture {
                            editingSwatchOriginal = swatch
                            editingSwatchName = swatch.name
                            editingSwatchColor = swatch.color
                            
                            var matchedProfiles = Set<String>()
                            for s in swatches where s.name == swatch.name && s.hexColor.lowercased() == swatch.hexColor.lowercased() {
                                if let pid = s.profileId { matchedProfiles.insert(pid) }
                            }
                            if matchedProfiles.isEmpty { matchedProfiles.insert(swatch.profileId ?? currentProfileId) }
                            
                            editingSwatchProfiles = matchedProfiles
                            showingEditSwatch = true
                        }
                        .swipeActions(edge: .trailing) {
                            Button(role: .destructive) {
                                deleteSwatch(swatch)
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle("Swatches")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { 
                    editingSwatchOriginal = nil
                    editingSwatchName = ""
                    editingSwatchColor = selectedColor
                    editingSwatchProfiles = [currentProfileId]
                    showingEditSwatch = true 
                }) {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showingEditSwatch) {
            NavigationStack {
                Form {
                    Section {
                        TextField("Swatch Name", text: $editingSwatchName)
                        ColorPicker("Color", selection: $editingSwatchColor, supportsOpacity: false)
                        
                        if MultiProfileManager.shared.profiles.count > 1 {
                            Section(header: Text("Apply to Accounts")) {
                                ForEach(MultiProfileManager.shared.profiles) { profile in
                                    Toggle(profile.email, isOn: Binding(
                                        get: { editingSwatchProfiles.contains(profile.appName) },
                                        set: { isOn in
                                            if isOn { editingSwatchProfiles.insert(profile.appName) }
                                            else { editingSwatchProfiles.remove(profile.appName) }
                                        }
                                    ))
                                }
                            }
                        }
                    }
                    
                    if editingSwatchOriginal != nil {
                        Section {
                            Button("Delete Swatch", role: .destructive) {
                                deleteEditingSwatch()
                                showingEditSwatch = false
                            }
                        }
                    }
                }
                .navigationTitle(editingSwatchOriginal == nil ? "New Swatch" : "Edit Swatch")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel") {
                            showingEditSwatch = false
                        }
                    }
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Save") {
                            saveSwatch()
                            showingEditSwatch = false
                        }
                        .disabled(editingSwatchName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || editingSwatchProfiles.isEmpty)
                    }
                }
            }
            .presentationDetents([.medium])
        }
    }
    
    private func saveSwatch() {
        let hex = editingSwatchColor.toHex() ?? "0000FF"
        let name = editingSwatchName.trimmingCharacters(in: .whitespacesAndNewlines)
        
        // Delete old matching swatches if editing
        if let original = editingSwatchOriginal {
            let matches = swatches.filter { $0.name == original.name && $0.hexColor.lowercased() == original.hexColor.lowercased() }
            for match in matches {
                FirestoreManager.shared.deleteSwatch(match)
                modelContext.delete(match)
            }
        }
        
        // Create new swatches
        for pid in editingSwatchProfiles {
            let newSwatch = ColorSwatch(name: name, hexColor: hex, profileId: pid)
            modelContext.insert(newSwatch)
            FirestoreManager.shared.saveSwatch(newSwatch)
        }
    }
    
    private func deleteEditingSwatch() {
        if let original = editingSwatchOriginal {
            deleteSwatch(original)
        }
    }
    
    private func deleteSwatch(_ original: ColorSwatch) {
        let matches = swatches.filter { $0.name == original.name && $0.hexColor.lowercased() == original.hexColor.lowercased() }
        for match in matches {
            FirestoreManager.shared.deleteSwatch(match)
            modelContext.delete(match)
        }
    }

}
