import { initializeApp } from "firebase/app";
import { getAuth, fetchSignInMethodsForEmail } from "firebase/auth";

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
    const methods1 = await fetchSignInMethodsForEmail(auth, "danielgoodwyn@gmail.com");
    console.log("danielgoodwyn@gmail.com providers:", methods1);
    
    const methods2 = await fetchSignInMethodsForEmail(auth, "info@AutomatedBusinessProcesses.com");
    console.log("info@... providers:", methods2);
  } catch (error) {
    console.error("Error:", error.message);
  }
}
test();
