import { useEffect, useState, useMemo, useRef } from 'react';
import { collection, query, onSnapshot, deleteDoc, doc, updateDoc, writeBatch, setDoc } from 'firebase/firestore';
import { LogOut, Plus, Trash2, ArrowUp, ArrowDown, Save, Image as ImageIcon, X, GripVertical } from 'lucide-react';
import { useProfiles } from './App';
import { DndContext, closestCenter, KeyboardSensor, PointerSensor, TouchSensor, useSensor, useSensors } from '@dnd-kit/core';
import { arrayMove, SortableContext, sortableKeyboardCoordinates, verticalListSortingStrategy, useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';

interface ResetItem {
  id: string;
  name: string;
  resetTime: number; // timestamp
  hexColor: string;
  imageDataBase64?: string;
  orderIndex?: number;
  profileId?: string;
}

interface ColorSwatch {
  id: string;
  name: string;
  hexColor: string;
  profileId?: string;
}

const formatHex = (hex: string) => hex.startsWith('#') ? hex : '#' + hex;

  const formatTimeLeft = (timestamp: number) => {
    const now = Date.now();
    if (now >= timestamp) return 'Countdown complete!';
    
    // Add 59999ms to prevent off-by-one truncations
    const diff = timestamp - now + 59999;
    const d = Math.floor(diff / (1000 * 60 * 60 * 24));
    const h = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
    const m = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
  
  let str = '';
  if (d > 0) str += `${d}d `;
  if (h > 0) str += `${h}h `;
  if (m >= 0) str += `${m}m `;
  return str + 'left';
};

function SortableItem({ item, index, isFirst, isLast, sortOption, startEditing, moveItem, deleteItem, disableUp, disableDown }: any) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: item.id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    zIndex: isDragging ? 100 : 1,
    opacity: isDragging ? 0.8 : 1,
  };

  return (
    <div 
      ref={setNodeRef}
      style={style}
      onClick={() => startEditing(item)}
      className={`relative overflow-hidden border border-zinc-200 dark:border-zinc-800 cursor-pointer group ${
        isFirst ? 'rounded-t-xl' : ''
      } ${isLast ? 'rounded-b-xl' : ''} ${
        !isFirst ? 'border-t-0' : ''
      } bg-white dark:bg-black transition-colors hover:bg-zinc-50 dark:hover:bg-zinc-900 ${isDragging ? 'shadow-lg border-zinc-400 dark:border-zinc-500' : ''}`}
    >
      <div className="absolute inset-0 opacity-20 dark:opacity-30 pointer-events-none transition-opacity group-hover:opacity-30 dark:group-hover:opacity-40" style={{ backgroundColor: formatHex(item.hexColor) }} />
      <div className="h-1 w-full absolute top-0 left-0" style={{ backgroundColor: formatHex(item.hexColor) }} />
      
      <div className="p-4 flex items-center gap-4 relative z-10">
        <div 
          className="w-12 h-12 rounded-full flex items-center justify-center text-xl font-bold border-2 overflow-hidden bg-white dark:bg-black flex-shrink-0"
          style={{ borderColor: formatHex(item.hexColor) }}
        >
          {item.imageDataBase64 ? (
            <img 
              src={`data:image/jpeg;base64,${item.imageDataBase64}`} 
              alt={item.name} 
              className="w-full h-full object-cover bg-white"
            />
          ) : (
            <span className="text-zinc-900 dark:text-white">{item.name.charAt(0)}</span>
          )}
        </div>
        
        <div className="flex-1 min-w-0">
          <h3 className="font-semibold text-lg truncate">{item.name}</h3>
          <p className="text-sm text-zinc-500 dark:text-zinc-400 truncate">
            {new Date(item.resetTime).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' })}
          </p>
          <p className="font-medium mt-1 truncate" style={{ color: Date.now() > item.resetTime ? '#ef4444' : 'inherit' }}>
            {formatTimeLeft(item.resetTime)}
          </p>
        </div>

        {sortOption === 'custom' && (
          <div className="flex items-center border-l sm:border-l-0 sm:border-r border-zinc-200 dark:border-zinc-700 pl-4 sm:pl-0 sm:pr-4 sm:mr-2">
            <div 
              {...attributes} 
              {...listeners}
              onClick={(e) => e.stopPropagation()}
              className="p-2 cursor-grab active:cursor-grabbing text-zinc-400 hover:text-zinc-900 dark:hover:text-white transition-colors mr-2 hidden sm:block"
            >
              <GripVertical size={20} />
            </div>
            <div className="flex flex-col">
              <button 
                onClick={(e) => moveItem(e, index, -1)}
                disabled={disableUp}
                className="p-1 text-zinc-400 hover:text-zinc-900 dark:hover:text-white disabled:opacity-30 disabled:hover:text-zinc-400 transition-colors"
              >
                <ArrowUp size={18} />
              </button>
              <button 
                onClick={(e) => moveItem(e, index, 1)}
                disabled={disableDown}
                className="p-1 text-zinc-400 hover:text-zinc-900 dark:hover:text-white disabled:opacity-30 disabled:hover:text-zinc-400 transition-colors"
              >
                <ArrowDown size={18} />
              </button>
            </div>
            <div 
              {...attributes} 
              {...listeners}
              onClick={(e) => e.stopPropagation()}
              className="p-2 cursor-grab active:cursor-grabbing text-zinc-400 hover:text-zinc-900 dark:hover:text-white transition-colors ml-2 sm:hidden block"
            >
              <GripVertical size={20} />
            </div>
          </div>
        )}

        <button 
          onClick={(e) => deleteItem(e, item.id, item.profileId)}
          className="p-2 text-zinc-400 hover:text-red-500 transition-colors"
        >
          <Trash2 size={20} />
        </button>
      </div>

      <div className="h-1 w-full absolute bottom-0 left-0" style={{ backgroundColor: formatHex(item.hexColor) }} />
    </div>
  );
}

export default function Dashboard() {
  const { profiles, addProfile, removeProfile } = useProfiles();
  const primaryProfile = profiles.length > 0 ? profiles[0] : null;

  const [itemsByProfile, setItemsByProfile] = useState<Record<string, ResetItem[]>>({});
  const [swatchesByProfile, setSwatchesByProfile] = useState<Record<string, ColorSwatch[]>>({});
  
  const items = useMemo(() => {
    return profiles.flatMap(p => itemsByProfile[p.id] || []);
  }, [itemsByProfile, profiles]);
  const [editingSwatch, setEditingSwatch] = useState<ColorSwatch | null>(null);
  const [swatchEditName, setSwatchEditName] = useState("");
  const [swatchEditColor, setSwatchEditColor] = useState("");
  const [swatchEditProfiles, setSwatchEditProfiles] = useState<Set<string>>(new Set());

  const swatches = useMemo(() => {
    const all = profiles.flatMap(p => swatchesByProfile[p.id] || []);
    return all.sort((a, b) => a.name.localeCompare(b.name));
  }, [swatchesByProfile, profiles]);
  
  const [name, setName] = useState('');
  const [date, setDate] = useState('');
  const [isRelativeTime, setIsRelativeTime] = useState(localStorage.getItem('preferRelativeTime') === 'true');
  const [relativeDays, setRelativeDays] = useState(1);
  const [relativeHours, setRelativeHours] = useState(0);
  const [relativeMinutes, setRelativeMinutes] = useState(0);
  const [color, setColor] = useState('#3B82F6');
  const [imageData, setImageData] = useState<string | null>(null);
  const [selectedProfileId, setSelectedProfileId] = useState<string>(primaryProfile?.id || '');
  
  const [editingId, setEditingId] = useState<string | null>(null);
  const [sortOption, setSortOption] = useState<string>('soonestAsc');
  
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (profiles.length === 0) return;
    if (!selectedProfileId || !profiles.find(p => p.id === selectedProfileId)) {
      setSelectedProfileId(profiles[0].id);
    }
  }, [profiles, selectedProfileId]);

  useEffect(() => {
    const unsubscribes: any[] = [];
    
    profiles.forEach(p => {
      if (!p.user) return;
      
      const q = query(collection(p.db, `users/${p.user.uid}/resets`));
      const u1 = onSnapshot(q, (snapshot) => {
        const newItems: ResetItem[] = [];
        snapshot.forEach((doc) => {
          newItems.push({ id: doc.id, profileId: p.id, ...doc.data() } as ResetItem);
        });
        setItemsByProfile(prev => ({ ...prev, [p.id]: newItems }));
      });
      unsubscribes.push(u1);

      const swatchQ = query(collection(p.db, `users/${p.user.uid}/swatches`));
      const u2 = onSnapshot(swatchQ, (snapshot) => {
        const newSwatches: ColorSwatch[] = [];
        snapshot.forEach((doc) => {
          newSwatches.push({ id: doc.id, profileId: p.id, ...doc.data() } as ColorSwatch);
        });
        setSwatchesByProfile(prev => ({ ...prev, [p.id]: newSwatches }));
      });
      unsubscribes.push(u2);
      
      const settingsRef = doc(p.db, `users/${p.user.uid}/settings/preferences`);
      const u3 = onSnapshot(settingsRef, (docSnap) => {
        if (docSnap.exists() && docSnap.data().sortOption) {
          setSortOption(docSnap.data().sortOption);
        }
      });
      unsubscribes.push(u3);
    });

    return () => unsubscribes.forEach(u => u());
  }, [profiles]);

  const handleSortChange = async (newSort: string) => {
    setSortOption(newSort);
    // Sync to all active profiles
    for (const p of profiles) {
      if (p.user) {
        await setDoc(doc(p.db, `users/${p.user.uid}/settings/preferences`), { sortOption: newSort }, { merge: true });
      }
    }
  };

  const sortedItems = useMemo(() => {
    const list = [...items];
    switch (sortOption) {
      case 'soonestAsc':
        return list.sort((a, b) => a.resetTime - b.resetTime);
      case 'soonestDesc':
        return list.sort((a, b) => b.resetTime - a.resetTime);
      case 'alphabetical':
        return list.sort((a, b) => a.name.localeCompare(b.name));
      case 'custom':
        return list.sort((a, b) => (a.orderIndex || 0) - (b.orderIndex || 0));
      default:
        return list;
    }
  }, [items, sortOption]);

  const saveItem = async (e: React.FormEvent) => {
    e.preventDefault();
    const targetProfile = profiles.find(p => p.id === selectedProfileId);
    if (!targetProfile || !targetProfile.user || !name) return;
    
    let finalTimestamp = 0;
    if (isRelativeTime) {
      finalTimestamp = Date.now() + (relativeDays * 86400000) + (relativeHours * 3600000) + (relativeMinutes * 60000);
    } else {
      if (!date) return;
      finalTimestamp = new Date(date).getTime();
    }
    
    const payload: any = {
      name,
      resetTime: finalTimestamp,
      hexColor: color.replace('#', ''),
    };
    
    if (imageData) {
      payload.imageDataBase64 = imageData;
    } else {
      payload.imageDataBase64 = null;
    }

    if (editingId) {
      const oldItem = items.find(i => i.id === editingId);
      const oldProfileId = oldItem?.profileId;
      
      if (oldProfileId && oldProfileId !== selectedProfileId) {
        // Moving profile
        const oldProfile = profiles.find(p => p.id === oldProfileId);
        if (oldProfile && oldProfile.user) {
          await deleteDoc(doc(oldProfile.db, `users/${oldProfile.user.uid}/resets/${editingId}`));
        }
        payload.orderIndex = itemsByProfile[selectedProfileId]?.length || 0;
        await setDoc(doc(targetProfile.db, `users/${targetProfile.user.uid}/resets/${editingId}`), payload);
      } else {
        // Same profile edit
        await updateDoc(doc(targetProfile.db, `users/${targetProfile.user.uid}/resets/${editingId}`), payload);
      }
      setEditingId(null);
    } else {
      payload.orderIndex = itemsByProfile[selectedProfileId]?.length || 0;
      const newId = crypto.randomUUID().toUpperCase();
      await setDoc(doc(targetProfile.db, `users/${targetProfile.user.uid}/resets/${newId}`), payload);
    }
    
    setName('');
    setDate('');
    setImageData(null);
  };

  const cancelEdit = () => {
    setEditingId(null);
    setName('');
    setDate('');
    setImageData(null);
    if (profiles.length > 0) {
      setSelectedProfileId(profiles[0].id);
    }
  };

  const startEditing = (item: ResetItem) => {
    setEditingId(item.id);
    setName(item.name);
    
    const d = new Date(item.resetTime);
    const dateString = new Date(d.getTime() - (d.getTimezoneOffset() * 60000)).toISOString().slice(0, 16);
    setDate(dateString);
    
    // Set relative values based on diff
    const diff = item.resetTime - Date.now() + 59999;
    if (diff > 0) {
      setRelativeDays(Math.floor(diff / (1000 * 60 * 60 * 24)));
      setRelativeHours(Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)));
      setRelativeMinutes(Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60)));
    } else {
      setRelativeDays(0);
      setRelativeHours(0);
      setRelativeMinutes(0);
    }
    
    setIsRelativeTime(localStorage.getItem('preferRelativeTime') === 'true');
    
    setColor(formatHex(item.hexColor));
    setImageData(item.imageDataBase64 || null);
    if (item.profileId) setSelectedProfileId(item.profileId);
    
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const deleteItem = async (e: React.MouseEvent, id: string, profileId?: string) => {
    e.stopPropagation();
    const targetProfile = profiles.find(p => p.id === profileId) || profiles[0];
    if (!targetProfile || !targetProfile.user) return;
    
    await deleteDoc(doc(targetProfile.db, `users/${targetProfile.user.uid}/resets/${id}`));
    if (editingId === id) cancelEdit();
  };

  const updateFirestoreOrder = (list: ResetItem[]) => {
    // We must use the global index in the combined list to allow interleaving accounts
    profiles.forEach(async (p) => {
      if (!p.user) return;
      const batch = writeBatch(p.db);
      list.forEach((item, globalIndex) => {
        if (item.profileId === p.id) {
          const docRef = doc(p.db, `users/${p.user!.uid}/resets/${item.id}`);
          batch.update(docRef, { orderIndex: globalIndex });
        }
      });
      await batch.commit();
    });
  };

  const moveItem = (e: React.MouseEvent, index: number, direction: number) => {
    e.stopPropagation();
    const newIndex = index + direction;
    if (newIndex < 0 || newIndex >= sortedItems.length) return;

    const list = [...sortedItems];
    const temp = list[index];
    list[index] = list[newIndex];
    list[newIndex] = temp;

    updateFirestoreOrder(list);
  };

  const handleDragEnd = (event: any) => {
    const { active, over } = event;
    
    if (over && active.id !== over.id) {
      const oldIndex = sortedItems.findIndex((item) => item.id === active.id);
      const newIndex = sortedItems.findIndex((item) => item.id === over.id);
      
      const list = arrayMove(sortedItems, oldIndex, newIndex);
      updateFirestoreOrder(list);
    }
  };

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 5,
      },
    }),
    useSensor(TouchSensor, {
      activationConstraint: {
        delay: 100,
        tolerance: 5,
      }
    }),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    })
  );

  const saveSwatch = async () => {
    const targetProfile = profiles.find(p => p.id === selectedProfileId) || profiles[0];
    if (!targetProfile || !targetProfile.user) return;
    
    const swatchName = prompt("Enter a name for this swatch:");
    if (!swatchName) return;
    
    const newId = crypto.randomUUID().toUpperCase();
    await setDoc(doc(targetProfile.db, `users/${targetProfile.user.uid}/swatches/${newId}`), {
      name: swatchName,
      hexColor: color.replace('#', '')
    });
  };

  const compressImage = (file: File, callback: (base64: string) => void) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      const img = new Image();
      img.onload = () => {
        const canvas = document.createElement('canvas');
        const MAX_SIZE = 400; // Small size for profile/icons
        let width = img.width;
        let height = img.height;

        if (width > height) {
          if (width > MAX_SIZE) {
            height *= MAX_SIZE / width;
            width = MAX_SIZE;
          }
        } else {
          if (height > MAX_SIZE) {
            width *= MAX_SIZE / height;
            height = MAX_SIZE;
          }
        }

        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext('2d');
        ctx?.drawImage(img, 0, 0, width, height);
        
        // Quality 0.7 for good compression
        const dataUrl = canvas.toDataURL('image/jpeg', 0.7);
        callback(dataUrl.split(',')[1]);
      };
      img.src = e.target?.result as string;
    };
    reader.readAsDataURL(file);
  };

  const handleImageChange = (file: File) => {
    compressImage(file, setImageData);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleImageChange(e.dataTransfer.files[0]);
    }
  };

  const handlePaste = (e: React.ClipboardEvent) => {
    if (e.clipboardData.files && e.clipboardData.files.length > 0) {
      handleImageChange(e.clipboardData.files[0]);
    }
  };



  return (
    <div className="min-h-screen bg-zinc-50 dark:bg-black text-zinc-900 dark:text-white p-4 sm:p-8 transition-colors duration-200">
      <div className="max-w-2xl mx-auto">
        <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-4 mb-8">
          <h1 className="text-3xl font-bold">My Countdowns</h1>
          <div className="flex flex-wrap items-center gap-4 w-full lg:w-auto justify-between lg:justify-end">
            <div className="flex bg-zinc-100 dark:bg-zinc-800 p-1 rounded-full border border-zinc-200 dark:border-zinc-700 w-full sm:w-auto overflow-x-auto">
              <button 
                type="button"
                onClick={() => handleSortChange('soonestAsc')}
                className={`flex-1 sm:flex-none px-3 py-1.5 text-xs sm:text-sm rounded-full transition-colors whitespace-nowrap ${sortOption === 'soonestAsc' ? 'bg-white dark:bg-zinc-600 shadow-sm text-zinc-900 dark:text-white font-medium' : 'text-zinc-500 hover:text-zinc-700 dark:text-zinc-400 dark:hover:text-zinc-200'}`}
              >Soonest</button>
              <button 
                type="button"
                onClick={() => handleSortChange('soonestDesc')}
                className={`flex-1 sm:flex-none px-3 py-1.5 text-xs sm:text-sm rounded-full transition-colors whitespace-nowrap ${sortOption === 'soonestDesc' ? 'bg-white dark:bg-zinc-600 shadow-sm text-zinc-900 dark:text-white font-medium' : 'text-zinc-500 hover:text-zinc-700 dark:text-zinc-400 dark:hover:text-zinc-200'}`}
              >Latest</button>
              <button 
                type="button"
                onClick={() => handleSortChange('alphabetical')}
                className={`flex-1 sm:flex-none px-3 py-1.5 text-xs sm:text-sm rounded-full transition-colors whitespace-nowrap ${sortOption === 'alphabetical' ? 'bg-white dark:bg-zinc-600 shadow-sm text-zinc-900 dark:text-white font-medium' : 'text-zinc-500 hover:text-zinc-700 dark:text-zinc-400 dark:hover:text-zinc-200'}`}
              >A-Z</button>
              <button 
                type="button"
                onClick={() => handleSortChange('custom')}
                className={`flex-1 sm:flex-none px-3 py-1.5 text-xs sm:text-sm rounded-full transition-colors whitespace-nowrap ${sortOption === 'custom' ? 'bg-white dark:bg-zinc-600 shadow-sm text-zinc-900 dark:text-white font-medium' : 'text-zinc-500 hover:text-zinc-700 dark:text-zinc-400 dark:hover:text-zinc-200'}`}
              >Custom</button>
            </div>
            <div className="flex items-center gap-2 lg:gap-4">
              <div className="flex -space-x-2 mr-2">
                {profiles.map(p => (
                  <div 
                    key={p.id} 
                    className="relative group"
                  >
                    <img 
                      className="inline-block h-8 w-8 sm:h-10 sm:w-10 rounded-full ring-2 bg-white cursor-pointer" 
                      src={p.customPhotoBase64 ? `data:image/jpeg;base64,${p.customPhotoBase64}` : (p.user?.photoURL || `https://ui-avatars.com/api/?name=${p.user?.email}&background=random`)} 
                      alt={p.user?.email || 'Profile'} 
                      title={`${p.user?.email} - Hover for options`}
                      style={{ '--tw-ring-color': p.ringColor ? formatHex(p.ringColor) : (document.documentElement.classList.contains('dark') ? '#18181b' : '#ffffff') } as React.CSSProperties}
                    />
                    <div className="absolute top-full right-0 mt-2 before:absolute before:-top-2 before:left-0 before:w-full before:h-2 before:content-[''] bg-white dark:bg-zinc-800 rounded shadow-lg p-2 flex flex-col gap-2 z-50 opacity-0 group-hover:opacity-100 pointer-events-none group-hover:pointer-events-auto transition-opacity border border-zinc-200 dark:border-zinc-700 min-w-[150px]">
                      <label className="text-sm cursor-pointer hover:bg-zinc-100 dark:hover:bg-zinc-700 p-2 rounded text-zinc-800 dark:text-zinc-200 flex items-center gap-2 relative overflow-hidden">
                        <ImageIcon size={14} />
                        Change Photo
                        <input 
                          type="file" 
                          accept="image/*"
                          className="hidden" 
                          onChange={(e) => {
                            const file = e.target.files?.[0];
                            if (file && p.user) {
                              compressImage(file, async (base64String) => {
                                const { doc, setDoc } = await import('firebase/firestore');
                                await setDoc(doc(p.db, `users/${p.user!.uid}/settings/profile`), { photoBase64: base64String }, { merge: true });
                              });
                            }
                          }} 
                        />
                      </label>
                      <label className="text-sm cursor-pointer hover:bg-zinc-100 dark:hover:bg-zinc-700 p-2 rounded text-zinc-800 dark:text-zinc-200 flex items-center gap-2 relative overflow-hidden">
                        <div className="w-3.5 h-3.5 rounded-full border border-black/20 dark:border-white/20" style={{ backgroundColor: p.ringColor ? formatHex(p.ringColor) : '#000000' }} />
                        Change Color
                        <input 
                          type="color" 
                          className="absolute inset-0 opacity-0 cursor-pointer w-full h-full"
                          value={p.ringColor ? formatHex(p.ringColor) : '#000000'}
                          onChange={async (e) => {
                            if (p.user) {
                              const newColor = e.target.value;
                              const { doc, setDoc } = await import('firebase/firestore');
                              await setDoc(doc(p.db, `users/${p.user!.uid}/settings/profile`), { ringColor: newColor }, { merge: true });
                            }
                          }} 
                        />
                      </label>
                      <button 
                        onClick={() => removeProfile(p.id)}
                        className="text-sm text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 p-2 rounded text-left flex items-center gap-2"
                      >
                        <LogOut size={14} />
                        Log Out
                      </button>
                    </div>
                  </div>
                ))}
              </div>
              <button 
                onClick={addProfile}
                title="Add Account"
                className="w-8 h-8 sm:w-10 sm:h-10 rounded-full border-2 border-dashed border-zinc-300 dark:border-zinc-700 flex items-center justify-center text-zinc-500 hover:text-zinc-700 dark:hover:text-zinc-300 hover:border-zinc-400 transition-colors"
              >
                <Plus size={20} />
              </button>
            </div>
          </div>
        </div>

        <div className="bg-white dark:bg-zinc-900 rounded-xl p-4 mb-8 shadow-sm dark:shadow-none border border-zinc-200 dark:border-zinc-800 transition-colors">
          <form 
            onSubmit={saveItem} 
            onPaste={handlePaste}
            className="flex flex-wrap gap-4 items-center"
          >
            <div 
              className="relative w-12 h-12 flex-shrink-0 rounded-full border-2 border-dashed border-zinc-300 dark:border-zinc-600 flex items-center justify-center overflow-hidden cursor-pointer hover:border-blue-500 transition-colors group bg-zinc-50 dark:bg-zinc-800"
              onClick={() => fileInputRef.current?.click()}
              onDragOver={(e) => e.preventDefault()}
              onDrop={handleDrop}
              title="Click, drop, or paste an image"
            >
              {imageData ? (
                <>
                  <img src={`data:image/jpeg;base64,${imageData}`} className="w-full h-full object-cover" alt="Preview" />
                  <div className="absolute inset-0 bg-black bg-opacity-50 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                    <X size={16} className="text-white" onClick={(e) => { e.stopPropagation(); setImageData(null); }} />
                  </div>
                </>
              ) : (
                <ImageIcon size={20} className="text-zinc-400" />
              )}
            </div>
            <input 
              type="file" 
              ref={fileInputRef} 
              className="hidden" 
              accept="image/*"
              onChange={(e) => {
                if (e.target.files && e.target.files[0]) handleImageChange(e.target.files[0]);
              }}
            />

            <input
              type="text"
              placeholder="Countdown Label"
              value={name}
              onChange={e => setName(e.target.value)}
              className="flex-1 min-w-[200px] bg-zinc-100 dark:bg-zinc-800 rounded-lg px-4 py-2 focus:ring-2 focus:ring-blue-500 outline-none transition-colors border border-transparent dark:border-transparent"
              required
            />
            
            <div className="flex items-center gap-2 border border-zinc-200 dark:border-zinc-700 p-1 rounded-lg">
              <button 
                type="button" 
                onClick={() => {
                  setIsRelativeTime(false);
                  localStorage.setItem('preferRelativeTime', 'false');
                }}
                className={`text-xs px-2 py-1 rounded ${!isRelativeTime ? 'bg-zinc-200 dark:bg-zinc-700 text-black dark:text-white' : 'text-zinc-500 dark:text-zinc-400 hover:text-black dark:hover:text-white'}`}
              >
                Exact Date/Time
              </button>
              <button 
                type="button" 
                onClick={() => {
                  setIsRelativeTime(true);
                  localStorage.setItem('preferRelativeTime', 'true');
                }}
                className={`text-xs px-2 py-1 rounded ${isRelativeTime ? 'bg-zinc-200 dark:bg-zinc-700 text-black dark:text-white' : 'text-zinc-500 dark:text-zinc-400 hover:text-black dark:hover:text-white'}`}
              >
                Duration Left
              </button>
            </div>
            
            {!isRelativeTime ? (
              <input
                type="datetime-local"
                value={date}
                onChange={e => setDate(e.target.value)}
                className="w-full sm:w-auto bg-zinc-100 dark:bg-zinc-800 rounded-lg px-4 py-2 outline-none dark:[color-scheme:dark] transition-colors"
                required
              />
            ) : (
              <div className="flex gap-2 items-center">
                <input
                  type="number"
                  min="0"
                  value={relativeDays}
                  onChange={e => setRelativeDays(parseInt(e.target.value) || 0)}
                  className="w-16 bg-zinc-100 dark:bg-zinc-800 rounded-lg px-2 py-2 outline-none transition-colors text-center"
                  placeholder="D"
                  title="Days"
                />
                <span className="text-zinc-500">d</span>
                <input
                  type="number"
                  min="0"
                  max="23"
                  value={relativeHours}
                  onChange={e => setRelativeHours(parseInt(e.target.value) || 0)}
                  className="w-16 bg-zinc-100 dark:bg-zinc-800 rounded-lg px-2 py-2 outline-none transition-colors text-center"
                  placeholder="H"
                  title="Hours"
                />
                <span className="text-zinc-500">h</span>
                <input
                  type="number"
                  min="0"
                  max="59"
                  value={relativeMinutes}
                  onChange={e => setRelativeMinutes(parseInt(e.target.value) || 0)}
                  className="w-16 bg-zinc-100 dark:bg-zinc-800 rounded-lg px-2 py-2 outline-none transition-colors text-center"
                  placeholder="M"
                  title="Minutes"
                />
                <span className="text-zinc-500">m</span>
              </div>
            )}
            
            <div className="flex items-center gap-3 w-full sm:w-auto justify-end">
              {profiles.length > 1 && (
                <select
                  value={selectedProfileId}
                  onChange={(e) => setSelectedProfileId(e.target.value)}
                  className="bg-zinc-100 dark:bg-zinc-800 text-sm rounded-lg px-2 py-2 outline-none border border-transparent dark:border-transparent transition-colors max-w-[100px] sm:max-w-[120px] truncate"
                  title="Save to profile"
                >
                  {profiles.map(p => (
                    <option key={p.id} value={p.id}>{p.user?.email}</option>
                  ))}
                </select>
              )}
              
              <div className="relative w-10 h-10 rounded-full overflow-hidden flex-shrink-0 border-2 border-transparent hover:border-blue-500 transition-colors">
                <input
                  type="color"
                  value={color}
                  onChange={e => setColor(e.target.value)}
                  className="absolute top-[-10px] left-[-10px] w-16 h-16 cursor-pointer border-0 p-0 bg-transparent"
                />
              </div>
              <button type="submit" className="w-10 h-10 bg-blue-600 hover:bg-blue-700 text-white rounded-full flex items-center justify-center transition-colors shadow-sm">
                {editingId ? <Save size={20} /> : <Plus size={24} />}
              </button>
              {editingId && (
                <button type="button" onClick={cancelEdit} className="w-10 h-10 bg-zinc-200 dark:bg-zinc-700 hover:bg-zinc-300 dark:hover:bg-zinc-600 text-zinc-800 dark:text-white rounded-full flex items-center justify-center transition-colors shadow-sm" title="Cancel Editing">
                  <X size={20} />
                </button>
              )}
            </div>
          </form>

          <div className="mt-4 flex flex-wrap gap-2 items-center">
            {swatches.length > 0 && <span className="text-zinc-500 dark:text-zinc-400 text-sm mr-2">Saved Swatches:</span>}
            {Array.from(new Map(swatches.map(s => [`${s.name}-${formatHex(s.hexColor).toLowerCase()}`, s])).values()).map(swatch => {
              const matchedProfiles = swatches.filter(s => s.name === swatch.name && formatHex(s.hexColor).toLowerCase() === formatHex(swatch.hexColor).toLowerCase()).map(s => profiles.find(p => p.id === s.profileId)?.user?.email).filter(Boolean);
              
              return (
                <button
                  key={swatch.id}
                  type="button"
                  title={`${swatch.name}${matchedProfiles.length > 1 ? ` (${matchedProfiles.join(', ')})` : ''} (Right-click/Long-press to edit)`}
                  onClick={() => setColor(formatHex(swatch.hexColor))}
                  onContextMenu={(e) => {
                    e.preventDefault();
                    setEditingSwatch(swatch);
                    setSwatchEditName(swatch.name);
                    setSwatchEditColor(formatHex(swatch.hexColor));
                    
                    const matchedProfileIds = new Set<string>();
                    swatches.forEach(s => {
                      if (s.name === swatch.name && formatHex(s.hexColor).toLowerCase() === formatHex(swatch.hexColor).toLowerCase()) {
                        if (s.profileId) matchedProfileIds.add(s.profileId);
                      }
                    });
                    if (matchedProfileIds.size === 0) matchedProfileIds.add(swatch.profileId || profiles[0].id);
                    setSwatchEditProfiles(matchedProfileIds);
                  }}
                  className={`w-6 h-6 rounded-full border-2 transition-transform hover:scale-110 ${color.toLowerCase() === formatHex(swatch.hexColor).toLowerCase() ? 'border-zinc-900 dark:border-white' : 'border-transparent'}`}
                  style={{ backgroundColor: formatHex(swatch.hexColor) }}
                />
              );
            })}
            {!swatches.some(s => formatHex(s.hexColor).toLowerCase() === color.toLowerCase()) && (
              <button 
                type="button" 
                onClick={saveSwatch}
                className="text-xs bg-zinc-100 dark:bg-zinc-800 hover:bg-zinc-200 dark:hover:bg-zinc-700 text-zinc-600 dark:text-zinc-300 px-2 py-1 rounded transition-colors border border-zinc-200 dark:border-zinc-700 ml-1 whitespace-nowrap"
              >
                + Save Swatch
              </button>
            )}
          </div>
        </div>

        <div className="flex flex-col shadow-sm dark:shadow-none">
          <DndContext 
            sensors={sensors}
            collisionDetection={closestCenter}
            onDragEnd={handleDragEnd}
          >
            <SortableContext 
              items={sortedItems.map(i => i.id)}
              strategy={verticalListSortingStrategy}
            >
              {sortedItems.map((item, index) => (
                <SortableItem 
                  key={item.id}
                  item={item}
                  index={index}
                  isFirst={index === 0}
                  isLast={index === sortedItems.length - 1}
                  sortOption={sortOption}
                  startEditing={startEditing}
                  moveItem={moveItem}
                  deleteItem={deleteItem}
                  disableUp={index === 0}
                  disableDown={index === sortedItems.length - 1}
                />
              ))}
            </SortableContext>
          </DndContext>
          {items.length === 0 && (
            <p className="text-center text-zinc-500 mt-12">No resets saved yet. Add one above!</p>
          )}
        </div>
      </div>

      {/* Swatch Edit Modal */}
      {editingSwatch && (
        <div 
          className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 z-50"
          onClick={() => setEditingSwatch(null)}
          onKeyDown={(e) => { if (e.key === 'Escape') setEditingSwatch(null); }}
        >
          <div 
            className="bg-white dark:bg-zinc-900 rounded-2xl p-6 max-w-sm w-full space-y-6 relative shadow-xl border border-zinc-200 dark:border-zinc-800"
            onClick={e => e.stopPropagation()}
          >
            <button 
              onClick={() => setEditingSwatch(null)}
              className="absolute top-4 right-4 text-zinc-400 hover:text-zinc-600 dark:hover:text-zinc-200"
            >
              <X size={20} />
            </button>
            <h2 className="text-xl font-bold pr-8">Edit Swatch</h2>
            
            <div className="space-y-4">
              <div>
                <label className="block text-sm text-zinc-500 mb-1">Name</label>
                <input 
                  type="text" 
                  value={swatchEditName} 
                  autoFocus
                  onChange={e => setSwatchEditName(e.target.value)} 
                  className="w-full bg-zinc-100 dark:bg-zinc-800 rounded-lg px-4 py-2 outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              
              <div>
                <label className="block text-sm text-zinc-500 mb-1">Color</label>
                <div className="flex gap-4 items-center">
                  <input 
                    type="color" 
                    value={swatchEditColor} 
                    onChange={e => setSwatchEditColor(e.target.value)} 
                    className="w-12 h-12 rounded cursor-pointer border-0 p-0 bg-transparent"
                  />
                  <span className="font-mono text-sm">{swatchEditColor.toUpperCase()}</span>
                </div>
              </div>
              
              {profiles.length > 1 && (
                <div>
                  <label className="block text-sm text-zinc-500 mb-2">Apply to Accounts</label>
                  <div className="space-y-2 max-h-40 overflow-y-auto">
                    {profiles.map(p => (
                      <label key={p.id} className="flex items-center gap-3 cursor-pointer p-1 hover:bg-zinc-50 dark:hover:bg-zinc-800/50 rounded">
                        <input 
                          type="checkbox" 
                          checked={swatchEditProfiles.has(p.id)}
                          onChange={(e) => {
                            const newSet = new Set(swatchEditProfiles);
                            if (e.target.checked) newSet.add(p.id);
                            else newSet.delete(p.id);
                            setSwatchEditProfiles(newSet);
                          }}
                          className="w-4 h-4 rounded text-blue-500"
                        />
                        <span className="text-sm truncate">{p.user?.email}</span>
                      </label>
                    ))}
                  </div>
                </div>
              )}
            </div>

            <div className="flex gap-3 justify-end pt-2 border-t border-zinc-200 dark:border-zinc-800">
              <button 
                onClick={async () => {
                  if (confirm(`Delete swatch '${editingSwatch.name}' from all selected accounts?`)) {
                    for (const p of profiles) {
                      if (swatchEditProfiles.has(p.id) && p.user) {
                        const matching = swatches.find(s => s.profileId === p.id && s.name === editingSwatch.name && formatHex(s.hexColor).toLowerCase() === formatHex(editingSwatch.hexColor).toLowerCase());
                        if (matching) {
                          await deleteDoc(doc(p.db, `users/${p.user.uid}/swatches/${matching.id}`));
                        }
                      }
                    }
                    setEditingSwatch(null);
                  }
                }}
                className="px-4 py-2 text-red-500 font-medium hover:bg-red-50 dark:hover:bg-red-950/30 rounded-lg transition-colors mr-auto"
              >
                Delete
              </button>
              <button 
                onClick={() => setEditingSwatch(null)}
                className="px-4 py-2 font-medium hover:bg-zinc-100 dark:hover:bg-zinc-800 rounded-lg transition-colors"
              >
                Cancel
              </button>
              <button 
                onClick={async () => {
                  if (!swatchEditName.trim()) return;
                  
                  // Delete old matching swatches from ALL profiles
                  for (const p of profiles) {
                    if (!p.user) continue;
                    const matching = swatches.find(s => s.profileId === p.id && s.name === editingSwatch.name && formatHex(s.hexColor).toLowerCase() === formatHex(editingSwatch.hexColor).toLowerCase());
                    if (matching) {
                      await deleteDoc(doc(p.db, `users/${p.user.uid}/swatches/${matching.id}`));
                    }
                  }
                  
                  // Create new swatches in selected profiles
                  for (const pid of Array.from(swatchEditProfiles)) {
                    const p = profiles.find(pr => pr.id === pid);
                    if (!p || !p.user) continue;
                    const newId = crypto.randomUUID().toUpperCase();
                    await setDoc(doc(p.db, `users/${p.user.uid}/swatches/${newId}`), {
                      name: swatchEditName.trim(),
                      hexColor: swatchEditColor.replace('#', '')
                    });
                  }
                  
                  setEditingSwatch(null);
                }}
                className="px-4 py-2 bg-blue-500 text-white font-medium rounded-lg hover:bg-blue-600 transition-colors disabled:opacity-50"
                disabled={swatchEditProfiles.size === 0}
              >
                Save
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
