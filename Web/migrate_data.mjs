import { initializeApp } from "firebase/app";
import { getAuth, signInWithEmailAndPassword } from "firebase/auth";
import { getFirestore, collection, getDocs, doc, setDoc } from "firebase/firestore";

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

async function migrateData() {
  try {
    // 1. Sign in to get access
    await signInWithEmailAndPassword(auth, "danielgoodwyn@gmail.com", "HalfPlus7!");
    console.log("Logged in!");

    const oldUID = "ZgsmV4OxqsPWnineqFt8hTNjUmL2"; // Google UID with data
    const newUID = "WglWmwVHmPVY4t6bPLsEDmKw6rj2"; // Email UID without data

    // 2. Fetch resets from old UID
    const oldResetsRef = collection(db, `users/${oldUID}/resets`);
    const oldResetsSnap = await getDocs(oldResetsRef);
    
    console.log(`Found ${oldResetsSnap.docs.length} items to migrate.`);

    // 3. Copy to new UID
    for (const docSnap of oldResetsSnap.docs) {
      const data = docSnap.data();
      const newDocRef = doc(db, `users/${newUID}/resets`, docSnap.id);
      await setDoc(newDocRef, data);
      console.log(`Copied item: ${data.name}`);
    }

    // 4. Also copy swatches if they exist
    const oldSwatchesRef = collection(db, `users/${oldUID}/swatches`);
    const oldSwatchesSnap = await getDocs(oldSwatchesRef);
    for (const docSnap of oldSwatchesSnap.docs) {
      const data = docSnap.data();
      const newDocRef = doc(db, `users/${newUID}/swatches`, docSnap.id);
      await setDoc(newDocRef, data);
      console.log(`Copied swatch: ${data.name}`);
    }

    console.log("Migration complete!");
  } catch (err) {
    console.error("Error:", err.message);
  }
}

migrateData();
