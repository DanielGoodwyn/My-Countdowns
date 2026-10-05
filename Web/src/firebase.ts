import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";
import { getFirestore } from "firebase/firestore";

export const firebaseConfig = {
  apiKey: "AIzaSyCGYYohQ6K92WNcOe-QjMymAp9w5flS7gY",
  authDomain: "my-countdowns-1e20e.firebaseapp.com",
  projectId: "my-countdowns-1e20e",
  storageBucket: "my-countdowns-1e20e.firebasestorage.app",
  messagingSenderId: "77456450491",
  appId: "1:77456450491:web:ca745ff9-e62f-4d60-bb19-e6f60724b05a"
};

export const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app);
