import { initializeApp } from "firebase/app";
import { getAuth, signInWithEmailAndPassword } from "firebase/auth";
import { getFirestore, collectionGroup, getDocs } from "firebase/firestore";

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
const db = getFirestore(app);

async function findData() {
  try {
    await signInWithEmailAndPassword(auth, "danielgoodwyn@gmail.com", "HalfPlus7!");
    console.log("Logged in!");
    
    // Attempt to read the entire 'resets' collection group
    const resetsSnap = await getDocs(collectionGroup(db, "resets"));
    console.log(`Found ${resetsSnap.docs.length} reset items across all users!`);
    for (const doc of resetsSnap.docs) {
      console.log(`- ${doc.data().name} (Path: ${doc.ref.path})`);
    }
  } catch (err) {
    console.error("Error:", err.message);
  }
}

findData();
