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
    <h2 className="heading-md mb-xs">Container Registry Credentials</h2>
    <p className="body-sm text-mute mb-md">Configure credentials for Docker Hub, GHCR, and other private registries.</p>
    {error && <p role="alert">{error}</p>}{message && <p role="status">{message}</p>}
    {registries.length > 0 && <div className="table-container mb-md"><table><thead><tr><th>Registry</th><th>Username</th><th>Token</th><th>Actions</th></tr></thead>
      <tbody>{registries.map(r => <tr key={r.registry}><td>{r.registry}</td><td>{r.username}</td><td>{r.token_set ? 'Configured' : 'Missing'}</td><td><div className="flex gap-sm">
        <Button variant="ghost-sm" onClick={() => setForm({ registry: r.registry, username: r.username, token: '' })}>Edit</Button>
        <Button variant="danger" onClick={() => remove(r.registry)}>Remove</Button>
      </div></td></tr>)}</tbody></table></div>}
    <form className="flex-col gap-sm" onSubmit={save}>
      <label htmlFor="registry-host">Registry host</label><Input id="registry-host" placeholder="docker.io or ghcr.io" required value={form.registry} onChange={e => setForm({ ...form, registry: e.target.value })} />
      <label htmlFor="registry-user">Username</label><Input id="registry-user" required value={form.username} onChange={e => setForm({ ...form, username: e.target.value })} />
      <label htmlFor="registry-token">Password or token</label><Input id="registry-token" type="password" autoComplete="new-password" value={form.token} onChange={e => setForm({ ...form, token: e.target.value })} />
      <p className="body-sm text-mute">Required for a new registry. Leave blank to retain a saved token.</p>
      <div><Button type="submit" variant="primary" disabled={saving}>{saving ? 'Saving...' : 'Save Registry'}</Button></div>
    </form>
  </Card>;
}
