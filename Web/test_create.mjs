import { initializeApp } from "firebase/app";
import { getAuth, createUserWithEmailAndPassword } from "firebase/auth";

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

async function test() {
  try {
    const cred = await createUserWithEmailAndPassword(auth, "danielgoodwyn@gmail.com", "HalfPlus7!");
    console.log("Created successfully! UID:", cred.user.uid);
  } catch (error) {
    console.error("Create error:", error.code, error.message);
  }
}
test();
