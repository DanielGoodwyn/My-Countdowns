// One-time: copy data from old project (usage-reset-countdowns) into new project's
// legacy/{email}/... staging area. The web app claims it on first sign-in.
import { initializeApp } from "firebase/app";
import { getFirestore, collectionGroup, getDocs, doc, setDoc } from "firebase/firestore";

const oldApp = initializeApp({
  apiKey: "AIzaSyDyKzxMRGHtwLDhJ3kAOpS_uAj19SLQWGw",
  authDomain: "usage-reset-countdowns.firebaseapp.com",
  projectId: "usage-reset-countdowns",
  appId: "1:1084800302781:web:ec638912e6439a066d81d4",
}, "old");
const newApp = initializeApp({
  apiKey: "AIzaSyCGYYohQ6K92WNcOe-QjMymAp9w5flS7gY",
  authDomain: "my-countdowns-1e20e.firebaseapp.com",
  projectId: "my-countdowns-1e20e",
  appId: "1:77456450491:web:ca745ff9-e62f-4d60-bb19-e6f60724b05a",
}, "new");
const oldDb = getFirestore(oldApp);
const newDb = getFirestore(newApp);

// Old UID -> email (from old project's Authentication users list)
const uidToEmail = {
  WglWmwVHmPVY4t6bPLsEDmKw6rj2: "danielgoodwyn@gmail.com",
  t7NRiWcCW8ha1QGO4wtsViEKGQx1: "info@automatedbusinessprocesses.com",
};

let count = 0;
for (const group of ["resets", "swatches", "settings"]) {
  const snap = await getDocs(collectionGroup(oldDb, group));
  for (const d of snap.docs) {
    const [root, uid] = d.ref.path.split("/");
    const email = uidToEmail[uid];
    if (root !== "users" || !email) continue;
    await setDoc(doc(newDb, `legacy/${email}/${group}/${d.id}`), d.data());
    count++;
  }
}
await setDoc(doc(newDb, `legacy/danielgoodwyn@gmail.com`), { pending: true });
await setDoc(doc(newDb, `legacy/info@automatedbusinessprocesses.com`), { pending: true });
console.log("Copied", count, "docs into legacy staging");
process.exit(0);
