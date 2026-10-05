import Foundation
import UserNotifications

class NotificationManager {
    static let shared = NotificationManager()
    
    private init() {}
    
    func scheduleNotification(for item: ResetItem) {
        let content = UNMutableNotificationContent()
        content.title = "Reset Time Reached"
        content.body = "Your \(item.name) reset time has arrived!"
        content.sound = .default
        
        if let data = item.imageData {
            let tempDir = FileManager.default.temporaryDirectory
            let tempURL = tempDir.appendingPathComponent("\(item.id.uuidString).jpg")
            do {
                try data.write(to: tempURL)
                let attachment = try UNNotificationAttachment(identifier: item.id.uuidString, url: tempURL, options: nil)
                content.attachments = [attachment]
            } catch {
                print("Could not attach image: \(error)")
            }
        }
        
        // Remove old notification for this item if it exists
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [item.id.uuidString])
        
        // Only schedule if the date is in the future
        if item.resetTime > Date() {
            let components = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute], from: item.resetTime)
            let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
            
            let request = UNNotificationRequest(identifier: item.id.uuidString, content: content, trigger: trigger)
            UNUserNotificationCenter.current().add(request) { error in
                if let error = error {
                    print("Error scheduling notification: \(error)")
                }
            }
        }
    }
    
    func cancelNotification(for item: ResetItem) {
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [item.id.uuidString])
    }
}
