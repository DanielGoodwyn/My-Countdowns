import type { User } from 'firebase/auth';
import { collection, deleteDoc, doc, getDoc, getDocs, writeBatch, type Firestore } from 'firebase/firestore';

const GROUPS = ['resets', 'swatches', 'settings'] as const;
const inFlight = new Set<string>();

/**
 * One-time claim of data migrated from the old Firebase project.
 * Old data was staged under legacy/{email}/... and is moved into users/{uid}/...
 * on the user's first sign-in. Existing docs in users/{uid} are never overwritten.
 */
export async function claimLegacyData(db: Firestore, user: User): Promise<void> {
  const email = user.email?.toLowerCase();
  if (!email || inFlight.has(user.uid)) return;
  inFlight.add(user.uid);
  try {
    const marker = doc(db, `legacy/${email}`);
    if (!(await getDoc(marker)).exists()) return;

    for (const group of GROUPS) {
      const legacySnap = await getDocs(collection(db, `legacy/${email}/${group}`));
      if (legacySnap.empty) continue;
      const existing = await getDocs(collection(db, `users/${user.uid}/${group}`));
      const existingIds = new Set(existing.docs.map(d => d.id));

      const batch = writeBatch(db);
      legacySnap.docs.forEach(d => {
        if (!existingIds.has(d.id)) batch.set(doc(db, `users/${user.uid}/${group}/${d.id}`), d.data());
      });
      await batch.commit();
      await Promise.all(legacySnap.docs.map(d => deleteDoc(d.ref)));
    }
    await deleteDoc(marker);
  } catch (err) {
    console.warn('Legacy data claim failed', err);
  } finally {
    inFlight.delete(user.uid);
  }
}
