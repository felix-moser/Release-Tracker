import React, { useState, useEffect } from 'react';
import { api } from '../api';
import { Button, Input, Card } from '../components/ui';

export default function SettingsView() {
  // Scheduler state
  const [interval, setIntervalVal] = useState('');

  // Secrets state
  const [secrets, setSecrets] = useState({
    jira_url: '',
    jira_email: '',
    jira_api_token: '',
    github_api_key: ''
  });
  const [secretsMeta, setSecretsMeta] = useState({
    jira_api_token_set: false,
    github_api_key_set: false
  });
  const [touchedSecrets, setTouchedSecrets] = useState(new Set());

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [savingSecrets, setSavingSecrets] = useState(false);
  const [message, setMessage] = useState('');
  const [secretsMessage, setSecretsMessage] = useState('');
  const [error, setError] = useState(null);
  const [secretsError, setSecretsError] = useState(null);

  useEffect(() => {
    const fetchSettings = async () => {
      setLoading(true);
      try {
        const [schedulerData, secretsData] = await Promise.all([
          api.getSchedulerStatus(),
          api.getSecrets()
        ]);
        setIntervalVal(schedulerData.interval_minutes);
        setSecrets({
          jira_url: secretsData.jira_url || '',
          jira_email: secretsData.jira_email || '',
          jira_api_token: '',
          github_api_key: ''
        });
        setSecretsMeta({
          jira_api_token_set: secretsData.jira_api_token,
          github_api_key_set: secretsData.github_api_key
        });
        setError(null);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };
    fetchSettings();
  }, []);

  const handleSecretChange = (field, value) => {
    setSecrets(prev => ({ ...prev, [field]: value }));
    setTouchedSecrets(prev => new Set(prev).add(field));
  };

  const handleSaveSecrets = async (e) => {
    e.preventDefault();
    if (touchedSecrets.size === 0) {
      setSecretsMessage('No changes to save.');
      return;
    }
    setSavingSecrets(true);
    setSecretsMessage('');
    setSecretsError(null);
    try {
      const payload = {};
      for (const field of touchedSecrets) {
        if (secrets[field] !== '') {
          payload[field] = secrets[field];
        }
      }
      if (Object.keys(payload).length === 0) {
        setSecretsMessage('No changes to save.');
        setSavingSecrets(false);
        return;
      }
      await api.saveSecrets(payload);
      setSecretsMessage('Credentials saved successfully.');
      setTouchedSecrets(new Set());
      // Refresh secrets meta
      const refreshed = await api.getSecrets();
      setSecrets({
        jira_url: refreshed.jira_url || '',
        jira_email: refreshed.jira_email || '',
        jira_api_token: '',
        github_api_key: ''
      });
      setSecretsMeta({
        jira_api_token_set: refreshed.jira_api_token,
        github_api_key_set: refreshed.github_api_key
      });
    } catch (err) {
      setSecretsError(err.message);
    } finally {
      setSavingSecrets(false);
    }
  };

  const handleSaveScheduler = async (e) => {
    e.preventDefault();
    setSaving(true);
    setMessage('');
    setError(null);
    try {
      const parsedInterval = parseInt(interval, 10);
      if (isNaN(parsedInterval) || parsedInterval < 1) {
        throw new Error('Interval must be at least 1 minute.');
      }
      const data = await api.updateSchedulerInterval(parsedInterval);
      setIntervalVal(data.interval_minutes);
      setMessage('Settings saved successfully.');
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="container mt-xl">
      <div className="mb-lg">
        <h1 className="heading-lg">Settings</h1>
        <p className="body-md text-mute">Manage your system configuration.</p>
      </div>

      {loading ? (
        <div className="body-md">Loading...</div>
      ) : (
        <div className="flex-col gap-lg">
          {/* API Keys & Credentials */}
          <Card>
            <form onSubmit={handleSaveSecrets} className="flex-col gap-md">
              <div>
                <h2 className="heading-md mb-xs">API Keys & Credentials</h2>
                <p className="body-sm text-mute mb-md">
                  Manage your Jira and GitHub integration credentials.
                </p>
              </div>

              {secretsError && <div className="mb-md body-sm" style={{ color: 'var(--colors-error)' }}>Error: {secretsError}</div>}
              {secretsMessage && <div className="mb-md body-sm" style={{ color: 'var(--colors-link)' }}>{secretsMessage}</div>}

              <div className="flex-col gap-xs">
                <label className="label-sm" style={{ display: 'block' }}>Jira URL</label>
                <Input
                  placeholder="https://your-domain.atlassian.net"
                  value={secrets.jira_url}
                  onChange={e => handleSecretChange('jira_url', e.target.value)}
                />
              </div>

              <div className="flex-col gap-xs">
                <label className="label-sm" style={{ display: 'block' }}>Jira Email</label>
                <Input
                  type="email"
                  placeholder="you@company.com"
                  value={secrets.jira_email}
                  onChange={e => handleSecretChange('jira_email', e.target.value)}
                />
              </div>

              <div className="flex-col gap-xs">
                <label className="label-sm" style={{ display: 'block' }}>Jira API Token</label>
                <Input
                  type="password"
                  placeholder={secretsMeta.jira_api_token_set ? '••••••••  (token is configured)' : 'Enter your Jira API token'}
                  value={secrets.jira_api_token}
                  onChange={e => handleSecretChange('jira_api_token', e.target.value)}
                />
                {secretsMeta.jira_api_token_set && !touchedSecrets.has('jira_api_token') && (
                  <p className="body-sm" style={{ color: 'var(--colors-link)', marginTop: '4px' }}>✓ Token is configured</p>
                )}
              </div>

              <div className="flex-col gap-xs">
                <label className="label-sm flex justify-between" style={{ display: 'flex' }}>
                  <span>GitHub API Key</span>
                  <span className="text-mute" style={{ fontWeight: 400 }}>(Optional)</span>
                </label>
                <Input
                  type="password"
                  placeholder={secretsMeta.github_api_key_set ? '••••••••  (key is configured)' : 'ghp_...'}
                  value={secrets.github_api_key}
                  onChange={e => handleSecretChange('github_api_key', e.target.value)}
                />
                {secretsMeta.github_api_key_set && !touchedSecrets.has('github_api_key') && (
                  <p className="body-sm" style={{ color: 'var(--colors-link)', marginTop: '4px' }}>✓ Key is configured</p>
                )}
                <p className="body-sm text-mute" style={{ marginTop: '4px' }}>Required for tracking private repositories or avoiding rate limits.</p>
              </div>

              <div className="flex mt-sm">
                <Button variant="primary" type="submit" disabled={savingSecrets || touchedSecrets.size === 0}>
                  {savingSecrets ? 'Saving...' : 'Update Credentials'}
                </Button>
              </div>
            </form>
          </Card>

          {/* Scheduler Configuration */}
          <Card>
            <form onSubmit={handleSaveScheduler} className="flex-col gap-md">
              <div>
                <h2 className="heading-md mb-xs">Scheduler Configuration</h2>
                <p className="body-sm text-mute mb-md">
                  Configure how frequently Release Tracker searches repositories for new releases.
                </p>
              </div>

              {error && <div className="mb-md body-sm" style={{ color: 'var(--colors-error)' }}>Error: {error}</div>}
              {message && <div className="mb-md body-sm" style={{ color: 'var(--colors-link)' }}>{message}</div>}

              <div>
                <label className="label-sm mb-xs" style={{ display: 'block' }}>Update Interval (minutes)</label>
                <Input
                  type="number"
                  min="1"
                  value={interval}
                  onChange={e => setIntervalVal(e.target.value)}
                  required
                  style={{ maxWidth: '200px' }}
                />
              </div>
              <div className="flex mt-sm">
                <Button variant="primary" type="submit" disabled={saving}>
                  {saving ? 'Saving...' : 'Save Changes'}
                </Button>
              </div>
            </form>
          </Card>
        </div>
      )}
    </div>
  );
}
