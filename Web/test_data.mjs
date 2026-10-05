import { initializeApp } from "firebase/app";
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
const db = getFirestore(app);

async function findData() {
  try {
    const resetsRef = collectionGroup(db, 'resets');
    const querySnapshot = await getDocs(resetsRef);
    console.log(`Total items found globally: ${querySnapshot.docs.length}`);
  } catch (error) {
    console.error("Error:", error.message);
  }
}
findData();
