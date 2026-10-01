import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { NavBar, Footer } from './components/ui';
import DashboardView from './pages/DashboardView';
import RepositoriesView from './pages/RepositoriesView';
import TemplatesView from './pages/TemplatesView';
import SettingsView from './pages/SettingsView';
import OnboardingView from './pages/OnboardingView';
import { api } from './api';

function App() {
  const [onboarded, setOnboarded] = useState(null);

  useEffect(() => {
    const checkStatus = async () => {
      try {
        const data = await api.checkOnboarding();
        setOnboarded(data.onboarded);
      } catch (err) {
        console.error("Failed to check onboarding status", err);
      }
    };
    checkStatus();
  }, []);

  if (onboarded === null) {
    return <div className="flex items-center justify-center" style={{ minHeight: '100vh', backgroundColor: 'var(--colors-canvas)' }}>Loading...</div>;
  }

  if (onboarded === false) {
    return <OnboardingView onComplete={() => setOnboarded(true)} />;
  }

  return (
    <Router>
      <div className="flex-col" style={{ minHeight: '100vh' }}>
        <NavBar />
        <main style={{ flex: 1 }}>
          <Routes>
            <Route path="/" element={<DashboardView />} />
            <Route path="/repos" element={<RepositoriesView />} />
            <Route path="/templates" element={<TemplatesView />} />
            <Route path="/settings" element={<SettingsView />} />
          </Routes>
        </main>
        <Footer />
      </div>
    </Router>
  );
}

export default App;
