import { initializeApp } from "firebase/app";
import { getAuth, signInWithEmailAndPassword, createUserWithEmailAndPassword } from "firebase/auth";

const firebaseConfig = {
  apiKey: "AIzaSyDyKzxMRGHtwLDhJ3kAOpS_uAj19SLQWGw",
  authDomain: "usage-reset-countdowns.firebaseapp.com",
  projectId: "usage-reset-countdowns",
  storageBucket: "usage-reset-countdowns.firebasestorage.app",
  messagingSenderId: "1084800302781",
  appId: "1:1084800302781:web:ec638912e6439a066d81d4"
};

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);

async function testAuth() {
  try {
    console.log("Attempting to sign in...");
    await signInWithEmailAndPassword(auth, "danielgoodwyn@gmail.com", "HalfPlus7!");
    console.log("Sign in successful!");
  } catch (error) {
    console.error("Sign in failed:", error.code, error.message);
    if (error.code === 'auth/invalid-credential') {
      console.log("Attempting to create account instead...");
      try {
        await createUserWithEmailAndPassword(auth, "danielgoodwyn@gmail.com", "HalfPlus7!");
        console.log("Created account successfully!");
      } catch (createError) {
        console.error("Create account failed:", createError.code, createError.message);
      }
    }
  }
}

testAuth();
