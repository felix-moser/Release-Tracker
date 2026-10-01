import React, { useState } from 'react';
import { Card, Input, Button } from '../components/ui';
import { api } from '../api';

export default function OnboardingView({ onComplete }) {
  const [formData, setFormData] = useState({
    jira_url: '',
    jira_email: '',
    jira_api_token: '',
    github_api_key: ''
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    
    if (!formData.jira_url || !formData.jira_email || !formData.jira_api_token) {
      setError('Please fill out all required fields.');
      return;
    }

    setLoading(true);
    try {
      await api.saveSecrets(formData);
      onComplete();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex-col items-center justify-center" style={{ minHeight: '100vh', backgroundColor: 'var(--colors-canvas)' }}>
      <Card elevated className="w-full" style={{ maxWidth: '480px', padding: 'var(--spacing-xl)' }}>
        <div className="text-center mb-xl">
          <h1 className="heading-lg mb-sm">Welcome to Release Tracker</h1>
          <p className="body-md text-mute">Let's get your integrations set up.</p>
        </div>

        {error && (
          <div className="mb-md p-sm" style={{ backgroundColor: '#fee', color: 'var(--colors-error)', borderRadius: 'var(--rounded-sm)' }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="flex-col gap-md">
          <div className="flex-col gap-xs">
            <label className="label-sm">Jira URL *</label>
            <Input 
              placeholder="https://your-domain.atlassian.net"
              value={formData.jira_url}
              onChange={e => setFormData({...formData, jira_url: e.target.value})}
              required
            />
          </div>

          <div className="flex-col gap-xs">
            <label className="label-sm">Jira Email *</label>
            <Input 
              type="email"
              placeholder="you@company.com"
              value={formData.jira_email}
              onChange={e => setFormData({...formData, jira_email: e.target.value})}
              required
            />
          </div>

          <div className="flex-col gap-xs">
            <label className="label-sm">Jira API Token *</label>
            <Input 
              type="password"
              placeholder="••••••••••••••••"
              value={formData.jira_api_token}
              onChange={e => setFormData({...formData, jira_api_token: e.target.value})}
              required
            />
          </div>

          <div className="flex-col gap-xs">
            <label className="label-sm flex justify-between">
              <span>GitHub API Key</span>
              <span className="text-mute font-normal">(Optional)</span>
            </label>
            <Input 
              type="password"
              placeholder="ghp_..."
              value={formData.github_api_key}
              onChange={e => setFormData({...formData, github_api_key: e.target.value})}
            />
            <p className="body-sm text-mute mt-xs" style={{marginTop: '4px'}}>Required if you are tracking private repositories or hitting rate limits.</p>
          </div>

          <Button variant="primary" type="submit" disabled={loading} className="mt-md" style={{ width: '100%', padding: '12px 14px' }}>
            {loading ? 'Saving...' : 'Save & Continue'}
          </Button>
        </form>
      </Card>
    </div>
  );
}
