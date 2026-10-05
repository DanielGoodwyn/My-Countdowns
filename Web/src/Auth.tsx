import React, { useState } from 'react';
import { signInWithEmailAndPassword, createUserWithEmailAndPassword, sendPasswordResetEmail, signInWithPopup, GoogleAuthProvider, type Auth as FirebaseAuth } from 'firebase/auth';
import { Lock, Mail, X } from 'lucide-react';
import { useProfiles } from './App';

export default function Auth({ authInstance }: { authInstance: FirebaseAuth }) {
  const [isLogin, setIsLogin] = useState(true);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const { isAddingProfile, cancelAddProfile } = useProfiles();

  const handleAuth = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setMessage('');
    
    const trimmedEmail = email.trim();
    
    try {
      if (isLogin) {
        await signInWithEmailAndPassword(authInstance, trimmedEmail, password);
      } else {
        await createUserWithEmailAndPassword(authInstance, trimmedEmail, password);
      }
    } catch (err: any) {
      setError(err.message);
    }
  };

  const handleGoogleSignIn = async () => {
    setError('');
    setMessage('');
    try {
      const provider = new GoogleAuthProvider();
      await signInWithPopup(authInstance, provider);
    } catch (err: any) {
      setError(err.message);
    }
  };

  const handleResetPassword = async () => {
    if (!email) {
      setError("Please enter your email first to reset your password.");
      return;
    }
    try {
      await sendPasswordResetEmail(authInstance, email);
      setMessage("Password reset email sent! Check your inbox.");
      setError("");
    } catch (err: any) {
      setError(err.message);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-zinc-50 dark:bg-black p-4 transition-colors duration-200">
      <div className="max-w-md w-full bg-white dark:bg-zinc-900 rounded-2xl p-8 shadow-sm dark:shadow-none border border-zinc-200 dark:border-zinc-800 transition-colors relative">
        {isAddingProfile && (
          <button 
            onClick={cancelAddProfile} 
            className="absolute top-4 right-4 text-zinc-400 hover:text-zinc-600 dark:hover:text-zinc-200"
          >
            <X size={24} />
          </button>
        )}
        <div className="text-center mb-8">
          <h2 className="text-3xl font-bold text-zinc-900 dark:text-white">{isAddingProfile ? 'Add Profile' : 'My Countdowns'}</h2>
          <p className="text-zinc-500 dark:text-zinc-400 mt-2">Sign in to sync your countdowns</p>
        </div>

        <form onSubmit={handleAuth} className="space-y-4">
          <div>
            <div className="relative">
              <Mail className="absolute left-3 top-3 text-zinc-400 dark:text-zinc-500" size={20} />
              <input
                type="email"
                placeholder="Email address"
                className="w-full bg-zinc-100 dark:bg-zinc-800 text-zinc-900 dark:text-white rounded-lg pl-10 pr-4 py-3 focus:outline-none focus:ring-2 focus:ring-blue-500 border border-transparent dark:border-transparent transition-colors"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>
          </div>
          <div>
            <div className="relative">
              <Lock className="absolute left-3 top-3 text-zinc-400 dark:text-zinc-500" size={20} />
              <input
                type="password"
                placeholder="Password"
                className="w-full bg-zinc-100 dark:bg-zinc-800 text-zinc-900 dark:text-white rounded-lg pl-10 pr-4 py-3 focus:outline-none focus:ring-2 focus:ring-blue-500 border border-transparent dark:border-transparent transition-colors"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required={isLogin ? password.length > 0 : true}
              />
            </div>
          </div>

          {error && <p className="text-red-500 dark:text-red-400 text-sm text-center">{error}</p>}
          {message && <p className="text-green-500 dark:text-green-400 text-sm text-center">{message}</p>}

          <button
            type="submit"
            className="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-3 rounded-lg transition-colors"
          >
            {isLogin ? 'Sign In' : 'Create Account'}
          </button>
        </form>
        
        <div className="mt-4">
          <div className="relative">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-zinc-200 dark:border-zinc-700"></div>
            </div>
            <div className="relative flex justify-center text-sm">
              <span className="px-2 bg-white dark:bg-zinc-900 text-zinc-500">Or continue with</span>
            </div>
          </div>
          
          <button
            onClick={handleGoogleSignIn}
            type="button"
            className="mt-4 w-full flex items-center justify-center gap-3 bg-white dark:bg-zinc-800 border border-zinc-300 dark:border-zinc-700 hover:bg-zinc-50 dark:hover:bg-zinc-700 text-zinc-900 dark:text-white font-medium py-3 rounded-lg transition-colors"
          >
            <svg viewBox="0 0 24 24" width="20" height="20" xmlns="http://www.w3.org/2000/svg">
              <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
              <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
              <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/>
              <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
            </svg>
            Google
          </button>
        </div>

        <div className="mt-6 flex flex-col items-center space-y-3">
          {isLogin && (
            <button
              onClick={handleResetPassword}
              type="button"
              className="text-blue-600 dark:text-blue-400 hover:text-blue-500 dark:hover:text-blue-300 text-sm transition-colors"
            >
              Forgot my password
            </button>
          )}
          
          <button
            onClick={() => setIsLogin(!isLogin)}
            type="button"
            className="text-zinc-600 dark:text-zinc-400 hover:text-zinc-900 dark:hover:text-white text-sm transition-colors"
          >
            {isLogin ? "Don't have an account? Sign up" : "Already have an account? Sign in"}
          </button>
        </div>
      </div>
    </div>
  );
}
