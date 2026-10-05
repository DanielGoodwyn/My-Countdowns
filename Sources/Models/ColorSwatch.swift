import Foundation
import SwiftData
import SwiftUI

@Model
final class ColorSwatch {
    var id: UUID
    var name: String
    var hexColor: String
    var profileId: String?
    
    init(id: UUID = UUID(), name: String, hexColor: String, profileId: String? = nil) {
        self.id = id
        self.name = name
        self.hexColor = hexColor
        self.profileId = profileId
    }
    
    var color: Color {
        Color(hex: hexColor) ?? .blue
    }
}
