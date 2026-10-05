import { useEffect, useState, createContext, useContext } from 'react';
import { auth as defaultAuth, db as defaultDb, firebaseConfig } from './firebase';
import { onAuthStateChanged, getAuth, type User, type Auth } from 'firebase/auth';
import { getFirestore, type Firestore } from 'firebase/firestore';
import { initializeApp, getApp } from 'firebase/app';
import AuthComponent from './Auth';
import Dashboard from './Dashboard';
import { claimLegacyData } from './migrateLegacy';

export interface Profile {
  id: string;
  auth: Auth;
  db: Firestore;
  user: User | null;
  customPhotoBase64?: string;
  ringColor?: string;
}

interface MultiProfileContextType {
  profiles: Profile[];
  addProfile: () => void;
  removeProfile: (id: string) => void;
  isAddingProfile: boolean;
  cancelAddProfile: () => void;
}

export const MultiProfileContext = createContext<MultiProfileContextType | null>(null);

export const useProfiles = () => {
  const context = useContext(MultiProfileContext);
  if (!context) throw new Error('useProfiles must be used within MultiProfileProvider');
  return context;
};

function App() {
  const [profiles, setProfiles] = useState<Profile[]>([
    { id: '[DEFAULT]', auth: defaultAuth, db: defaultDb, user: null }
  ]);
  const [loading, setLoading] = useState(true);
  const [isAddingProfile, setIsAddingProfile] = useState(false);
  const [addingProfileId, setAddingProfileId] = useState<string | null>(null);

  useEffect(() => {
    // Load saved extra profiles
    const savedProfiles = JSON.parse(localStorage.getItem('activeProfiles') || '[]');
    const loadedProfiles: Profile[] = [ { id: '[DEFAULT]', auth: defaultAuth, db: defaultDb, user: null } ];

    savedProfiles.forEach((pId: string) => {
      if (pId === '[DEFAULT]') return;
      let appInstance;
      try {
        appInstance = getApp(pId);
      } catch {
        appInstance = initializeApp(firebaseConfig, pId);
      }
      loadedProfiles.push({
        id: pId,
        auth: getAuth(appInstance),
        db: getFirestore(appInstance),
        user: null
      });
    });

    setProfiles(loadedProfiles);

    let loadedCount = 0;
    const unsubscribes = loadedProfiles.map(p => 
      onAuthStateChanged(p.auth, (user) => {
        setProfiles(prev => prev.map(prof => prof.id === p.id ? { ...prof, user } : prof));
        loadedCount++;
        if (loadedCount >= loadedProfiles.length) setLoading(false);
      })
    );

    // Listen to custom photos for all loaded profiles
    loadedProfiles.forEach(p => {
      onAuthStateChanged(p.auth, (user) => {
        if (user) {
          claimLegacyData(p.db, user);
          import('firebase/firestore').then(({ doc, onSnapshot }) => {
            const unsub = onSnapshot(doc(p.db, `users/${user.uid}/settings/profile`), (docSnap) => {
              if (docSnap.exists()) {
                const data = docSnap.data();
                setProfiles(prev => prev.map(prof => prof.id === p.id ? { ...prof, customPhotoBase64: data.photoBase64, ringColor: data.ringColor } : prof));
              }
            });
            unsubscribes.push(unsub);
          });
        }
      });
    });

    // Fallback if no auth state changes
    setTimeout(() => setLoading(false), 2000);

    return () => unsubscribes.forEach(unsub => unsub());
  }, []);

  // Save active profiles to localStorage whenever they change
  useEffect(() => {
    const validProfiles = profiles.filter(p => p.user !== null || p.id === addingProfileId);
    const ids = validProfiles.map(p => p.id).filter(id => id !== '[DEFAULT]');
    localStorage.setItem('activeProfiles', JSON.stringify(ids));
  }, [profiles, addingProfileId]);

  const addProfile = () => {
    const newId = `profile_${Date.now()}`;
    const appInstance = initializeApp(firebaseConfig, newId);
    const newAuth = getAuth(appInstance);
    const newDb = getFirestore(appInstance);
    
    setAddingProfileId(newId);
    setProfiles(prev => [...prev, { id: newId, auth: newAuth, db: newDb, user: null }]);
    setIsAddingProfile(true);

    // Listen to the new auth instance
    onAuthStateChanged(newAuth, (user) => {
      setProfiles(prev => {
        if (user) {
          const isDuplicate = prev.some(p => p.user?.uid === user.uid && p.id !== newId);
          if (isDuplicate) {
             newAuth.signOut();
             // Return without the newId profile
             return prev.filter(p => p.id !== newId);
          }
          
          claimLegacyData(newDb, user);
          import('firebase/firestore').then(({ doc, onSnapshot }) => {
            onSnapshot(doc(newDb, `users/${user.uid}/settings/profile`), (docSnap) => {
              if (docSnap.exists()) {
                const data = docSnap.data();
                setProfiles(prev => prev.map(prof => prof.id === newId ? { ...prof, customPhotoBase64: data.photoBase64, ringColor: data.ringColor } : prof));
              }
            });
          });
        }
        return prev.map(prof => prof.id === newId ? { ...prof, user } : prof);
      });
      
      if (user) {
        setIsAddingProfile(false);
        setAddingProfileId(null);
      }
    });
  };

  const removeProfile = async (id: string) => {
    const profile = profiles.find(p => p.id === id);
    if (profile) {
      await profile.auth.signOut();
    }
    setProfiles(prev => prev.filter(p => p.id !== id));
  };

  const cancelAddProfile = () => {
    if (addingProfileId) {
      setProfiles(prev => prev.filter(p => p.id !== addingProfileId));
      setAddingProfileId(null);
    }
    setIsAddingProfile(false);
  };

  if (loading) {
    return <div className="min-h-screen bg-black flex items-center justify-center text-white">Loading...</div>;
  }

  const loggedInProfiles = profiles.filter(p => p.user !== null);
  const showDashboard = loggedInProfiles.length > 0 && !isAddingProfile;

  return (
    <MultiProfileContext.Provider value={{ profiles: loggedInProfiles, addProfile, removeProfile, isAddingProfile, cancelAddProfile }}>
      {showDashboard ? <Dashboard /> : <AuthComponent authInstance={profiles.find(p => p.id === addingProfileId)?.auth || defaultAuth} />}
    </MultiProfileContext.Provider>
  );
}

export default App;
