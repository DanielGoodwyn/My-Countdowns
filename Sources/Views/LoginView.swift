import SwiftUI
import FirebaseAuth
import SwiftData
import GoogleSignIn

struct LoginView: View {
    @Environment(\.modelContext) private var modelContext
    @ObservedObject private var profileManager = MultiProfileManager.shared
    var authInstance: Auth = Auth.auth()
    var onCancel: (() -> Void)? = nil
    
    @State private var email = ""
    @State private var password = ""
    @State private var isLogin = true
    @State private var errorMessage = ""
    @State private var message = ""
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                Text(onCancel != nil ? "Add Profile" : "My Countdowns")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .padding(.bottom, 20)
                
                if let currentUser = authInstance.currentUser {
                    VStack(spacing: 8) {
                        Text("Currently logged in as:")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                        
                        HStack {
                            if let photoURL = currentUser.photoURL {
                                AsyncImage(url: photoURL) { image in
                                    image.resizable()
                                } placeholder: {
                                    Circle().fill(Color.gray.opacity(0.3))
                                }
                                .frame(width: 40, height: 40)
                                .clipShape(Circle())
                            } else {
                                Circle()
                                    .fill(Color.blue)
                                    .frame(width: 40, height: 40)
                                    .overlay(Text(String(currentUser.email?.prefix(1) ?? "?").uppercased()).foregroundColor(.white))
                            }
                            Text(currentUser.email ?? "Unknown")
                                .font(.headline)
                                .foregroundColor(.green)
                        }
                        
                        if let onCancel = onCancel {
                            Button("Continue to Dashboard") {
                                onCancel()
                            }
                            .buttonStyle(.borderedProminent)
                            .padding(.top, 4)
                        } else {
                            // Plan B: Explicit navigation button requested by user
                            NavigationLink(destination: ContentView().navigationBarBackButtonHidden(true)) {
                                Text("Go to Dashboard")
                            }
                            .buttonStyle(.borderedProminent)
                            .padding(.top, 4)
                        }
                        
                        Button(role: .destructive) {
                            do {
                                try authInstance.signOut()
                                MultiProfileManager.shared.logout(appName: authInstance.app?.name ?? "[DEFAULT]")
                            } catch {
                                print("Error signing out: \(error)")
                            }
                        } label: {
                            Text("Log Out")
                                .font(.subheadline)
                        }
                        .padding(.top, 4)
                    }
                    .padding(.bottom, 20)
                } else {
                    Text("Not logged in")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .padding(.bottom, 20)
                }
                
                TextField("Email", text: $email)
                    .keyboardType(.emailAddress)
                    .autocapitalization(.none)
                    .disableAutocorrection(true)
                    .padding()
                    .background(Color(UIColor.secondarySystemBackground))
                    .cornerRadius(10)
                
                SecureField("Password", text: $password)
                    .padding()
                    .background(Color(UIColor.secondarySystemBackground))
                    .cornerRadius(10)
                
                if !errorMessage.isEmpty {
                    Text(errorMessage)
                        .foregroundColor(.red)
                        .font(.caption)
                }
                if !message.isEmpty {
                    Text(message)
                        .foregroundColor(.green)
                        .font(.caption)
                }
                
                Button(action: handleAuth) {
                    Text(isLogin ? "Sign In" : "Create Account")
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.blue)
                        .cornerRadius(10)
                }
                .padding(.top, 10)
                
                if isLogin {
                    Button(action: handleResetPassword) {
                        Text("Forgot my password")
                            .font(.footnote)
                            .foregroundColor(.blue)
                    }
                    .padding(.top, 5)
                }
                
                Button(action: { isLogin.toggle() }) {
                    Text(isLogin ? "Don't have an account? Sign up" : "Already have an account? Sign in")
                        .font(.footnote)
                        .foregroundColor(.blue)
                }
                
                Divider().padding(.vertical)
                
                Button(action: handleGoogleSignIn) {
                    HStack {
                        Image(systemName: "g.circle.fill")
                        Text("Continue with Google")
                    }
                    .foregroundColor(.black)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.white)
                    .cornerRadius(10)
                    .shadow(color: Color.black.opacity(0.1), radius: 3, x: 0, y: 1)
                }
            }
            .padding()
            .toolbar {
                if let onCancel = onCancel {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button("Cancel") {
                            onCancel()
                        }
                    }
                }
            }
        }
    }
    
    private func handleAuth() {
        errorMessage = ""
        message = ""
        
        let trimmedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
        
        guard !trimmedEmail.isEmpty, !password.isEmpty else {
            errorMessage = "Please enter both email and password."
            return
        }
        
        if isLogin {
            authInstance.signIn(withEmail: trimmedEmail, password: password) { result, error in
                if let error = error {
                    self.errorMessage = error.localizedDescription
                } else {
                    self.onSuccessfulLogin()
                }
            }
        } else {
            authInstance.createUser(withEmail: trimmedEmail, password: password) { result, error in
                if let error = error {
                    self.errorMessage = error.localizedDescription
                } else {
                    self.onSuccessfulLogin()
                }
            }
        }
    }
    
    private func handleResetPassword() {
        guard !email.isEmpty else {
            errorMessage = "Please enter your email first to reset your password."
            message = ""
            return
        }
        
        authInstance.sendPasswordReset(withEmail: email) { error in
            if let error = error {
                self.errorMessage = error.localizedDescription
                self.message = ""
            } else {
                self.message = "Password reset email sent! Check your inbox."
                self.errorMessage = ""
            }
        }
    }
    
    private func handleGoogleSignIn() {
        // Google Sign-In requires a client ID and presenting view controller.
        // It is omitted from this simple preview, but will be wired up via GIDSignIn.
        // Since we are using SwiftUI, we can wrap the UIKit presentation or just alert the user.
        // Let's implement it.
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let window = windowScene.windows.first,
              let rootViewController = window.rootViewController else {
            return
        }
        
        guard let clientID = authInstance.app?.options.clientID else {
            self.errorMessage = "Google Sign-In is not fully configured. Please redownload GoogleService-Info.plist."
            return
        }
        
        let serverClientID = "77456450491-aheoi1qj740dadhtjn5g43gpkg57scbe.apps.googleusercontent.com"
        let config = GIDConfiguration(clientID: clientID, serverClientID: serverClientID)
        GIDSignIn.sharedInstance.configuration = config
        
        GIDSignIn.sharedInstance.signIn(withPresenting: rootViewController) { signInResult, error in
            if let error = error {
                self.errorMessage = error.localizedDescription
                return
            }
            
            guard let user = signInResult?.user,
                  let idToken = user.idToken?.tokenString else {
                self.errorMessage = "Failed to get Google idToken"
                return
            }
            
            let credential = GoogleAuthProvider.credential(withIDToken: idToken,
                                                           accessToken: user.accessToken.tokenString)
            
            authInstance.signIn(with: credential) { result, error in
                if let error = error {
                    self.errorMessage = error.localizedDescription
                } else {
                    self.onSuccessfulLogin()
                }
            }
        }
    }
    
    private func onSuccessfulLogin() {
        if let currentUser = authInstance.currentUser {
            // Check if this user is ALREADY logged in on a DIFFERENT profile
            let isDuplicate = profileManager.profiles.contains { profile in
                return profile.uid == currentUser.uid && profile.appName != (authInstance.app?.name ?? "[DEFAULT]")
            }
            
            if isDuplicate {
                // Instantly sign out of the duplicate session
                try? authInstance.signOut()
                self.errorMessage = "This email is already logged in as another active account! You can only have one instance per email."
                self.isLogin = true // Force UI update
                return
            }
        }
        
        // Force a live UI update of the current view immediately
        self.message = "Successfully logged in!"
        
        if let onCancel = onCancel {
            onCancel()
        } else {
            let descriptor = FetchDescriptor<ResetItem>()
            if let localItems = try? modelContext.fetch(descriptor) {
                FirestoreManager.shared.pushLocalItemsToFirestore(localItems)
            }
            
            let swatchDescriptor = FetchDescriptor<ColorSwatch>()
            if let localSwatches = try? modelContext.fetch(swatchDescriptor) {
                FirestoreManager.shared.pushLocalSwatchesToFirestore(localSwatches)
            }
        }
    }
}
