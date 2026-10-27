import React, { useEffect, useState } from 'react';
import { api } from '../api';
import { Button, Card, Input, Modal, Select } from './ui';
export const shortDigest = digest => (digest || '').replace(/^sha256:/, '').slice(0, 12);

export default function DockerImagesSection({ templates = [] }) {
  const [images, setImages] = useState([]);
  const [editing, setEditing] = useState(null);
  const [open, setOpen] = useState(false);
  const [name, setName] = useState('');
  const [template, setTemplate] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);
  const load = async () => {
    try { setImages(await api.getDockerImages()); setError(''); }
    catch (e) { setError(e.message); }
    finally { setLoading(false); }
  };
  useEffect(() => { load(); }, []);
  const edit = image => {
    setError(''); setEditing(image); setName(image?.image_name || ''); setTemplate(image?.template_name || ''); setOpen(true);
  };
  const save = async e => {
    e.preventDefault(); setSaving(true);
    try {
      const payload = { image_name: name, template_name: template || null };
      if (editing) await api.updateDockerImage(editing.image_name, payload);
      else await api.createDockerImage(payload);
      setOpen(false); await load();
    } catch (e) { setError(e.message); }
    finally { setSaving(false); }
  };
  const remove = async image => {
    try { await api.deleteDockerImage(image); await load(); }
    catch (e) { setError(e.message); }
  };
  return <section className="mt-xl mb-xl">
    <div className="flex justify-between items-center mb-lg gap-sm">
      <div><h2 className="heading-lg">Docker Images</h2><p className="body-sm text-mute">Standalone Docker images tracking :latest.</p></div>
      <Button variant="primary" onClick={() => edit(null)}>Add Docker Image</Button>
    </div>
    {!open && error && <p role="alert" className="body-sm">{error}</p>}
    {loading ? <p>Loading Docker images...</p> : images.length === 0 ? <Card>No standalone Docker images tracked yet.</Card> :
      <div className="table-container"><table><thead><tr><th>Image</th><th>Short digest</th><th>Template</th><th>Actions</th></tr></thead>
        <tbody>{images.map(image => <tr key={image.image_name}>
          <td>{image.image_name}:latest</td><td title={image.manifest_digest}>{shortDigest(image.manifest_digest)}</td><td>{image.template_name || 'None'}</td>
          <td><div className="flex gap-sm"><Button variant="ghost-sm" onClick={() => edit(image)}>Edit</Button><Button variant="danger" onClick={() => remove(image.image_name)}>Remove</Button></div></td>
        </tr>)}</tbody></table></div>}
    <Modal isOpen={open} onClose={() => setOpen(false)} title={editing ? 'Edit Docker Image' : 'Add Docker Image'}>
      <form onSubmit={save} className="flex-col gap-sm">
        {error && <p role="alert">{error}</p>}
        <label htmlFor="standalone-image">Docker Image</label>
        <Input id="standalone-image" value={name} onChange={e => setName(e.target.value)} placeholder="nginx or ghcr.io/owner/app" required />
        <p className="body-sm text-mute">Always tracks :latest. Any supplied tag or digest is stripped.</p>
        <label htmlFor="standalone-template">Docker Jira Template</label>
        <Select id="standalone-template" value={template} onChange={e => setTemplate(e.target.value)}><option value="">(None)</option>{templates.map(t => <option key={t.name} value={t.name}>{t.name}</option>)}</Select>
        <div className="flex justify-end gap-sm mt-md"><Button type="button" variant="ghost-sm" onClick={() => setOpen(false)}>Cancel</Button><Button type="submit" variant="primary" disabled={saving}>{saving ? 'Saving...' : 'Save'}</Button></div>
      </form>
    </Modal>
  </section>;
}
