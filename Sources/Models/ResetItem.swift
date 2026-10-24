import Foundation
import SwiftData
import SwiftUI

@Model
final class ResetItem {
    var id: UUID
    var name: String
    var resetTime: Date
    var hexColor: String
    
    // Base64 encoded image or simple local identifier
    var imageData: Data?
    var orderIndex: Int?
    var profileId: String?
    var url: String?
    
    init(id: UUID = UUID(), name: String, resetTime: Date, hexColor: String, imageData: Data? = nil, orderIndex: Int? = 0, profileId: String? = nil, url: String? = nil) {
        self.id = id
        self.name = name
        self.resetTime = resetTime
        self.hexColor = hexColor
        self.imageData = imageData
        self.orderIndex = orderIndex
        self.profileId = profileId
        self.url = url
    }
    
    var color: Color {
        Color(hex: hexColor) ?? .blue
    }
}
