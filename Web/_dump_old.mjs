// Temporary: dump all data from the old Firebase project (usage-reset-countdowns) to JSON.
import { initializeApp } from "firebase/app";
import { getFirestore, collectionGroup, getDocs } from "firebase/firestore";
import { writeFileSync } from "fs";

const app = initializeApp({
  apiKey: "AIzaSyDyKzxMRGHtwLDhJ3kAOpS_uAj19SLQWGw",
  authDomain: "usage-reset-countdowns.firebaseapp.com",
  projectId: "usage-reset-countdowns",
  appId: "1:1084800302781:web:ec638912e6439a066d81d4",
});
const db = getFirestore(app);

const out = {};
for (const group of ["resets", "swatches", "settings"]) {
  try {
    const snap = await getDocs(collectionGroup(db, group));
    for (const d of snap.docs) {
      const parts = d.ref.path.split("/"); // users/{uid}/{group}/{id}
      if (parts[0] !== "users") continue;
      const uid = parts[1];
      out[uid] ??= { resets: {}, swatches: {}, settings: {} };
      out[uid][group][d.id] = d.data();
    }
    console.log(group, snap.size);
  } catch (e) {
    console.error(group, "ERROR", e.message);
  }
}
writeFileSync(process.argv[2], JSON.stringify(out, (k, v) => (v && typeof v.toDate === "function" ? { __ts: v.toDate().toISOString() } : v), 2));
for (const [uid, v] of Object.entries(out)) {
  console.log(uid, "resets:", Object.keys(v.resets).length, "swatches:", Object.keys(v.swatches).length, "settings:", Object.keys(v.settings), "email:", v.settings.profile?.email);
}
process.exit(0);
