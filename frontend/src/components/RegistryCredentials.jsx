import React, { useEffect, useState } from 'react';
import { api } from '../api';
import { Button, Card, Input } from './ui';
export default function RegistryCredentials() {
  const [registries, setRegistries] = useState([]);
  const [form, setForm] = useState({ registry: '', username: '', token: '' });
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [saving, setSaving] = useState(false);
  const load = async () => {
    try { setRegistries(await api.getRegistries()); }
    catch (e) { setError(e.message); }
  };
  useEffect(() => { load(); }, []);
  const save = async e => {
    e.preventDefault(); setSaving(true); setError(''); setMessage('');
    try { await api.saveRegistry(form); setForm({ registry: '', username: '', token: '' }); await load(); setMessage('Registry credentials saved.'); }
    catch (e) { setError(e.message); }
    finally { setSaving(false); }
  };
  const remove = async registry => {
    try { await api.deleteRegistry(registry); await load(); setMessage('Registry removed.'); }
    catch (e) { setError(e.message); }
  };
  return <Card>
    <div>
      <h2 className="heading-md mb-xs">Container Registry Credentials</h2>
      <p className="body-sm text-mute mb-md">Configure credentials for Docker Hub, GHCR, and other private registries.</p>
    </div>
    {error && <p role="alert" className="mb-md body-sm" style={{ color: 'var(--colors-error)' }}>{error}</p>}{message && <p role="status" className="mb-md body-sm" style={{ color: 'var(--colors-link)' }}>{message}</p>}
    {registries.length > 0 && <div className="table-container mb-md"><table><thead><tr><th>Registry</th><th>Username</th><th className="text-center">Token</th><th className="text-center">Actions</th></tr></thead>
      <tbody>{registries.map(r => <tr key={r.registry}><td><strong>{r.registry}</strong></td><td>{r.username}</td><td className="text-center"><span className="mono-eyebrow token-badge" style={{ color: r.token_set ? 'var(--colors-link)' : 'var(--colors-mute)' }}>{r.token_set ? 'Configured' : 'Missing'}</span></td><td><div className="flex justify-center gap-sm">
        <Button variant="ghost-sm" onClick={() => setForm({ registry: r.registry, username: r.username, token: '' })}>Edit</Button>
        <Button variant="danger" onClick={() => remove(r.registry)}>Remove</Button>
      </div></td></tr>)}</tbody></table></div>}
    <form className="flex-col gap-md" onSubmit={save}>
      <div className="flex-col gap-xs">
        <label htmlFor="registry-host" className="label-sm display-block">Registry host</label>
        <Input id="registry-host" placeholder="docker.io or ghcr.io" required value={form.registry} onChange={e => setForm({ ...form, registry: e.target.value })} />
      </div>
      <div className="flex-col gap-xs">
        <label htmlFor="registry-user" className="label-sm display-block">Username</label>
        <Input id="registry-user" required value={form.username} onChange={e => setForm({ ...form, username: e.target.value })} />
      </div>
      <div className="flex-col gap-xs">
        <label htmlFor="registry-token" className="label-sm display-block">Password or token</label>
        <Input id="registry-token" type="password" autoComplete="new-password" value={form.token} onChange={e => setForm({ ...form, token: e.target.value })} />
      </div>
      <p className="body-sm text-mute">Required for a new registry. Leave blank to retain a saved token.</p>
      <div className="flex mt-sm"><Button type="submit" variant="primary-sm" disabled={saving}>{saving ? 'Saving...' : 'Save Registry'}</Button></div>
    </form>
  </Card>;
}
